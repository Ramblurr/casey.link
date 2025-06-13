{
    :page/title  "wayland-java, a library no one asked for"
    :blog/date "2024-10-04"
    :blog/author "Casey Link"
    :blog/description "Modern Java bindings for libwayland that let you create Wayland clients/servers without stepping outside the JVM."
    :blog/tags #{"java" "wayland" "open-source" "linux"}
}
# wayland-java, a library no one asked for

...because someone had to bridge this particularly niche technological divide between JVM developer and the modern Linux desktop.

[`wayland-java`'s][proj] a set of Java bindings for libwayland and wayland-protocols that lets you create [Wayland][wayland] client (and server!) applications in pure Java - without writing a single line of C code.

If you've ever needed to call into native libraries from Java, you've likely experienced the special kind of frustration that is JNI.
It's verbose, error-prone, and feels like you're working in two entirely different languages at once... because you are.
Since JDK 1.1, released in the late 90s, this has been the inevitable tax paid by JVM-language developers wanting to interface with native code.

With the release of JDK 22, we got the finalized Foreign Memory & Memory API (formerly Project Panama).
This is a serious upgrade for Java developers needing to interact with native code.
The new [FFM APIs][FFM] mean: no more JNI ceremony, faster native interop, and no more brittle glue code in C, just a cleaner interface to the systems we need to talk to.


wayland-java leverages this new API to provide a proper Java interface to the Wayland protocol, making it possible to create graphical applications that run directly on Wayland compositors, while writing code in only Java (or other JVM hosted languages).
The code reads like actual Java, because it is!

The codebase for this library isn't entirely new.
It's a fork of Erik De Rijcke's 2015-era effort, which I've completely rewritten to use Project Panama for FFI.
The original project was impressive in its own right, but the new FFM API makes this approach significantly cleaner and more maintainable.

However the project isn't production-ready, and probably won't be without more community or commercial interest. 
A client [funded me][ol] to create a proof-of-concept application using Wayland on Linux desktop through late 2024. 
The work was just R&D and won't be moving forward (for reasons unrelated to the tech!)


While I cannot opensource the full PoC project, I was able to extract out the wayland bindings into this little library.

If you're curious, [the project][proj] provides several artifacts: client stubs, server stubs, shared stubs, a scanner tool that generates bindings from Wayland protocol XML descriptions, and a pre-packaged selection of protocols.
You can generate your own protocols too, if the included ones don't meet your needs.
The library isn't particularly high-level, you'll still need to understand wayland deeply.

I'm not expecting this library to take the world by storm.
It's much more a case of "this exists, and if you need it someday, you won't have to suffer through building it yourself"
The code is Apache-licensed, properly documented, and the build process won't make you question your career choices as a JVM developer.
You'll need JDK 22+, Linux with libc, and libwayland available at runtime.
(If you use nix, the included devshell will get it working out of the box)

Will the intersection of JVM developers and Wayland enthusiasts ever grow beyond dozens? Probably not.
But for those few, perhaps this makes something possible that wasn't before.

And that's reason enough to build it.


[proj]: https://github.com/ramblurr/wayland-java
[me]: https://outskirtslabs.com
[FFM]: https://docs.oracle.com/en/java/javase/22/core/foreign-function-and-memory-api.html
[wayland]: https://wayland.freedesktop.org/
