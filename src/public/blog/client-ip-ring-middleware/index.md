{
    :page/title  "ol.client-ip: A Clojure Library to Prevent IP Spoofing"
    :blog/date "2025-08-18"
    :blog/author "Casey Link"
    :blog/description "A clojure ring middleware for extracting client IPs from HTTP headers without the usual security vulnerabilities."
    :blog/tags #{"clojure" "security" "open-source" "web-development"}
}

Getting the IP address of a client making a request to your web application sounds like it should be embarrassingly simple. It's not.

If you think it's just a matter of checking `:remote-addr` in your request map or pulling a value out of the X-Forwarded-For header, you've just opened yourself up to IP spoofing vulnerabilities that would make a pentester giggle with delight.

There is no easy, or simple, solution.
The problem is inextricably tied with your specific deployment environment: whether you're behind a reverse proxy, a CDN (or both), which headers your infrastructure uses, and crucially, which ones you can actually trust.
None of this information lives in the HTTP request itself.
You need out-of-band knowledge about your infrastructure topology to correctly parse the maze of forwarding headers without letting an attacker spoof their way past your IP-based security controls.

Armed with that knowledge you still need to correctly parse a tangled mess of headers, deal with IPv6 addresses with ports, know which header to trust when multiple ones exist, and implement all the validation logic to prevent spoofing attacks.

I've been wrestling with this problem for years across various client projects. After implementing the same buggy patterns over and over (and watching other developers do the same), I finally decided to solve this properly.

[`ol.client-ip`][proj] is a Ring middleware (with zero deps!) that handles all this complexity so you don't have to. It's essentially a Clojure port of the excellent [realclientip-go][golang] implementation, which itself was born from the collective frustration of developers who were tired of getting this wrong.

## Who cares?

> Why do we need IP-based security controls?

Well you often don't! And yes, in today's landscape of easy VPNs, CGNAT, the client's ip address is not a bullet-proof identifier or security mechanism.

But sometimes it is very helpful for rate limiting, geographic compliance requirements, abuse prevention, audit trails, or fraud detection.
All of these are made easier when you can, with some confidence, know the actual IP address making the request.


Please don't reach for IP-based controls first thing, they are problematic. But if you do, understand how to protect your self from trivial IP spoofing.

## Why This Is Actually Hard

Here's the thing about modern web infrastructure: your application almost never talks directly to the actual client.
There's usually a reverse proxy, or three. Possibly a load-balancer. Maybe a CDN. All of the above?

Each of these helpful intermediaries likes to "help" by adding headers telling you about the original client. The problem? Any client can also set these headers. Watch this:

```bash
curl -H "X-Forwarded-For: 1.2.3.4" https://your-app.com
```

Does your app use [http-kit][http-kit] as a web server and then look at `:remote-addr`? Boom. I just tricked your app in beliving I'm from IP 1.2.3.4.
If you're using that IP for rate limiting, geolocation, or (heaven forbid) authentication decisions, you're in trouble.

(My PR for this [is here][http-kit-pr] here by the way, I hope it gets merged soon. It doesn't "solve" the problem completely it just makes it harder to shoot your self in the foot if you twiddle a non-default option.)

Often you hear advice like "just use the leftmost IP in X-Forwarded-For." This is terrible advice. It's trivially spoofable.

The slightly better advice is "use the rightmost IP that's not from your infrastructure." Getting warmer, but now you need to know what "your infrastructure" means, and that changes depending on your deployment.

## Enter [`ol.client-ip`][proj]

The library takes a strategy-based approach because (and this is my whole schtick!) there is no one-size-fits-all solution.
Your network topology determines the correct strategy.
The library just makes it easy to implement that strategy correctly.

```clojure
(ns myapp.core
  (:require [ol.client-ip.core :as client-ip]
            [ol.client-ip.strategy :as strategy]))

;; Behind Cloudflare? They provide a trustworthy header
(def app
  (-> handler
      (client-ip/wrap-client-ip
        {:strategy (strategy/single-ip-header-strategy "cf-connecting-ip")})))

;; Behind exactly 2 proxies? Count backwards
(def app
  (-> handler
      (client-ip/wrap-client-ip
        {:strategy (strategy/rightmost-trusted-count-strategy "x-forwarded-for" 2)})))

;; Many more strategies available see usage documentation.

;; Your handler now has access to the "real" † client IP
(defn handler [request]
  (let [client-ip (:ol/client-ip request)]
    {:status 200
     :body (str "I actually know your IP: " client-ip)}))
```

† "Real" is doing a lot of work here.
The epistemological problem isn't just technical.
We're trying to identify "the client" through layers of network abstraction, where each proxy and NAT boundary forces us to accept increasingly indirect evidence of the originating request.
What `client-ip` provides is the earliest source address in the chain that you've declared trustworthy through your configuration.
It's not knowledge of the "true" client (whatever that means in a world of shared connections and VPNs), but rather the best available proxy for client identity given your position in the network topology.

For better or worse, this approach forces you to think about your network topology.
You can't just slap it in and hope for the best.
You have to make a conscious decision about which strategy matches your setup.

### The strategies that Actually Matter

After implementing this for various clients, I've found that 90% of use cases fall into three categories:

**Single trusted header**: You're behind Cloudflare, Fly.io, or a *single* properly configured nginx.
These services provide a header with the socket-level client IP that can't be spoofed (as long as clients can't bypass the proxy).
This is the golden path if you have it.

