# Haze migration — 2026-09-17

## What changed

- Removed `io.github.kyant0:backdrop:2.0.1` from the Wear module.
- Added Haze `2.0.0-rc01`: `haze`, `haze-blur`, and `haze-glass`, all pinned to the same version.
- Replaced the app-wide Kyant `LayerBackdrop` with one shared `HazeState` and `Modifier.hazeSource()` on the app background.
- Replaced custom liquid-glass shaders with `Modifier.hazeGlass()` / `GlassStyle`.
- Replaced frosted glass with `Modifier.hazeBlur()` / `HazeBlurStyle`.
- Course cards now use the same global glass modifier as the rest of the app; the old special per-card Kyant pipeline was removed.
- Global Glass Material now uses Haze Glass rather than the previous hand-drawn translucent gradient.
- Wear uses `HazePerformanceMode.Performance` and `expandLayerBounds = false` for both blur and glass.
- Removed the API 33 gate. Haze owns platform fallback behavior.

## Performance policy

The app has one shared source state. It does not create a new backdrop engine per card. The Wear profile deliberately disables the extra refraction-detail pass, keeps chromatic aberration at 0.10 maximum, uses modest blur/refraction values, and disables expanded effect bounds.

## Verification

Static source checks confirm there are no remaining `com.kyant` imports or Kyant dependency declarations in the Wear module. A real Gradle compile was attempted, but this sandbox cannot resolve `services.gradle.org`, so Gradle 9.4.1 could not be downloaded. This package therefore does **not** claim a successful compile.

## Upstream references checked

Haze 2.0.0-rc01 changelog, Glass documentation, Haze 2 migration guide, and source/state recipes were checked on 2026-09-17 before the migration.
