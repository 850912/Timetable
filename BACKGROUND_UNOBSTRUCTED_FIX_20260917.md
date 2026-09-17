# Background unobstructed fix — 2026-09-17

## Root cause
`AppBackground` always painted `AppTheme.colors.background` and then painted a full-screen black alpha scrim after THEME/IMAGE content. That last full-screen layer was literally above the selected fluid/image background. It also added avoidable alpha overdraw before the Backdrop glass was sampled.

## Fix
- Removed the full-screen black brightness scrim completely.
- SOLID is now the only mode that paints the opaque app background as its root content.
- THEME renders the ambient/fluid layer directly as the root pixels.
- IMAGE renders the selected bitmap directly as the root pixels.
- Background brightness for IMAGE is applied with a ColorMatrix to the image itself, not by covering it with a black translucent Box.
- Missing IMAGE falls back directly to the theme ambient layer.
- Existing shared `layerBackdrop` remains on the root background, so Kyant glass samples the actual fluid/image background rather than a covering layer.

## Why
Android's rendering guidance recommends removing unnecessary backgrounds and transparent overlays because they create overdraw. Kyant Backdrop also expects the backdrop source to contain the real pixels that glass should refract.
