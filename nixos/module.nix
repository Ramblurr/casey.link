{ self, ... }@inputs:
{
  config,
  pkgs,
  lib,
  ...
}:
let
  inherit (lib)
    mkEnableOption
    mkOption
    mkIf
    types
    mkMerge
    ;
  cfg = config.services.site;
in
{
  options.services.site = {
    enable = lib.mkEnableOption "site";
    package = mkOption {
      type = types.path;
      default = self.packages.x86_64-linux.default;
      description = ''
        Path to the package
      '';
    };
    socket = mkOption {
      type = types.string;
      description = ''
        The name of the unix socket. e.g, "site.sock"
      '';
    };
  };
  config = mkIf cfg.enable {
    system.stateVersion = "25.11";
    systemd.services.site = {
      after = [ "network.target" ];
      wantedBy = [ "multi-user.target" ];
      unitConfig.ConditionPathExists = [
        "${cfg.package}/bin/site"
        "%h/.run"
      ];
      environment = {
        SITE_UNIX_SOCKET = "%h/.run/site.sock";
      };
      script = ''
        rm -f "%h/.run/site.sock"
        ${cfg.package}/bin/site
      '';
      postStart = ''
        TIMEOUT=60
        ELAPSED=0

        while [ ! -S "$SITE_UNIX_SOCKET" ]; do
            if [ $ELAPSED -ge $TIMEOUT ]; then
                echo "Timeout waiting for socket $SITE_UNIX_SOCKET"
                exit 1
            fi
            sleep 1
            ELAPSED=$((ELAPSED + 1))
        done
        chmod 0770 "$SITE_UNIX_SOCKET"
      '';
      postStop = ''
        rm -f $SITE_UNIX_SOCKET
      '';
    };
  };
}