**Rightmost non-private**: You have proxies in your private network (10.x.x.x, 192.168.x.x, etc.) and they all append to X-Forwarded-For. The rightmost non-private IP is your client. This works great until you put a proxy with a public IP in the chain, at which point you need...

**Rightmost trusted count**: You know exactly how many proxies are between the internet and your app. Count backwards that many IPs in the X-Forwarded-For chain. Simple, effective, but requires you to update the count if your infrastructure changes.

The library supports more strategies: trusted IP ranges, chain strategies for multiple paths, even the dangerous leftmost strategy for when you need to know what the client *claims* their IP is. But honestly? Start with these three.

## Let's shave the yak

This library needs to parse ip addresses. It needs to do comparisons like is the ip address "192.168.1.22" in the trusted subnet "192.168.0/24".

Seems easy enough? But, like, what is an ip address? What can show up there?

Consider these valid IP addresses that might show up in your headers:
- `192.168.1.1` - Easy, IPv4
- `2001:db8::8a2e:370:7334` - IPv6, still manageable
- `[2001:db8::1]:8080` - IPv6 with port notation
- `fe80::1%eth0` - IPv6 with zone identifier (yes, that % is supposed to be there)
- `::ffff:192.0.2.1` - IPv4-mapped IPv6 address
- `[fe80::1%25eth0]:8080` - URL-encoded zone identifier with port

And that's before malicious actors start sending you garbage like `definitely-not-an-ip.com` or `192.168.1.1.example.com` hoping to trigger interesting behaviors in your parser.

I originally had hoped to just lean on Java's `InetAddress.getByName()`, which can do ip address parsing if you pass it raw ip addresses. Seems innocent enough, right?

Wrong. That method will happily perform DNS lookups (it is kind of what it is supposed to do) which will block your request thread. Very bad for throughput.

So I had to implement an IP address parser to guarantee there were no side effects. It's in the namespace [ol.client-ip.ip][ip-parse]. It might come in handy elsewhere..

It's a small detail, but it's the kind of thing you only learn after your app mysteriously hangs in production because someone sent you a malformed IP string that Java decided to resolve when DNS was on the fritz[^note1].


[^note1]: It's not DNS. There’s no way it's DNS. It was DNS


## Should You Use This?

If you're running a Clojure web app and you need to know client IPs (for analytics, rate limiting, geolocation, whatever), then, yea probably.
At least read the documentation and existing literature do understand the problem space.

The library has zero dependencies beyond Clojure itself. It's about 1k SLOC (incl docstrings), thoroughly tested, and boring in all the right ways.
It won't revolutionize your application, but it will prevent that awkward moment when you realize you've been rate-limiting your CDN instead of actual clients.

You can obtain `ol.client-ip` from [Clojars][clojars-artifact] or via a gitlib dep from the repo at [github.com/outskirtslabs/client-ip][proj].

[proj]: https://github.com/outskirtslabs/client-ip
[golang]: https://github.com/realclientip/realclientip-go
[clojars-artifact]: https://clojars.org/com.outskirtslabs/client-ip
[usage]: https://github.com/outskirtslabs/client-ip/blob/main/doc/usage.md
[http-kit-issue]: https://github.com/http-kit/http-kit/issues/226#issuecomment-2903435082
[http-kit-pr]: https://github.com/http-kit/http-kit/pull/599
[http-kit]: https://github.com/http-kit/http-kit/
