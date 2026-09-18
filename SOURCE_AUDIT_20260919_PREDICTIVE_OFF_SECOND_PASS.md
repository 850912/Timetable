# Timetable 3.4.0 — predictive-back off / navigation second pass

Date: 2026-09-19

## Verified against current AndroidX sources

- `SwipeDismissableNavHost(userSwipeEnabled = false)` disables the Wear host's `PredictiveBackHandler` path on API 35+.
- Android's official application-level opt-out is `android:enableOnBackInvokedCallback="false"`; this disables predictive system animations while keeping `OnBackPressedCallback`/Compose `BackHandler` behavior available.
- Wear's published `PredictiveBackNavHost` transition constants were checked against the current AndroidX source. The internal child host uses the same values for ordinary enter/pop navigation; these are copied values from the AndroidX implementation, not a public API.

## Changes in this pass

1. `wear/src/main/AndroidManifest.xml`
   - Changed `android:enableOnBackInvokedCallback` from `true` to `false`.

2. `wear/src/main/java/com/hufeng943/timetable/presentation/ui/components/WearInternalNavHost.kt`
   - Kept the same Wear motion values for ordinary forward/back navigation.
   - Added `predictivePopEnterTransition = EnterTransition.None` and `predictivePopExitTransition = ExitTransition.None` so the child `NavHost` has no separate predictive visual path.
   - Retained the existing ordinary `BackHandler` pop.

3. `wear/src/main/java/com/hufeng943/timetable/presentation/ui/screens/more/settings/SettingScreen.kt`
   - Replaced the unsafe `LocalContext.current as ViewModelStoreOwner` cast with the supported `hiltViewModel()` default owner lookup.

4. `wear/src/main/java/com/hufeng943/timetable/presentation/ui/AppNavHost.kt`
   - Updated comments to accurately describe the manifest-level predictive-back opt-out.

## Language crash review

No new deterministic language-page crash was found in the current source. The Android 13+ path uses `LocaleManager.applicationLocales` and does not explicitly call `recreate()` there. The remaining API <=32 recreate path is isolated behind the SDK check.

## Build limitation

A real Gradle compile could not be completed in this sandbox because the Gradle distribution at `services.gradle.org` is not reachable. Do not treat this package as build-verified.
