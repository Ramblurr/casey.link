{
    :page/title  "Taming Datomic Pro Deployment with Nix"
    :blog/date "2025-01-24"
    :blog/author "Casey Link"
    :blog/description "Simplifying Datomic Pro deployments for NixOS and non-NixOS systems."
    :blog/tags #{"datomic" "nix" "devops" "clojure"}
}

I've been working with [Datomic][datomic] for years, and while I love its immutable, time-aware data model, deploying it has always been... an adventure.
Unlike most modern databases that come with ready-to-use container images or OS packages, Datomic Pro arrives as a bare JAR file with some configuration examples.

If you are running it on bare metal or a standard Linux VM, then a custom systemd service file is all you need, but if you need to deploy it to a containerized environment you have a little work to do.

Datomic Pro is distributed as a Java application with a deployment model that requires manual assembly.
It consists of a transactor (the server component), some storage backend (like a SQL db), and an optional console (web UI).
Getting all these pieces working together requires:

1. JVM setup and configuration
2. Various storage backend configurations
3. Secret management
4. Classpath wrangling for custom drivers
5. Coordination between different components

And everyone ends up with custom shell scripts, Docker files, and deployment procedures that rapidly drift out of sync with the codebase.
In other words, it's a perfect candidate for the reproducible deployment approach that NixOS offers.

Enter [datomic-pro-flake][proj], my attempt to bring sanity to Datomic Pro deployments using the power of Nix.
If you've never heard of Nix, it's that declarative package manager that your one colleague won't shut up about. (Confession: I'm that colleague.)

## What's in the Flake?

The datomic-pro-flake project provides three main components:

1. **Nix Packages**: Pure, reproducible builds of Datomic Pro components (transactor, console, and peer library)
2. **NixOS Modules**: Declarative configuration for running Datomic Pro on NixOS systems
3. **Container Images**: Run Datomic with your favorite container orchestrator (mine is docker compose), no nix required!

All three approaches are end-to-end tested in CI, which means you don't have to worry about whether they actually work.
The tests exercise the package and module by booting a transactor, writing some datoms, and ensuring they can be read back.
(I've spent enough late nights debugging non-functional database deployments for all of us.)

## The Nix Package Approach

If you're already using Nix, this is probably what you're looking for. The flake provides:

```nix
# In your flake.nix
inputs.datomic-pro.url = "https://flakehub.com/f/Ramblurr/datomic-pro/$LATEST_TAG.tar.gz";
```

The packages are configurable through Nix's override pattern, allowing you to add custom Java libraries or native dependencies.

This is particularly handy if you need specific JDBC drivers or want to integrate with exotic storage systems.

What makes this approach special? The entire packaging and deployment becomes declarative, reproducible, and easily version-controlled.

One particularly neat feature is the automatic JRE slimming.
Since Datomic only needs specific JDK modules, we use `jdeps` to analyze exactly what's needed and create a minimal runtime environment.
This reduces the package size by hundreds of megabytes.


### NixOS Modules: Set It and Forget It (But Don't Actually Forget It—This Is a Database After All)

For those running NixOS systems, the modules provide a simple approach:

```nix
# In your configuration.nix
services.datomic-pro = {
  enable = true;
  secretsFile = "/path/to/secrets";
  settings = {
    protocol = "sql";
    host = "0.0.0.0";
    port = 4334;
    # And any other Datomic settings... see README
  };
};
```

The module handles all the hardest parts:
- Systemd service configuration
- Runtime property generation
- Data directory setup
- Classpath configuration
- Ready to use with sops-nix, agenix, or whatever hand-rolled secrets management tool you have.

And there's a companion `datomic-console` module for running the web UI with similarly straightforward configuration.

## Container Images: For Everyone Else

Not everyone is ready to drink the Nix Kool-Aid (though you should—it's delicious), so the flake also produces ready-to-use container images.
Yes, good ol' Docker-compatible images that work exactly where you'd expect them to:

```yaml
services:
  datomic-transactor:
    image: ghcr.io/ramblurr/datomic-pro:unstable
    environment:
      DATOMIC_PROTOCOL: sql
      DATOMIC_SQL_URL: jdbc:sqlite:/data/datomic-sqlite.db
      # ... more ....
    volumes:
      - ./data:/data
    ports:
      - 127.0.0.1:4334:4334
```

These images support both environment variables and file-based configuration, making them suitable for Kubernetes, Docker Compose, or other container orchestration systems.
The [README][readme] includes examples for deploying with various storage backends, including Postgres and SQLite.

[readme]: https://github.com/Ramblurr/datomic-pro-flake/blob/main/README.md

## Real-World Usage

I've had variations of this project in production for awhile, though it wasn't until relatively recently when Datomic Pro became totally free (as in beer) and the binaries
released under the Apache 2.0 license that I felt comfortable making this public.

With the client projects I've used this on, it has significantly simplified deployments. The container image in particular "just works" and is grokkable by non-clojure non-jvm familiar operations folk.

FWIW the practical deployment patterns I've found useful:

1. **Local Development**: Use the container with a SQLite backend for quick local development
2. **Production (Single-Node)**: Deploy with SQLite for simple single-node projects
3. **Production (Multi-Node)**: Use PostgreSQL for scalable multi-node deployments

(For testing you're using datomic in-mem dbs, right?!)

Each approach is documented in the README with concrete examples you can start from.

## Future Directions

The project is currently in a "stable but evolving" state. Before hitting 1.0, I'm planning to add:

- Better version pinning to prevent surprise database upgrades
- Out of the box example for NixOS modules w/ Postgres
- More storage backend examples and configurations
- Integration with secrets management tools like sops-nix
- Performance optimization for specific cloud environments

## Why You Should Try It

If you're using Datomic Pro, the NixOS module or container image might save you hours of configuration headaches and provide a more reliable deployment process.

Even if you're just curious about Datomic, it offers the easiest way to get started without wrestling with configuration files.

Interested? Check out the [GitHub repository][proj].

And if you have questions or suggestions, feel free to open an issue or reach out to me on the Clojurians Slack (@Ramblurr).

As with any database deployment, remember: test thoroughly before pointing it at production data.
Your future self will thank you.

Happy datom accretion!

[datomic]: https://www.datomic.com/
[proj]: https://github.com/Ramblurr/datomic-pro-flake
[nix]: https://nixos.org/
