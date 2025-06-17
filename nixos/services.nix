{
  nixpkgs,
  pkgs,
  modules,
  inputs,
  ...
}:
{
  port ? 3000,
  branch ? "main",
#secretsFolder ? ./secrets,
#secretsPath ? "/home/site/.config/site/environment",
#sopsKey ? "/home/site/.config/site/keys.txt",
}:
let
  inherit (pkgs.lib) concatStringsSep mapAttrsToList;

  serviceSuffix = "-${branch}";
  secretsSuffix =
    if
      builtins.elem branch [
        "main"
        "develop"
      ]
    then
      serviceSuffix
    else
      "-pr";

  nixosSystem = nixpkgs.lib.nixosSystem {
    system = "x86_64-linux";
    modules = modules ++ [
      {
        services.site = {
          enable = true;
          socket = "site${serviceSuffix}.sock";
        };
        systemd.services.site.preStart = ''
          while ! ( systemctl is-active network-online.target > /dev/null ); do sleep 1; done
        '';
      }
    ];
  };

  serviceNames = [
    "site"
  ];

  mkService =
    name: pkgs.writeText "${name}.service" nixosSystem.config.systemd.units."${name}.service".text;

  copyServices = concatStringsSep "\n" (
    map (
      name:
      let
        serviceName = name + serviceSuffix;
      in
      ''
        rm -f -- "$HOME/.config/systemd/user/${serviceName}.service" "$HOME/.config/systemd/user/default.target.wants/${serviceName}.service"
        ln -s ${mkService name} "$HOME/.config/systemd/user/${serviceName}.service"
        ln -s "$HOME/.config/systemd/user/${serviceName}.service" "$HOME/.config/systemd/user/default.target.wants"
      ''
    ) serviceNames
  );
  mkSystemctlCommand =
    action: serviceNames: serviceSuffix:
    ''systemctl --user ${action} ${
      concatStringsSep " " (map (name: name + serviceSuffix) serviceNames)
    }'';
  mkDeleteServices =
    serviceNames: serviceSuffix:
    concatStringsSep "\n" (
      map (
        name:
        let
          serviceName = name + serviceSuffix;
        in
        ''
          rm -f -- "$HOME/.config/systemd/user/${serviceName}.service" "$HOME/.config/systemd/user/default.target.wants/${serviceName}.service"
        ''
      ) serviceNames
    );

  restartServices = mkSystemctlCommand "restart" serviceNames serviceSuffix;
  stopServices = mkSystemctlCommand "stop" serviceNames serviceSuffix;
  startServices = mkSystemctlCommand "start" serviceNames serviceSuffix;
  reloadServices = mkSystemctlCommand "reload" serviceNames serviceSuffix;
  statusServices = mkSystemctlCommand "status" serviceNames serviceSuffix;
  disableServices = mkSystemctlCommand "disable" serviceNames serviceSuffix;
  deleteServices = mkDeleteServices serviceNames serviceSuffix;

  isActive = concatStringsSep " > /dev/null && " (
    map (
      name:
      let
        serviceName = name + serviceSuffix;
      in
      "systemctl --user is-active ${serviceName}"
    ) serviceNames
  );

  activate = pkgs.writeShellScriptBin "activate" ''
    set -xeuo pipefail
    export XDG_RUNTIME_DIR="/run/user/$UID"
    mkdir -p "$HOME/.config/systemd/user/default.target.wants"
    ${copyServices}
    systemctl --user daemon-reload
    ${restartServices}

    # Check if services started
    retry_count=0
    while [[ "$retry_count" -lt 5 ]]; do
      set +e
      ${isActive} > /dev/null && exit 0
      set -e
      retry_count=$((retry_count+1))
      sleep 5
    done
    # if services didn't start exit with failure
    exit 1
  '';

  removeServices = concatStringsSep "\n" (
    map (
      name:
      let
        serviceName = name + serviceSuffix;
      in
      ''
        set +e
        ${stopServices}
        ${disableServices}
        set -e
        ${deleteServices}
      ''
    ) serviceNames
  );

  deactivate = pkgs.writeShellScriptBin "deactivate" ''
    set -xeuo pipefail
    export XDG_RUNTIME_DIR="/run/user/$UID"
    ${removeServices}
    systemctl --user daemon-reload
    systemctl --user reset-failed
  '';

in
pkgs.buildEnv {
  name = "site-service";
  paths = [
    activate
    deactivate
  ];
}
