# Kiwi — Implementation Design Tokens

These tokens are the single source of truth for Compose UI implementation. Do not invent screen-specific colors or spacing unless a screen specification explicitly overrides them.

## Colors

```kotlin
val KiwiPaper = Color(0xFFFAF4EA)
val KiwiWarmBeige = Color(0xFFF5EDE2)
val KiwiCream = Color(0xFFFFF9F1)
val KiwiCharcoal = Color(0xFF292724)
val KiwiWarmGray = Color(0xFF766F68)
val KiwiCoralRose = Color(0xFFF56F7D)
val KiwiBlush = Color(0xFFF8B8B2)
val KiwiPeach = Color(0xFFF6C7A6)
val KiwiButter = Color(0xFFF3C65D)
val KiwiSage = Color(0xFFA9BE83)
val KiwiPistachio = Color(0xFFD8E4B8)
val KiwiForest = Color(0xFF275C3B)
val KiwiPowderBlue = Color(0xFFAFCFE6)
val KiwiPeriwinkle = Color(0xFFB8B7E8)
val KiwiLavender = Color(0xFFD7C7E8)
val KiwiWhite = Color(0xFFFFFFFF)
```

## Spacing and dimensions

Use a 4dp base grid: `4, 8, 12, 16, 20, 24, 32, 40, 48`.

- Screen horizontal padding: 20dp phone, 32dp tablet.
- Section gap: 24dp.
- Card internal padding: 16dp, larger hero cards 20dp.
- Minimum interactive target: 48dp.
- Bottom dock height: 72dp plus safe-area inset.
- Standard card radius: 24dp; hero radius: 32dp; chips: 999dp.
- Standard icon: 24dp; compact icon: 20dp; hero icon: 28dp.

## Elevation and surfaces

Prefer tonal layering to shadows. Standard shadow: `0dp 6dp 20dp 8%` charcoal. Avoid heavy Material elevation.

## Typography

- Display: 32sp, line height 38sp, weight 700.
- Screen title: 28sp, line height 34sp, weight 700.
- Section title: 20sp, line height 26sp, weight 700.
- Body: 16sp, line height 23sp, weight 400.
- Supporting: 14sp, line height 20sp, weight 500.
- Caption: 12sp, line height 16sp, weight 600.
- Diary accent may use a licensed handwritten font only for user-authored diary text.

## Background recipe

Use `KiwiPaper` as the base. Add four large low-opacity organic blobs (coral top-left, sage top-right, lavender bottom-right, powder-blue bottom-left), a soft cream center wash, and the `paper-grain.svg` overlay at 3–5% opacity. Blobs must stay behind content and never reduce contrast.

## Category colors

- Planner/work: `KiwiCoralRose` with blush tint.
- Personal/self-care: `KiwiSage` with pistachio tint.
- Wellness: `KiwiLavender` with periwinkle tint.
- Diary: `KiwiPeach` with cream tint.
- Assistant: `KiwiButter` with cream tint.
