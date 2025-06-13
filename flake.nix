{
  description = "casey.link";
  inputs = {
    nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1"; # tracks nixpkgs unstable branch
    flake-utils.url = "github:numtide/flake-utils";
    clj-nix.url = "github:jlesquembre/clj-nix";
  };
  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
      clj-nix,
    }:

    let

      javaVersion = 24;

      overlays = [
        (
          final: prev:
          let
            jdk = prev."jdk${toString javaVersion}";
          in
          {
            clojure = prev.clojure.override { inherit jdk; };
          }
        )
      ];
    in
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        #pkgs = nixpkgs.legacyPackages.${system};
        pkgs = import nixpkgs { inherit system overlays; };
      in
      {
        packages = {
          default = clj-nix.lib.mkCljApp {
            inherit pkgs;
            modules = [
              {
                projectSrc = ./.;
                name = "me.casey.link/www";
                main-ns = "site.server";
                java-opts = [
                  "-Duser.timezone=UTC"
                  "-XX:+UseZGC"
                  "--enable-native-access=ALL-UNNAMED"
                ];
                jdk = pkgs."jdk${toString javaVersion}";
                #customJdk.enable = true;
                buildCommand = ''
                  export PATH=${pkgs.tailwindcss_4}/bin:$PATH
                  clj -T:build uber
                '';
              }
            ];
          };

        };
        devShells = {
          default = pkgs.mkShell {
            packages = [
              pkgs.babashka
              pkgs.caddy
              pkgs.cljfmt
              pkgs.clj-kondo
              pkgs.clojure
              pkgs.clojure-lsp
              pkgs.tailwindcss_4
              pkgs.vips
              pkgs.zsh
            ];
          };
        };
      }
    );
}
