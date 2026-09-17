# Wear OS optical-glass home rework — 2026-09-17

## Goal
Remove the coloured-plastic look from timetable cards while retaining real backdrop refraction and keeping GPU work appropriate for Wear OS.

## Source changes
- CourseCard glass body is now optically neutral (`Color.Transparent`) instead of theme `surfaceContainer`.
- Course colour no longer washes the full card. It is restricted to a 2–3dp edge reflection, progress/state text, and progress bar.
- Card outline and order badge were reduced to neutral white optical rims; the order badge no longer has a translucent filled disc.
- GlobalLiquidGlass explicitly supports a neutral optical surface: backdrop/vibrancy/lens/highlight remain active while the material fill is omitted.
- Android < 13 / missing-backdrop fallback uses a neutral white frosted gradient rather than an opaque/material-coloured fill.
- Existing single shared LayerBackdrop architecture remains intact; no per-card backdrop provider was added.

## Web-grounded design/performance basis
- Kyant AndroidLiquidGlass/Backdrop is retained for Compose refraction rather than replacing it with a hand-written shader.
- Real liquid-glass examples built on Kyant use captured backdrop + lens refraction + optional blur/chromatic aberration.
- Android guidance recommends removing unnecessary backgrounds and reducing transparency/overdraw.
- Wear OS Compose guidance prioritizes release measurement, R8 and Baseline Profiles; those measurement tasks require device/CI validation and are not claimed complete here.

## Build status
Local Gradle compilation is not claimed. Run GitHub Actions as compiler oracle for the current dependency/API combination.
