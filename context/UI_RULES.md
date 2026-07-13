# Kiwi — UI/UX Rules

## Emotional direction

Kiwi should feel like a gentle personal space that also helps the user act. The interface should be happy and expressive without becoming loud, childish, or visually exhausting.

## Visual language

- Warm ivory/paper base with richer coordinated accent shades.
- Editorial typography for major moments and headings.
- Highly readable sans-serif for tasks, dates, analytics, and health information.
- Handwritten typography only for diary content and small personal accents.
- Organic layered blobs and paper-like textures used purposefully.
- Botanical line art used sparingly.
- Color should communicate category, state, and emotional tone—not decorate every surface.
- Use a floating pill/dock navigation pattern where it improves hierarchy.
- Avoid repetitive stacks of identical white cards.

## Layout

- Design for phones first, then adapt to tablets.
- Preserve clear hierarchy above the fold.
- Use scrolling intentionally; do not squeeze every feature into one screen.
- Keep important actions reachable with one hand on a phone.
- Use bottom sheets for focused creation flows when appropriate.
- Provide clear empty states for a first-time user; never invent fake data as if it were real.

## Accessibility

- Maintain readable contrast even with pastel surfaces.
- Never communicate status by color alone.
- Give icons content descriptions.
- Keep interactive targets comfortably tappable.
- Respect system font scaling as far as the layout allows.
- Do not use handwritten fonts for instructions, medical information, or dense data.

## Interaction rules

- Safe creation normally executes immediately.
- Updates execute and show the result.
- Deletes require confirmation.
- Bulk changes require confirmation.
- Ambiguous health-log changes require clarification.
- Offline saves show a subtle pending-sync state, not a blocking error.
- Loading states should preserve the surrounding layout and avoid jarring jumps.
- Animations should communicate state changes and remain short and purposeful.

## Screen rules

### Today

The Today screen prioritizes the next meaningful action, progress, time-of-day groups, self-care, and quick actions. It should not become an analytics dashboard.

### Calendar

Calendar supports history and future planning. It should show completion and recorded wellness indicators without making predictions.

### Wellness

Wellness presents recorded facts and historical summaries. It must not imply a diagnosis, forecast, fertility window, or medical certainty.

### Diary

Diary is intimate, calm, private, and writing-led. It should feel different from the planner while remaining consistent with the design system.

### Assistant

Ask Kiwi is a focused command interface with suggested commands, interpreted actions, and clear confirmation. It must not pretend to have open-ended intelligence.

### More/settings

Secondary features belong here: self-care, downloads, analytics, themes, privacy, backup/sync, and account.