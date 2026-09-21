# Watch Face redesign — 2026-09-21

## Direction

The previous centered dashboard-style layout was replaced with a quieter anime/editorial composition inspired by current Wear OS anime watch-face patterns: character-first artwork, time placed in negative space, and only one compact complication.

## Changes

- Keeps the user's original cat-girl artwork; no AI redraw or character replacement.
- New 450x450 crop preserves both ears, face, eyes, and right-side hair/tail area.
- Adds a subtle left-side readability grade only.
- Replaces the large centered `HH:mm` block with stacked hour/minute typography on the left.
- Uses `hourFormat="SYNC_TO_DEVICE"` so the clock follows the watch's 12/24-hour preference.
- Keeps date as quiet secondary information.
- Renders the next-course complication as `COMPLICATION.TITLE` (dynamic countdown) + `COMPLICATION.TEXT` (course name).
- Ambient mode hides artwork, date, and complication; only a dimmed stacked clock remains on black.

## Key resources

- `watchface-nacho/src/main/res/raw/watchface.xml`
- `watchface-nacho/src/main/res/drawable/watchface_catgirl_bg_editorial.png`
- `watchface-nacho/src/main/res/drawable/preview.png`
