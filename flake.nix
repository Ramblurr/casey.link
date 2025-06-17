{
  inputs = {
    nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1"; # tracks nixpkgs unstable branch
    clj-nix.url = "github:jlesquembre/clj-nix";
    deploy-rs.url = "github:serokell/deploy-rs";
  };
  outputs =
    {
      self,
      nixpkgs,
      clj-nix,
      deploy-rs,
      sops-nix,
      ...
    }@inputs:
    let
      system = "x86_64-linux";
      javaVersion = 24;
      jdk = "jdk${toString javaVersion}_headless";
      pkgs = import nixpkgs {
        inherit system;
        overlays = [
          (final: prev: {
            jdk = prev.${jdk};
            clojure = prev.clojure.override { jdk = prev.${jdk}; };
          })
        ];
      };
      nixosModules = {
        default = import ./nixos/module.nix inputs;
      };
      services = import ./nixos/services.nix {
        inherit (inputs) nixpkgs;
        inherit pkgs inputs;
        modules = [ nixosModules.default ];
      };
      site = clj-nix.lib.mkCljApp {
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
            jdk = pkgs."jdk${toString javaVersion}_headless";
            #customJdk.enable = true;
            buildCommand = ''
              export PATH=${pkgs.tailwindcss_4}/bin:$PATH
              clj -T:build uber
            '';
          }
        ];
      };

      mkNode =
        {
          hostname ? "james",
          script ? "activate",
          branch,
          port,
        }:
        {
          inherit hostname;
          sshUser = "root";
          user = "casey.link";
          profiles = {
            "site-${branch}".path = deploy-rs.lib.x86_64-linux.activate.custom (services {
              inherit branch port;
            }) "$PROFILE/bin/${script}";
          };
        };
    in
    {
      inherit nixosModules;
      packages.x86_64-linux = {
        default = site;
      };
      checks.x86_64-linux = deploy-rs.lib.${system}.deployChecks self.outputs.deploy;

      deploy = {
        nodes = {
          main = mkNode {
            branch = "main";
            port = 3000;
          };
        };
      };

      devShells.x86_64-linux =
        let
          base = [
            pkgs.babashka
            pkgs.cljfmt
            pkgs.clj-kondo
            pkgs.clojure
            pkgs.tailwindcss_4
            pkgs.brotli
          ];
        in
        {
          ci = pkgs.mkShell {
            packages = base;
          };
          default = pkgs.mkShell {
            packages = base ++ [
              pkgs.clojure-lsp
              pkgs.vips
              pkgs.zsh
            ];
          };
        };

    };
}
