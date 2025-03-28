{
  system ? "x86_64-linux",
  pkgs ? import <nixpkgs> { inherit system; },
}:
let

  packages = [
    #pkgs.tailwindcss_4
    pkgs.zsh
    pkgs.vips
    pkgs.caddy
    pkgs.flyctl
  ];

  LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
    # Add any missing library needed
    # You can use the nix-index package to locate them, e.g. nix-locate -w --top-level --at-root /lib/libudev.so.1
  ];
in
pkgs.mkShell {
  buildInputs = packages;
  shellHook = ''
    export SHELL=${pkgs.zsh}
    export LD_LIBRARY_PATH="${LD_LIBRARY_PATH}"
  '';
}
