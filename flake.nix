{
  inputs = {
    nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1"; # tracks nixpkgs unstable branch
    clj-helpers.url = "github:outskirtslabs/clojure-nix-locker-helpers";
    clj-helpers.inputs.nixpkgs.follows = "nixpkgs";
    deploy-rs.url = "github:serokell/deploy-rs";
    devshell.url = "github:numtide/devshell";
    devshell.inputs.nixpkgs.follows = "nixpkgs";
    devenv.url = "https://flakehub.com/f/ramblurr/nix-devenv/*";
    devenv.inputs.nixpkgs.follows = "nixpkgs";
  };
  outputs =
    inputs@{
      self,
      nixpkgs,
      clj-helpers,
      deploy-rs,
      devshell,
      devenv,
      ...
    }:
    let
      javaVersion = 25;
      system = "x86_64-linux";
      jdk = "jdk${toString javaVersion}_headless";
      nixosModule = import ./nixos/module.nix inputs;
      package =
        pkgs:
        clj-helpers.lib.mkCljBin {
          inherit pkgs;
          name = "site";
          src = ./.;
          jdk = pkgs.${jdk};
          java-opts = [
            "-Duser.timezone=UTC"
            "-XX:+UseZGC"
            "--enable-native-access=ALL-UNNAMED"
          ];
          # the PATH export must live in buildCommand so the locker sees it too
          buildCommand = ''
            export PATH=${pkgs.tailwindcss_4}/bin:$PATH
            clojure -Srepro -T:build uber
          '';
        };
    in
    devenv.lib.mkFlake ./. {
      inherit inputs;
      systems = [ system ];
      withOverlays = [
        devshell.overlays.default
        devenv.overlays.default
        (final: prev: {
          jdk = prev.${jdk};
          clojure = prev.clojure.override { jdk = prev.${jdk}; };
        })
      ];

      packages = {
        default = package;
        # regenerates ./deps-lock.json: `nix run .#locker`
        locker = pkgs: (package pkgs).locker;
      };

      nixosModules.default = nixosModule;

      outputs =
        { pkgsFor, ... }:
        let
          pkgs = pkgsFor.${system};
          services = import ./nixos/services.nix {
            inherit (inputs) nixpkgs;
            inherit pkgs inputs;
            modules = [ nixosModule ];
          };

          mkNode =
            {
              hostname ? "james",
              script ? "activate",
              branch,
            }:
            {
              inherit hostname;
              sshUser = "casey.link";
              user = "casey.link";
              remoteBuild = false;
              sshOpts = [
                "-o"
                "StrictHostKeyChecking=no"
              ];
              profiles = {
                "site-${branch}".path = deploy-rs.lib.${system}.activate.custom (services {
                  inherit branch;
                }) "$PROFILE/bin/${script}";
              };
            };
        in
        {
          deploy.nodes.main = mkNode {
            branch = "main";
          };
        };

      checks = pkgs: deploy-rs.lib.${pkgs.stdenv.hostPlatform.system}.deployChecks self.deploy;

      devShells =
        let
          base = pkgs: [
            pkgs.tailwindcss_4
            pkgs.brotli
            deploy-rs.packages.${system}.deploy-rs
            pkgs.libxml2
            pkgs.playwright
            pkgs.playwright-test
            pkgs.playwright-mcp
          ];
        in
        {
          ci =
            pkgs:
            pkgs.devshell.mkShell {
              imports = [
                devenv.capsules.base
                devenv.capsules.clojure
              ];
              packages = base pkgs;
            };

          default =
            pkgs:
            pkgs.devshell.mkShell {
              imports = [
                devenv.capsules.base
                devenv.capsules.clojure
              ];
              env = [
                {
                  name = "PLAYWRIGHT_BROWSERS_PATH";
                  value = "${pkgs.playwright.browsers}";
                }
              ];
              packages = (base pkgs) ++ [
                pkgs.vips
                pkgs.zsh
              ];
            };
        };
    };
}
