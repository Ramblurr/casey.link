{
    :page/title  "From the Outskirts: Q1/Q2 2026"
    :blog/date "2026-06-30"
    :blog/author "Casey Link"
    :blog/description "A Q1/Q2 2026 roundup of Casey's open source projects including Clave, shared documentation, ol.vips, and datahike-sqlite."
    :blog/tags #{"clojure" "open-source" "nix"}
}

This is a roundup of my open source work in Q1/Q2 2026.
I'm going to try to make these updates quarterly.
Check back in three months to see how that went.

## Clave Is Public

[Clave](https://docs.outskirtslabs.com/ol.clave/next/) ([repo](https://github.com/outskirtslabs/clave)) entered public preview this quarter, its first public release.
It remains experimental and has no tagged versions yet.
For now, you consume it as a git dependency.

Clave is an ACME client for Clojure ([RFC 8555](https://datatracker.ietf.org/doc/html/rfc8555))[^note1].
ACME is the protocol used to obtain and renew HTTPS certificates automatically. You're probably familiar with Let's Encrypt. It is more or less the poster child for ACME.
Other household tools such as Certbot, Lego and Caddy use it to automate certificate management. Clave is comparable to Certbot and Lego but for the Clojure ecosystem. But it's a library not a CLI tool.

[^note1]: plus RFC 8737, plus 8738, oh and 9773. And those are just the top-level "ACME" ones...

Clave exposes ACME at two useful levels.
The lower-level API lets an application work directly with accounts, orders, authorizations, challenges, and certificate issuance (you won't need this unless you're fixing bugs in the higher-level Clave or doing some fun RFC spelunking).
The higher-level API handles the job most applications need: provision a certificate, manage it, and keep it renewed.
Applications can choose how much of the protocol they need to handle themselves.

A small runtime footprint was an important design goal for Clave.
This was at odds with the requirements dictated by the RFC.

To implement ACME and the certificate-lifecycle, Clave needed many capabilities the JDK does not expose through public APIs. As Rachel By the Bay puts it:

> The whole thing amounts to "throw in every little bit of webshit tech that we can", and it makes for a real problem to try to implement this in a safe and thorough way. 
> -- ["Why I no longer have an old-school cert on my https site"][rbtb]

[rbtb]: https://rachelbythebay.com/w/2025/05/22/ssl/

1. Certificate Signing Request (CSR) generation (RFC 2986)
2. JSON Web Key (JWK) construction and JSON Web Signature (JWS) signing for ACME requests (RFCs 7517, 7515, 8555)
3. ASN.1 (DER) encoding and decoding for PKCS#10, X.509 extensions, and protocol messages (RFCs 5280, 6960; and ITU-T X.690, god help you if you have to learn about ITU-T)
4. Self-signed X.509 certificate generation for TLS-ALPN-01 challenges (RFC 8737)
5. OCSP request construction, response parsing, and responder URL extraction from certificates (RFC 6960)
6. Authority Key Identifier parsing for ACME Renewal Information (ARI) identifiers (RFC 9773)
7. JSON (de)serialization (because I'm a completionist, not that you care but that is RFC 8259, and no JSON probably isn't "webshit" (ahem) but the JDK does still lack it in 2026)

Most of these could be obtained by dropping in a heavyweight security dep like Bouncy Castle, but I cringe at the thought of foisting a dep like that onto a consumer.
In particular, a dependency like Bouncy Castle is one that has to be carefully mediated and upgraded.

Shelling out to subprocesses like `openssl` was off the table too.
Deploying and using Clave (and applications that bundle it) should be as easy as copying a jar around without special instructions like "Oh and make sure you apt-get install openssl in the runtime env too!" (not to mention the needless surface area that brings in).

(aside: The JDK ships CSR generation code inside `keytool`, that CLI that comes bundled with the JDK usually, yet keeps it in internal APIs that are off limits to such plebes as us application developers)

In the end, Clave implements all of the above in Clojure.
Well, not "all" of the "all of the above".
For example, only just enough ASN.1 [is included][der.clj] to do CSRs, TLS-ALPN-01 generation, a few cert extensions, and OCSP responses.
I didn't consider performance much at all since I don't expect these functions to be used in a hot path anywhere.

[der.clj]: https://github.com/outskirtslabs/clave/blob/04f7bf854bcbe379df14e3f9d7221af9d0c030d6/src/ol/clave/crypto/impl/der.clj

Snark and NIH aside, I want to call out that all cryptographic primitives come from the JVM.
Clave hand rolls exactly zero custom crypto. 
It composes the primitives as the RFCs decree and handles the awkward certificate plumbing around it. 
There might be the odd correctness bug outside our narrow ACME use case, but if it doesn't affect the usage of the library, I'm fine with that. 

That keeps the project focused and avoids adding a heavyweight security library to every application using it.

And I can stake my reputation on the statement that you won't be exposed to any RCEs from cursed ASN.1 decoder.

That was a long-winded way of saying that Clave has two hard runtime dependencies: 

1. [babashka/json](https://github.com/babashka/json) is a slim adapter that uses a JSON provider already on your classpath.
2. [Trove](https://github.com/taoensso/trove) provides lightweight signals and logging without forcing a backend on the application.

Clave comes with a handy adapter for ring-jetty out of the box, with more planned.

## Docs, Docs, Docs!

I also launched [docs.outskirtslabs.com](https://docs.outskirtslabs.com/) ([repo](https://github.com/outskirtslabs/docs)), a consolidated documentation site for all my projects.

My projects needed coherent documentation that could combine guides, how-tos, API references (aka docstrings), and I wanted to be able to interlink among them.
Each tagged release also needed its own quasi-immutable complete documentation bundle[^note2].

[^note2]: "quasi" because I reserve the right to improve the docs by correcting typos or falsehoods, or by adding new information after a release tag has been cut.

I first considered [cljdoc](https://cljdoc.org/) as I hope many would in the Clojure community.
It's a valuable resource for the community, and I appreciate the work its maintainers put into it, but it didn't provide several things I needed for my projects.
The changes/tweaks I wanted were substantial and I had no assurance cljdoc would accept them after all the effort.
Also, frankly, I bounced off trying to contribute over there pretty hard as I found it hard to get any traction.

Important features I want from docs are:

- A strong in-page table of contents,
- Fast, cross-project search,
- docstrings in one project linking directly to vars and namespaces in another, custom markup inside docstrings, API references mixed with longer guides, and Markdown content alongside AsciiDoc, and
- control over the branding, colors, and dark and light themes.

I landed on [Antora](https://antora.org/) with Babashka handling the surrounding build work.
Antora is highly customizable and, somewhat to my surprise, pleasant to work with despite living in the JavaScript and npm ecosystem.
Each project keeps its documentation in its own repo while the shared site publishes everything as one coherent collection.

## Smaller Updates

### Datomic Pro Flake v0.14.0

[Datomic Pro Flake v0.14.0](https://docs.outskirtslabs.com/datomic-pro-flake/0.14/) ([repo](https://github.com/outskirtslabs/datomic-pro-flake)) shipped on April 29, one day after Datomic Pro 1.0.7622.
It added versioned Nix packages for Datomic Pro and Peer 1.0.7622 while retaining packages for older versions, which keeps upgrades deliberate.
The [1.0.7622 changelog](https://docs.datomic.com/changes/pro.html#1.0.7622) has more details.

### ol.vips: Clojure bindings for libvips image processing library

[ol.vips](https://docs.outskirtslabs.com/ol.vips/next/) ([repo](https://github.com/outskirtslabs/vips)) gives Clojure programs access to [libvips](https://www.libvips.org/) ([repo](https://github.com/libvips/libvips)), a fast and memory-efficient native image-processing library.
It provides a Clojure-oriented API, generated wrappers for libvips operations, and platform-native jars built on Java 22's Project Panama APIs.

I've used an earlier version in a client project for months, with great results processing large batches of images quickly.
This public project extracts that work into a library other Clojure applications can use.
I think it has the potential to become the best image-processing library in the Clojure ecosystem.
The first public release is almost ready, and I hope to mark it stable by the end of the year.

### datahike-sqlite Playing Catch Up

I brought [datahike-sqlite](https://docs.outskirtslabs.com/datahike-sqlite/next/) ([repo](https://github.com/outskirtslabs/datahike-sqlite)) up to Konserve 0.7.x and Datahike 0.8, which was a much larger compatibility change than those version numbers suggest.
The backend talks to SQLite through sqlite4clj and Java's Foreign Function Interface rather than JDBC.
It works, but it is still experimental.
Datahike and Konserve have kept moving-[@whilo](https://github.com/whilo) has really been busy-so it needs another compatibility pass.
Eventually I want to split the reusable storage layer into a separate project called konserve-sqlite.

## Further Out: Q3 2026

Work has already started on an unnamed sister project to Clave: a provider-independent Clojure API for managing DNS records.
ACME's DNS-01 challenge requires creating temporary TXT records, and wiring that up means dealing with a different API, authentication model, error format, and set of sharp edges for every DNS provider.
A shared provider layer can solve those details once, giving Clave and other projects a safer way to manipulate DNS records without reimplementing the same code.

I'm also hoping to put out an initial public preview of Busker, an experimental modern Clojure web server built around [libh2o](https://github.com/h2o/h2o/) (the server that powers Fastly's global CDN).
I've mentioned it a little in the Clojurians Slack and Datastar Discord, where it has already generated useful technical discussion and collaborative development.
There is plenty left to do, but getting that first preview into public is the Q3 goal.
