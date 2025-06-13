{
    :page/title  "A Reluctant Relationship: Yubikey and Google Authentication"
    :blog/date "2011-12-13"
    :blog/modified "2025-06-01"
    :blog/author "Casey Link"
    :blog/description "Learn how to use a Yubikey instead of a smartphone for Google's 2-factor authentication."
    :blog/tags #{"security" "yubikey" "google"}
}

*Want the yubikey+google 2-factor authentication solution?* [Skip to the good stuff](#goodstuff).

Passwords rule our lives on the Internet; they are the foundation of
identity management. When a website or service wants to know who you are, you
prove you are you with your username and password. However,  passwords aren't the fundamental piece of this identity management
system.

What happens when you lose your password? We've all been through this
jig before. The website usually sends an email to you with a link or instructions on
how to reset your password. By proving you have access to your email, you are
effectively proving you are you.

Then, your email account is the lowest common denominator; with access to your email
account (nearly) all your other accounts can be accessed. If you use
a completely unique passwords for each service (and store them in a password manager like
[Keepass][kpx] or [Lastpass][lp]), then access to your email account is even
more attractive. Therefore, the importance of securing your email account cannot be
overstated.

When it comes to securing your email [Google's 2-factor authentication][2fac] is pretty awesome. Even though there
are still [some important flaws][flaw] one should be aware of, it can
significantly increase the security of your Google or Google Apps account.

For me there is one major drawback to Google's 2-factor offering: it requires
a cellphone to be useful. This is a drawback for several reasons.

First, your smartphone isn't as secure as we would like. Mobile malware is on
the rise--[particularly][and] if you have an Android phone--and if I was
a malware writer I would be targeting 2-factor authentication apps like
Google's.

The second drawback most people won't identify with: I don't want to carry
a smartphone with me! I travel. A lot. In fact, [I travel by bike][et].
When traveling by bike, minimizing weight is important followed closely by
minimizing the value of my equipment (shiny stuff [gets broken][sad] or stolen), and smartphones are heavy and expensive. So, I carry a tiny and cheap $30 cellphone and swap sim cards as I enter new countries.

How then can I retain the benefit of Google's 2-factor authentication, while
ditching the phone? I could generate OTPs through Google and write them down,
which I have been doing for awhile, but that is a huge PITA.

Enter the [Yubikey][yub]. The Yubikey is a tiny usb device that produces
One-Time Passwords and appears to the OS as a USB keyboard making it work on all
platforms. The Yubikey can hold two identities that can be configured according
to four different options (Yubico OTP, OATH, static, challenge-response).

A Yubikey seems like the perfect lightweight, secure replacement for OTP
generation, how then could I use it with Google's 2-factor authentication?

<a name="goodstuff"></a>

### Finding common ground: OATH-TOTP

Originally, I imagined some system that stored your Google 2-factor auth
secret, and allowed you to auth with your Yubico OTP. Such a system would not
be ideal, because wherever that system lived so would live your google secret. We
want to be sure wherever the secret is stored is secure.

As it turns out [Google's 2-factor authenticator][gauth] is an implementation of the OATH-TOTP protocol, a system for generating one-time passwords via a HMAC-SHA1 hash using the current time as input.

The Yubikey also happens to support the OATH-HOTP protocol (of which TOTP is
a variant), so we should be able to configure the Yubikey to generate OATH-HOTP
OTPs somehow. Unfortunately, the Yubikey is battery-less, so it is unable to
store the current time.

All is not lost, for the Yubikey's fourth configuration option is
a *challenge-response* configuration. This allows a client-side application to
send a challenge to the Yubikey, which the Yubikey uses as input to generate a HMAC-SHA1 hash that becomes the response. This is exactly the cryptographic hashed used by OATH-TOTP and hence Google's 2-factor auth.

Around the same time I figured all this out, Yubico [posted][ytotp] the same
explanation I just gave, along with a Windows client-side application that used
the challenge-response method described to enable Google authentication with
a Yubikey. Huzzah!

### YubiTOTP for Linux

It took awhile, but a [friend][mutant] and I eventually got around to implementing a similar
client-side helper application for Linux.

The implementation is fairly simple (if not pretty). A challenge is generated
based on the current time, sent to the yubikey using the *ykchalresp* utility,
and then the HMAC-SHA1 hash is mangled according to the HOTP specification to
produce a 6 digit code.

Before you can use the tool, you must configure your Yubikey, but then
generating OTPs from your Yubikey is as simple as:
`$ ./yubi_goog.py`.

The tool can also be used to generate OTPs without a Yubikey (using the
*--generate* flag), but you must enter the secret on every invocation.

**Grab the tool and instructions over at the [github repo][yubigoog].**

### Not Perfect

We now have a way to generate OTPs for Google's 2-factor authentication without
a phone; however, this isn't the perfect solution. Generating a TOTP requires
the current time, so the Yubikey must be told the current time which stipulates
the use of a client-side helper application.

So, using this method you can only use your Yubikey where you are able to run
my helper app (or the windows version from Yubico). I often find myself in
Internet cafes or other public terminals, where running a python script isn't
feasible.

As of yet I do not have a working solution. It would be fantastic if Google
would natively support the Yubikey, but in the meantime we'll have to be
satsfied with innovative hacks.

[2fac]: http://googleblog.blogspot.com/2011/02/advanced-sign-in-security-for-your.html 
[kpx]: http://keepass.info
[lp]: http://lastpass.com
[flaw]: http://tech.kateva.org/2011/07/massive-security-hole-in-google-two.html
[and]: http://www.schneier.com/blog/archives/2011/11/android_malware.html
[et]: http://elusivetruth.net
[sad]: https://twitter.com/#!/Ramblurr/status/144521420762918914
[yub]: http://yubico.com/yubikey
[gauth]: http://code.google.com/p/google-authenticator/
[ytotp]: http://yubico.com/totp
[yubigoog]: https://github.com/Ramblurr/yubi-goog
[mutant]: http://mutantmonkey.in/
