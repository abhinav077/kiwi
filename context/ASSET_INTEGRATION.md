# Kiwi — Asset Integration Guide

Copy this entire documentation folder into the repository root. For Android, copy `assets/vectors/*.svg` into `app/src/main/res/drawable/` after validating Android Studio's SVG import, or convert them to Android Vector Drawable XML without changing their colors or opacity.

Recommended Compose structure:

```text
app/src/main/java/com/abhinavsirohi/kiwi/ui/theme/
app/src/main/java/com/abhinavsirohi/kiwi/ui/components/
app/src/main/java/com/abhinavsirohi/kiwi/ui/screens/
app/src/main/res/drawable/
```

Use `DESIGN_TOKENS.md` to create `Color.kt`, `Type.kt`, `Shape.kt`, and `Theme.kt`. Use `KiwiBackground` as the shared background wrapper. Keep decorative assets optional so the app remains readable and fast on lower-end devices.

The reference PNGs are for visual comparison only. Do not bundle all reference images into the release APK.
