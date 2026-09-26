# Wear CI compile hotfix v2 — 2026-09-16

This package makes the CI fixes materially different from the stale Git commit so the update script will create a real source commit.

- `AppNavHost.kt`: the concrete result of `rememberLayerBackdrop()` is kept and passed directly to `Modifier.layerBackdrop(...)`; only the CompositionLocal widens it to the general backdrop abstraction.
- `TimetablePager.kt`: the course list composable closes exactly once before the top-level empty-state composable.
- `LiquidGlassAdvancedPager.kt`: `itemModifier` is explicitly `@Composable`, has an explicit `Modifier` return type, and invokes `minimumVerticalContentPadding(...)` inside that composable helper.
- Text source EOFs were normalized to exactly one newline.

Local Gradle compilation could not be completed because this environment cannot resolve `services.gradle.org`; GitHub Actions remains the authoritative compiler run.
