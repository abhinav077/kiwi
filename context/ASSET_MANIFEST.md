# Kiwi — Asset Manifest

## Bundled vector assets

- `assets/vectors/kiwi-mark.svg` — small brand mark/leaf symbol.
- `assets/vectors/botanical-sprig.svg` — restrained botanical line art.
- `assets/vectors/organic-blob-coral.svg` — coral background field.
- `assets/vectors/organic-blob-sage.svg` — sage background field.
- `assets/vectors/organic-blob-lavender.svg` — lavender background field.
- `assets/vectors/organic-blob-blue.svg` — powder-blue background field.
- `assets/vectors/paper-grain.svg` — subtle reusable texture overlay.
- `assets/vectors/sparkle-cluster.svg` — small emotional accent for empty/hero states.

## Rules

- Import SVGs as Android vector drawables or use them as source assets after optimization.
- Keep decorative assets behind content and at low opacity.
- Do not introduce stock photos or random illustrations without updating this manifest and `PROGRESS_TRACKER.md`.
- Generated raster illustrations, if later approved, must be named, versioned, and linked from the relevant screen specification.
