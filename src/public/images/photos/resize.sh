#!/usr/bin/env /bash
sizes=(176 288 472 672)

for size in "${sizes[@]}"; do
  for img in home/*.{jpg,png,webp}; do
    [ -f "$img" ] || continue
    base=$(basename "${img%.*}")
    vips thumbnail "$img" \
      "${base}-${size}w.webp[Q=85,strip]" \
      ${size} \
      --export-profile=srgb \
      --intent=perceptual
  done
done

sizes=(300 600 900 1236)
for size in "${sizes[@]}"; do
  for img in about/*.{jpg,png,webp}; do
    [ -f "$img" ] || continue
    base=$(basename "${img%.*}")
    vips thumbnail "$img" \
      "${base}-${size}w.webp[Q=85,strip]" \
      ${size} \
      --export-profile=srgb \
      --intent=perceptual
  done
done
