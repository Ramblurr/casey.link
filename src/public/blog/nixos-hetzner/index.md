{
    :page/title  "Reusable NixOS images on Hetzner Cloud"
    :blog/date "2025-12-19"
    :blog/author "Casey Link"
    :blog/description "Deploy NixOS configurations to Hetzner Cloud, fast."
    :blog/tags #{"nix" "hetzner" "devops" "cloud"}
}


Hetzner is a price-competitive and conceptually simpler alternative to AWS and the other hyperscalers for the small orgs and teams that [I tend to work with][ol].


NixOS is a declarative, reproducible operating system that turns 'works on my machine' into 'works on every machine' - infrastructure-as-code that's finally achievable for the lean teams that I tend to work with.

But Hetzner doesn't ship NixOS images, just the standard debian, ubuntu, and rhel clones.

Most folks resort to [using nixos-infect][infect-article] or [nixos-anywhere][anywhere-article] to transmogrify a Debian or Ubuntu instance into NixOS.
The more ambitious reach for [Packer and rescue mode][packer-article], which requires munging around manually with Hetzner's rescue mode.

All three share the same fundamental ritual: provision a VM, SSH in, and overwrite its soul with NixOS.
These approaches work, but you pay the conversion tax every time you spin up a new VM - and the demons don't work for free nor are they particularly fast.

But the winds have shifted and three things have fallen into place:

First, [hcloud-upload-image][hcloud-upload-image] was released in 2024.
It's a simple Go tool that handles the soul conversion for you. hcloud-upload-image takes a disk image as input and side effects Hetzner with enough conviction that a Snapshot materializes.

```bash
hcloud-upload-image upload \
--image-path  result/nixos-image-25.11.x86_64-linux.img \
  --architecture x86
... long wait while dark arts are performed...
Uploaded Image: 123467
```

Second, [Stéphan Kochen][stephank] ([gh][stephank-gh]) opened [nixpkgs PR #375551][pr] to bring native Hetzner Cloud image building into nixpkgs.
The PR packages hcloud-upload-image, adds a NixOS image builder config, and includes his own [systemd-network-generator-hcloud][sng-hcloud] tool for IPv6 autoconfiguration.

Third, [FlakeHub Cache][cache], available since late 2024, changes how you deploy NixOS configurations.
Normally, deploying a NixOS config means evaluating the entire flake on the target machine - slow, memory-hungry, and painful on a cheap VPS.
FlakeHub pre-computes [resolved store paths][store-paths] when you publish your flake, so `fh apply nixos` skips evaluation entirely.
Push your config from CI, deploy to your server with one command, and the configuration arrives in seconds rather than minutes.

Separately, these tools solve pieces of the puzzle.
Together, they enable a workflow that didn't exist before: Building reusable NixOS images for Hetzner Cloud!

I made a flake that packages these pieces into a ready-to-use solution for Hetzner Cloud images.
I wrote the glue; the people above wrote the magic. The code is at [outskirtslabs/nixos-hetzner][proj].

As of writing, PR #375551 is still open. My flake vendors the relevant pieces so you don't need to wait for merge in the meanwhile.

The images can be built for both x86_64-linux and aarch64-linux, and come bundled with [Determinate Nix][det-nix] and the [FlakeHub CLI][fh]. Using it looks like this:[^note2]

[^note2]: Surprisingly, Hetzner's ARM instances (CAX series) no longer have a price advantage over the Intel/AMD CX series. In fact they are more expensive from $0.50-$1.00.


```bash
HCLOUD_TOKEN=...your hcloud token...
ARCH=x86_64-linux # or aarch64-linux
HCLOUD_ARCH=x86   # or arm

nix build "github:outskirtslabs/nixos-hetzner#diskImages.$ARCH.hetzner" --print-build-logs

# inspect the image
ls result/*
IMAGE_PATH=$(ls result/*.img 2>/dev/null | head -1)

# upload to hetzner cloud
hcloud-upload-image upload \
    --image-path="$IMAGE_PATH" \
  --architecture="$HCLOUD_ARCH" \
  --description="nixos-hetzner image"
```

With that you have a NixOS Snapshot in your Hetzner Cloud console that you can clickops into a fresh VM. Once you've booted a VM from the image, you can authenticate with FlakeHub and use `fh apply` to deploy configurations directly.

FlakeHub Cache makes this fast - *really fast*.
Instead of rebuilding or waiting for slow binary cache downloads, your configurations deploy in seconds.

You'll need to bring your own paid Hetzner and FlakeHub accounts of course.

For a more complete example, with Terraform/Opentofu and Github Actions, check out the [outskirtslabs/nixos-hetzner-demo][demo].
It builds on nixos-hetzner and showcases a full continuous deployment methodology with NixOS.

And, FYI, going into Q1/Q2 of 2026, my consulting calendar still has openings:
If you are a small to medium org or team who needs a devops assist or Clojure full-stack reinforcement, [get in touch][contact]. [^note1]

[^note1]: Happy holidays folks, I'm hoping for a 2026 where Hetzner makes all of the above redundant by just supporting NixOS natively.



[ol]: https://outskirtslabs.com
[proj]: https://github.com/outskirtslabs/nixos-hetzner
[demo]: https://github.com/outskirtslabs/nixos-hetzner-demo
[hcloud-upload-image]: https://github.com/apricote/hcloud-upload-image
[pr]: https://github.com/NixOS/nixpkgs/pull/375551
[cache]: https://docs.determinate.systems/flakehub/cache
[det-nix]: https://docs.determinate.systems/determinate-nix
[detsys]: https://determinate.systems
[nixos-amis]: https://github.com/DeterminateSystems/nixos-amis
[fh]: https://docs.determinate.systems/flakehub/cli
[infect-article]: https://guillaumebogard.dev/posts/declarative-server-management-with-nix/
[anywhere-article]: https://joinemm.dev/blog/nixos-hetzner-cloud
[packer-article]: https://developer-friendly.blog/blog/2025/01/20/packer-how-to-build-nixos-24-snapshot-on-hetzner-cloud/
[stephank]: https://stephank.nl/
[stephank-gh]: https://github.com/stephank
[sng-hcloud]: https://github.com/stephank/systemd-network-generator-hcloud
[store-paths]: https://docs.determinate.systems/flakehub/store-paths/
[contact]: https://outskirtslabs.com/#contact
