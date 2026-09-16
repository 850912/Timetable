# Wear performance / power audit — 2026-09-16

Scope: Wear OS module, with emphasis on foreground jank, unnecessary wakeups/recomposition, and allocation pressure. Changes intentionally preserve timetable accuracy and UI behavior.

## Applied fixes

1. **Pull-to-date gesture hot path**
   - Replaced per-drag `CoroutineScope.launch { Animatable.snapTo(...) }` with direct `mutableFloatStateOf` updates.
   - Settling animation now allocates/runs only when the gesture ends.
   - This avoids creating/cancelling large numbers of short-lived coroutines while dragging or nested-scrolling.

2. **Effect churn while dragging**
   - Effects in `TimetablePager` are keyed by the discrete picker-open state instead of continuously changing pixel offset.
   - Focus and pager-lock side effects now run only when the picker crosses closed/open state.

3. **Repeated gradient allocations**
   - Cached Galaxy ambient gradient brushes with `remember`.
   - Cached course-card highlight/accent/progress brushes with `remember`.
   - This reduces allocations during scrolling and the minute-boundary course-state refresh.

4. **Existing power fixes retained**
   - No unconditional Tile/Complication refresh from `MainActivity.onStop()`.
   - Tile timeline remains time-bound, with hourly freshness fallback.
   - Home status refresh remains minute-boundary based rather than a 1 Hz loop.
   - Release build keeps R8 minification and resource shrinking enabled.

## Findings deliberately not changed without device measurements

- The two optional current/next complication providers request 10-minute system refreshes. They only matter when configured on a watch face, and reducing their interval would make static progress/minutes stale. Converting them to fully dynamic complication expressions should be validated against the target watch/API before changing behavior.
- The home UI uses static gradients, but no runtime blur/shader or infinite animation was found. Removing the visual treatment without GPU/frame measurements would be speculative.
- `WAKE_LOCK` is declared but there is no direct app wakelock acquisition in the Wear source. Permission presence itself does not hold a wakelock, so removal was not treated as a power fix.

## Validation status

Static review performed after the changes:
- searched for remaining drag-offset-keyed `LaunchedEffect` instances in the affected flow;
- verified `PullToDatePickerState` call sites match the new constructor;
- verified no per-drag coroutine launch remains in `snapTo`;
- reviewed gesture close/open, focus restoration, and nested-scroll behavior;
- re-reviewed modified gradient creation and keys.

A real Kotlin/Android compile was attempted with `./gradlew :wear:compileDebugKotlin --no-daemon`, but the environment cannot resolve `services.gradle.org`, so Gradle 9.4.1 cannot be downloaded. This is an environment/network failure, not a compiler result.

## Device validation recommended before judging battery improvement

Google recommends drawing final performance conclusions from **release builds on physical Wear OS devices**. For the reported ~10% / 40 min drain, compare the same interaction script before/after using `adb shell dumpsys batterystats`, and inspect frame timing/jank in a release or benchmark build. Screen-on interactive mode itself is a high-power state, so separate display power from app CPU/GPU work when interpreting the result.

References:
- Android Developers — Conserve power and battery (Wear OS): https://developer.android.com/training/wearables/apps/power
- Android Developers — Jetpack Compose performance on Wear OS: https://developer.android.com/training/wearables/compose/performance
- Android Developers — Jetpack Compose performance: https://developer.android.com/develop/ui/compose/performance
