# Full source audit — 2026-09-17

## CI blocker fixed
- `wear/.../AppNavHost.kt` used `RectangleShape` in the THEME and IMAGE fallback background paths without importing `androidx.compose.ui.graphics.RectangleShape`.
- Added the missing import. The uploaded CI log reached `:wear:compileDebugKotlin`, so the previous Android 37.2 AAR-metadata blocker is resolved.

## Build/toolchain
- Haze 2.0.0-rc01 requires Android 37.2 metadata; all Android modules already compile against 37.2.
- Project AGP is 9.2.1. CI warns that 9.2.1 was tested through 37.0. This is currently a warning, not the failing task. A toolchain upgrade should be handled separately and tested together with the Gradle wrapper rather than mixed into this compiler hotfix.

## Haze / background architecture
- Root background is the single Haze source (`hazeSource(globalHazeState)`).
- Swipe-to-dismiss scrims are overridden to transparent, avoiding a black layer covering IMAGE/THEME backgrounds.
- IMAGE decode is dispatched to IO and bitmap lifetime is tied to the composable.
- Background visibility scrim is capped at 0.35.
- Glass cards use Haze Performance mode and `expandLayerBounds = false`, appropriate for Wear GPU cost control.

## Technical debt found
- Mobile/Wear Data Layer compatibility code still uses deprecated `GoogleApiClient`, `Wearable.NodeApi`, `Wearable.DataApi`, and `Wearable.MessageApi` APIs. It compiles today but should migrate to `NodeClient`, `DataClient`, `MessageClient`, and `Asset`/client APIs in a dedicated change because transport compatibility is user-data-critical.
- Several kotlinx-datetime `monthNumber` / `dayOfMonth` usages are deprecated and should move to the newer date properties.
- `shared/build.gradle.kts` uses deprecated `srcDir(...)` DSL.
- Gradle reports debug compile/runtime classpaths being resolved during configuration, which hurts configuration-cache/build scalability.

## Settings/options review
- The current glass renderer still supports three booleans (`isGlobalGlassMaterialEnabled`, `isLiquidGlassEnabled`, `isFrostedGlassEnabled`) for compatibility. Rendering precedence is deterministic, but these represent overlapping concepts and should ultimately be represented by one material mode enum (Off/Frosted/Glass) plus effect/profile settings.
- Existing visual parameters consumed by Haze (opacity, blur enable/radius, lens distortion, chromatic aberration, effect profile) are live and should not be deleted merely as “unused”.
- Background mode, image path, and brightness are live in `AppBackground`.

## Validation status
- This environment cannot be treated as authoritative CI. The uploaded GitHub Actions log is the compiler oracle. After this hotfix, rerun the same `:shared:test :mobile:testDebugUnitTest :wear:testDebugUnitTest` workflow and use the next compiler/test failure, if any, as the next repair target.

## Dead state removed in this audit
- Removed `glassHighSaturation`: it was persisted and exposed through the ViewModel but had no renderer/UI consumer.
- Removed `blurredBackgroundEnabled` and `backgroundBlurRadius`: they were persisted/exposed but the current root background no longer reads them.
- Removed the corresponding unused ViewModel setters. Old DataStore files remain forward-safe: unknown historical preference keys are simply ignored.
- Kept legacy frosted/global preference readers and storage setters for state migration compatibility; the visible master glass switch clears both when changed.
