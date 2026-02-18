{
  inputs = {
    nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1"; # tracks nixpkgs unstable branch
    clj-nix.url = "github:jlesquembre/clj-nix";
    clj-nix.inputs.nixpkgs.follows = "nixpkgs";
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
      clj-nix,
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

      packages.default =
        pkgs:
        clj-nix.lib.mkCljApp {
          inherit pkgs;
          modules = [
            {
              projectSrc = ./.;
              name = "link.casey/site";
              main-ns = "site.server";
              java-opts = [
                "-Duser.timezone=UTC"
                "-XX:+UseZGC"
                "--enable-native-access=ALL-UNNAMED"
              ];
              jdk = pkgs.${jdk};
              buildCommand = ''
                export PATH=${pkgs.tailwindcss_4}/bin:$PATH
                clj -T:build uber
              '';
            }
          ];
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
              remoteBuild = true;
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
