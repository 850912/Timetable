# Timetable 3.4.0 — Navigation + Language Stability Fix (2026-09-19)

## User-visible problems addressed

1. Opening a new Wear page could visibly move/slide left, pause, then disappear or leave a ghost frame.
2. Navigation animation was implemented by multiple nested full-screen navigation hosts.
3. Opening the Settings > Language page could crash.
4. Language changes still relied on a custom `attachBaseContext` + `Activity.recreate()` path on modern Android.

## Root cause found in this source tree

The app already had a root `SwipeDismissableNavHost` in `AppNavHost`, but seven child flows created another `SwipeDismissableNavHost` inside that destination. The child host owns its own full-screen predictive-back surface and its own entry/exit animation. This makes a top-level destination and its first child destination animate independently.

The previous audit only disabled the child swipe gesture at its start destination. That did not remove the second host's entry animation, so the double-transition structure remained.

Google's Wear implementation contains a single self-contained `SwipeDismissableNavHost` with its own entry/pop transitions. The supplied source now follows the same separation: one Wear navigation host for app-level navigation, while child flows use the standard Navigation Compose back stack with the same motion constants used by Google's Wear `PredictiveBackNavHost`.

## Navigation changes

### `WearInternalNavHost.kt`

- Removed nested `SwipeDismissableNavHost`.
- Uses `androidx.navigation.compose.NavHost` for child flows.
- Uses Google's Wear navigation transition definitions verbatim in structure:
  - enter: half-width slide + scale-in + fade-in spring
  - exit: half-width slide + scale-out + fade-out spring
  - pop-enter: linear scale/slide/fade
  - pop-exit: linear slide/scale
- This keeps child navigation motion visually aligned with the official Wear motion instead of the previous custom fade/duplicate full-screen host.

### Child flow controllers

The following child flows now use `rememberNavController()` + Navigation Compose `composable`:

- Settings
- Liquid Glass advanced settings
- Edit timetable
- Edit time slot
- Schedule tools
- Course adjustment
- Day arrangement

Only the app-level `AppNavHost` keeps `rememberSwipeDismissableNavController()` and `SwipeDismissableNavHost`.

## Language changes

### Android 13+

The in-app language picker now writes the preference and applies the locale through the Android framework `LocaleManager.setApplicationLocales()` API. The framework performs the resulting configuration/lifecycle transition, so the app no longer calls `Activity.recreate()` after setting a modern app locale.

### Android 12 and below

The existing synchronous preference mirror is retained only for the legacy `attachBaseContext` path, followed by the existing recreate event.

### Existing-install migration

On Android 13+, the activity checks the previous mirror once. If an old installation has a saved non-system language but no framework app locale yet, it migrates that language into `LocaleManager`.

### Locale declaration

Added `res/xml/locale_config.xml` for `en` and `zh-CN` and referenced it with `android:localeConfig` in the Wear application manifest.

### Language page stability

The language picker no longer restores a custom lazy-column anchor across a configuration/locale change. It uses the normal `TransformingLazyColumn` state and keeps stable non-null item keys.

## Static verification

- Exactly one production `SwipeDismissableNavHost` remains: `AppNavHost`.
- Exactly one production `rememberSwipeDismissableNavController()` remains: `AppNavHost`.
- No child Wear flow still imports the Wear navigation controller or Wear `composable` DSL.
- No language `@null` sentinel / obsolete language string arrays remain.
- No custom destination `tween(...)` remains outside the centralized Google-derived transition policy in `WearInternalNavHost.kt`.
- Existing Material 3 `MotionScheme.standard()` remains the app theme motion scheme.

## Build verification limitation

A real `:wear:compileDebugKotlin` run was attempted. The container could not resolve `services.gradle.org`, so Gradle 9.4.1 could not be downloaded. Therefore this delivery does **not** claim a successful local compilation. The remaining validation step is the project's GitHub Actions build on an environment with normal access to the Gradle distribution and Maven repositories.

## Official references used

- Android per-app language preferences:
  https://developer.android.com/guide/topics/resources/app-languages
- Android `LocaleManager` API:
  https://developer.android.com/reference/kotlin/android/app/LocaleManager
- Compose Navigation predictive back:
  https://developer.android.com/develop/ui/compose/system/predictive-back-setup
- Wear Compose navigation:
  https://developer.android.com/training/wearables/compose/navigation
- Google AOSP Wear predictive-back navigation implementation:
  https://android.googlesource.com/platform/frameworks/support/+/76d0b0fea265ed90a4ea030b0bc54e0f9fcf522d/wear/compose/compose-navigation/src/main/java/androidx/wear/compose/navigation/PredictiveBackNavHost.kt
