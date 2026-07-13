# Kiwi — Component Specifications

## KiwiBackground

Layer order: paper base → four low-opacity organic blobs → center cream wash → grain texture → content. Blobs use large asymmetric paths, never perfect circles.

## BottomDock

Floating rounded container, 72dp tall, 20dp side margin, cream/white surface with tonal shadow. Five destinations maximum. Selected destination expands into a dark pill with icon and label; unselected items remain quiet icons.

## TaskCard

24dp radius, 16dp padding, colored category rail or top accent, title first, time second, completion control at the trailing edge. Completed state lowers contrast but remains readable.

## DateRail

Seven cells, 44–48dp diameter, weekday caption above day number. Selected state is a filled forest/charcoal cell; today gets a subtle outer ring.

## PriorityChip

Low = sage, Medium = butter/peach, High = coral. Always include text; color alone is insufficient.

## ProgressRing

Use a soft track and one saturated progress color. Center label is numeric and readable. Do not use a neon gradient.

## AssistantCard

Butter or cream surface, small Kiwi mark, interpreted action summary, explicit confirm/cancel for destructive or ambiguous commands.

## OfflineStatus

Quiet icon plus “Saved on this device” or “Sync pending”. It must not block local actions.
