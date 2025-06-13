#!/usr/bin/env /bash
sizes=(176 288 472 672)

for size in "${sizes[@]}"; do
  height=$((size * 10 / 9))

for img in orig/*.{jpg,png,webp}; do
    [ -f "$img" ] || continue
    base=$(basename "${img%.*}")
    vips thumbnail "$img" \
      "${base}-${size}w.webp[Q=85,strip]" \
      ${size} \
      --height=${height} \
      --size=force \
      --export-profile=srgb \
      --intent=perceptual
  done
done
