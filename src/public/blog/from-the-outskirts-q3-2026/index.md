{
    :page/title  "From the Outskirts: Q3 2026"
    :blog/date "2026-10-20"
    :blog/author "Casey Link"
    :blog/description "A Q3 2026 roundup of Casey's open source work on Busker, native interop, ol.vips, Fluent tooling, and Protocol53."
    :blog/tags #{"clojure" "open-source"}
}

From the Outskirts is a roundup of my open source work in Q3 2026.

In the last update, I mentioned a DNS library and hoped to get a public preview of Busker out during Q3. Both projects are now public.

There's also been some work on FFI (native interop) in the Clojure ecosystem that I'm particularly excited about.

In this edition of From The Outskirts:

- [Busker is in public preview](#busker-is-in-public-preview)
- [Native libraries from Clojure and Babashka](#native-libraries-from-clojure-and-babashka)
- [Mozilla Fluent tooling](#mozilla-fluent-tooling)
- [Protocol53](#protocol53)
- [Further Out: Q4 2026](#further-out:-q4-2026)


## Busker is in public preview

[Busker][busker] is a Clojure web server built on [libh2o][libh2o] (the web server core that powers Fastly's global CDN) serving Ring applications over HTTP/1.1, HTTP/2, and HTTP/3 with automatically provisioned TLS certificates (now [Clave][clave-docs] makes sense?).
I've soft-launched it, no big announcement yet, so others can preview it and provide feedback before APIs solidify.
It's available to try, but still experimental and not recommended for production use.

But why another Clojure web server? 
I'll expand on this in a future Rationale section of the Busker docs, but it boils down to simple deployments.

My clients value simplicity and affordability in the services I build and run for them (much of my work is operational, keeping servers online and healthy).
Funding for these projects is grant-based, and between grants we have to run lean and mean.
Every component in the deployment system is something that can fail.
So I want to minimize all the moving parts.

Busker helps with this because it lets me embed the web server inside the Clojure application.
I already embed the database+queue etc in the application also.
Deployment is as simple as `scp`ing an uberjar (that can generate its own systemd service unit file) to a Linux virtual machine.

An internet-facing HTTP stack is a bad place to indulge in reinventing things.
That's why another pillar of Busker is [libh2o][libh2o]: we build on the H2O maintainers' work and Fastly's expertise rather than rolling our own HTTP implementations.
Busker already gives me enough security-sensitive work without volunteering for another stream of incoming CVEs.

More on Busker in another post.
The [documentation][busker-docs] has setup instructions and examples if you'd like to experiment with it.
Send me feedback!

## Native libraries from Clojure and Babashka

[ol.vips][ol-vips] and Busker's native bindings are now using [babashka/ffi][babashka-ffi].
[borkdude][borkdude] was inspired created babashka/ffi after some cross-pollination between [Anders Murphy][anders] and myself.

An FFI, or foreign function interface, lets you call functions in native libraries from another language.
In this case, that means calling them from Clojure and Babashka, including ordinary `bb` scripts.

Before babashka/ffi I was using [coffi][coffi], the library that babashka/ffi is inspired by.
And I want to take a moment to give props and thanks on Joshua Suskalo, the coffi author.
He pioneered this approach to easy native interop in Clojure, using Java's new Foreign Function and Memory API.

But why switch to babashka/ffi?
Compatibility, simply put.
It supports both Clojure and Babashka.
It's built into Babashka itself, whereas coffi is not Babashka compatible.
That opens up native libraries to small Babashka scripts, not just full JVM applications.
You can load a library, describe the functions you need, and call them from the script.
With the same babashka/ffi API you can also run code in Clojure on the JVM.

Example: [libvips][libvips] is a very well respected library in the image processing world, primarily for security, speed and extremely low memory footprint (compared to alternatives like ImageMagick).
[`ol.vips`][ol-vips] is my Clojure library for image processing that wraps the native [libvips][libvips] shared library and provides a nice Clojure interface.
As of this post it is ported to babashka/ffi but is not 100% compatible yet.
I'm waiting on [a small issue][bbi] in babashka to be fixed.

Oh and in [sqlite4clj][sqlite4clj] I landed the coffi -> babashka/ffi migration also.
Are you using SQLite in Clojure?
Are you still using JDBC?
Boo!
Check out [sqlite4clj][sqlite4clj]!

This is what excites me about Project Panama and the JVM's Foreign Function and Memory API!
Clojure was designed as a hosted language and taking advantage of the JVM library ecosystem has definitely been a boon.
But often the Java libraries we want to consume are bloated and/or expose cumbersome object-oriented APIs that are ungainly in Clojure.

I think Clojure has a great future making use of native libraries instead of reaching for another bloated Java dependency.
Like with libh2o and libvips, there are substantial existing implementations to build on, while the application-facing code can remain Clojure.
Native libraries still need packaging and the bindings still need to get memory management right, but I'm glad to have this option in both Clojure and Babashka.

## Mozilla Fluent tooling

[Fluent][project-fluent] is Mozilla's localization system for the text an application presents in different languages.
It's been around since roughly 2017 and is in my opinion the best system for creating and managing translations of strings for software projects that don't bend the knee to English.

Take a selected-file count, with `$count` passed in as a number:

<div class="grid gap-4 md:grid-cols-2">
<div class="min-w-0">

English (`en.ftl`):

```ftl
selected-files = { $count ->
    [one] One file selected.
   *[other] { $count } files selected.
}
```

</div>
<div class="min-w-0">

Arabic (`ar.ftl`):

```ftl
selected-files = { $count ->
    [zero] لم يتم تحديد أي ملفات.
    [one] تم تحديد ملف واحد.
    [two] تم تحديد ملفين.
    [few] تم تحديد { $count } ملفات.
    [many] تم تحديد { $count } ملفًا.
   *[other] تم تحديد { $count } ملف.
}
```

</div>
</div>

English needs only two plural categories, but Arabic has six!
With [fluent-clj][fluent-clj], the application supplies the same message ID and count:

```clojure
(require '[noahtheduke.fluent :as fluent])

(def en (fluent/build "en" (slurp "en.ftl")))
(def ar (fluent/build "ar" (slurp "ar.ftl")))

(fluent/format en :selected-files {:count 2})
;; => "2 files selected."

(fluent/format ar :selected-files {:count 2})
;; => "تم تحديد ملفين."
```

However, suppose that the message we wanted to show addresses the reader with the 2nd person pronoun "You": 

> "You have two files selected."

Arabic distinguishes masculine and feminine forms of singular "you", but English's "you" doesn't.

Assuming we have the gender of the reader then this becomes in fluent:


<div class="grid gap-4 md:grid-cols-2">
<div class="min-w-0">

English (`en.ftl`):

```ftl
selected-files = { $count ->
    [one] You have one file selected.
   *[other] You have { $count } files selected.
}
```

</div>
<div class="min-w-0">

Arabic (`ar.ftl`, `[two]` branch only):

```ftl
...
    [two] { $gender ->
        [masculine] لديكَ ملفان محددان.
        [feminine] لديكِ ملفان محددان.
       *[other] تم تحديد ملفين.
    }
...
```

</div>
</div>

Unlike plural categories, `masculine` and `feminine` are labels we chose for an application-supplied `$gender` string.
Fluent doesn't infer the person's gender.
Passing `"unspecified"` uses the neutral fallback, "Two files have been selected."
Rebuild the bundles from the updated files:

```clojure
(def en (fluent/build "en" (slurp "en.ftl")))
(def ar (fluent/build "ar" (slurp "ar.ftl")))

(fluent/format en :selected-files {:count 2 :gender "feminine"})
;; => "You have 2 files selected."

(fluent/format ar :selected-files {:count 2 :gender "feminine"})
;; => "لديكِ ملفان محددان."

(fluent/format ar :selected-files {:count 2 :gender "masculine"})
;; => "لديكَ ملفان محددان."
```

The English translator/writer doesn't even have to know about the `$gender` for these strings.
Arabic uses it to choose the wording.

Fluent is powerful and enables you to produce native sounding applications for all languages.
If I'm building software for people, getting their language right is part of the job.

Fluent, however, does have a tooling problem, especially if you don't want to use the npm ecosystem.
It can be cumbersome to work with fluent files in your editor as there's no linter or highlighting.

As part of a client project I was able to build some tooling which I can now open source under [outskirtslabs/fluent-tooling][fluent-tooling].
It includes :

- `tree-sitter-fluent`, an *error-tolerant* Tree-sitter grammar
- `ftl-lint`, a command-line linter for Fluent `.ftl` files with compiler-style diagnostics
- and `fluent-ts-mode`, a Tree-sitter major mode for Emacs with syntax highlighting.

The [docs][fluent-docs] cover the individual tools and how to use them.

## Protocol53

[ol.protocol53][p53] provides one Clojure interface for managing DNS records across providers.
Available provider integrations include Cloudflare, [deSEC][desec], GoDaddy, and Porkbun.

It grew out of Clave's need to handle ACME DNS-01 challenges without duplicating the integration work for every DNS provider.

Example usage:

```clojure
(require '[ol.protocol53 :as protocol53]
         '[ol.protocol53.cloudflare :as cloudflare])

(def dns (cloudflare/provider {:api-token (System/getenv "CLOUDFLARE_API_TOKEN")}))

(protocol53/append-records! dns
 "example.com"
 [{:name "www"
   :type "A"
   :ttl  60
   :data "192.0.2.1"}]
 {:timeout (java.time.Duration/ofSeconds 10)})
```


See the [documentation][p53] and [API reference][p53-api].

## Further Out: Q4 2026

[Stardust DB][stardustdb] has landed in public preview.
It's a data engine from the creator of Datastar.
Think SQLite + Datomic + Redis + Elasticsearch + NATS + Object Storage all in one small binary.
It has crazy potential, so I'll be poking and proding at it over the next months to learn more.

Busker's performance is pretty good, but it can be better. 
My goal is to be within 20% of vanilla h2os numbers for h1, h2, and h3.

That's all from the outskirts. Back to the lab.


‣ Casey


[p53]: https://docs.outskirtslabs.com/ol.protocol53
[desec]: https://desec.io/
[p53-api]: https://docs.outskirtslabs.com/ol.protocol53/next/api
[anders]: https://github.com/andersmurphy
[babashka-ffi]: https://github.com/babashka/ffi
[borkdude]: https://github.com/borkdude
[busker]: https://github.com/outskirtslabs/busker
[busker-docs]: https://docs.outskirtslabs.com/ol.busker/next/
[clave-docs]: https://docs.outskirtslabs.com/ol.clave/next/
[coffi]: https://github.com/IGJoshua/coffi
[fluent-docs]: https://docs.outskirtslabs.com/fluent-tooling/next/
[fluent-tooling]: https://github.com/outskirtslabs/fluent-tooling
[h2o-zig]: https://github.com/outskirtslabs/h2o-zig
[libh2o]: https://github.com/h2o/h2o
[ol-vips]: https://github.com/outskirtslabs/vips
[outskirtslabs-docs]: https://docs.outskirtslabs.com/
[protocol53-repo]: https://github.com/outskirtslabs/protocol53
[project-fluent]: https://projectfluent.org/
[sqlite4clj]: https://github.com/andersmurphy/sqlite4clj
[libvips]: https://www.libvips.org/
[stardustdb]: https://stardustdb.com
[fluent-clj]: https://github.com/noahtheduke/fluent-clj
[bbi]: https://github.com/babashka/babashka/issues/2203
