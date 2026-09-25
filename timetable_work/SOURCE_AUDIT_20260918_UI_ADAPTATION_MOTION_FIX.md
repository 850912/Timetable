# Timetable 3.4.0 — UI adaptation / motion / picker fix (2026-09-18)

This pass was based on the current source tree, not only on the previous audit notes. The focus was the Wear OS UI symptoms shown in the supplied screenshots: child-page transitions exposing the previous page at the left edge, liquid-glass numeric controls overflowing/clipping, and time/date pickers showing dark rectangular gradient bands on the app's custom background.

## Changes applied

### 1. Child navigation now uses one centralized Wear motion policy

Added `WearInternalNavHost` for child flows inside the app-level `SwipeDismissableNavHost`.

- The app-level navigation remains `SwipeDismissableNavHost`, which is the Wear OS navigation component responsible for swipe-to-dismiss / predictive back.
- Child flows continue to use a plain Navigation Compose host so they do not create a second swipe-dismiss surface.
- Removed seven copies of hard-coded `140ms/100ms` tween fades.
- Child transitions now use Wear Material 3 `MaterialTheme.motionScheme.fastEffectsSpec()` for alpha-only transitions.
- When the app's `uiAnimationsEnabled` setting is off, child navigation transitions are `EnterTransition.None` / `ExitTransition.None`.
- Avoiding child spatial translation removes the class of visual artifact where a previous child page can remain visible at the circular left edge while the next page is already on screen.

Affected flows: Settings, Liquid Glass settings, edit timetable, edit time slot, schedule tools, course adjustment, day arrangement.

### 2. Wear Material 3 MotionScheme is now explicit in the app theme

`TimetableTheme` now installs `MotionScheme.standard()`.

Custom interaction motion was changed to use the theme motion scheme:

- Press scale -> `fastSpatialSpec()`.
- Switch thumb movement -> `fastSpatialSpec()`.
- Disabled UI animations -> `snap()`.
- Pull-to-date-picker settle no longer uses a fixed 300ms tween; it uses a no-bounce Compose spring, which remains interruptible and velocity-continuous.

This removes scattered timing constants and makes motion consistent with Wear Material 3.

### 3. Liquid-glass numeric adjustment now uses the official Wear `Picker`

The former implementation used a `TransformingLazyColumn` as a hand-built numeric wheel. On a round screen it allowed too many items to remain visible under the title/edge-button area, producing the clipping shown in the screenshot.

It now uses Wear Material 3 `Picker` + `rememberPickerState`:

- centered selected value;
- official rotary snap behavior;
- non-repeating bounded ranges;
- stable selected index;
- haptic tick only when the selected option changes;
- `gradientColor = Color.Unspecified`, as recommended by the Picker API when the app draws a custom image/gradient background, preventing opaque gradient rectangles.

### 4. TimePicker / DatePicker are no longer incorrectly wrapped in `ScreenScaffold`

Wear Material 3 `TimePicker` and `DatePicker` are full-screen components. The old code nested them inside `ScreenScaffold`, reducing the viewport and allowing the custom app background to show through while the picker internally faded to `MaterialTheme.colorScheme.background` (black in this app). That mismatch is the source of the black rectangular bands visible around picker values.

Added `WearTimePickerPage` / `WearDatePickerPage` wrappers that:

- give the full screen to the official picker;
- draw the exact Material theme background behind it so the picker gradient is continuous;
- do not add a second scaffold/time-text layer.

All time/date picker entry points in timetable editing, time-slot editing, schedule tools, course adjustment and day arrangement now use these wrappers.

## Source-wide motion scan

After the changes, the Wear source no longer contains hard-coded `tween(...)` navigation transitions. Remaining animation APIs are limited to:

- official `AnimatedVisibility` for the home page indicator;
- layout-stable `AnimatedContent` with no enter/exit animation for edit-state swaps;
- `animateFloatAsState` backed by Wear Material MotionScheme for press/switch feedback;
- `Animatable` with an interruptible spring for pull-to-date-picker settling;
- the app-level official Wear `SwipeDismissableNavHost`.

No custom slide-left/slide-right destination animation remains in child navigation.

## Verification

Static checks performed:

- scanned the full mobile/shared/wear source for animation APIs and navigation hosts;
- confirmed only one `SwipeDismissableNavHost` remains (the app-level host);
- confirmed all child navigation hosts use the centralized helper;
- confirmed all full-screen time/date picker call sites use the full-screen wrappers;
- confirmed no hard-coded `tween(...)` remains in Wear UI source.

A local Gradle compile was attempted, but this sandbox cannot resolve `services.gradle.org`, so Gradle 9.4.1 cannot be downloaded. No local build-success claim is made. The project is intended to be built by the user's GitHub Actions workflow.

## Official Android references used

- Wear Compose navigation: app-level `SwipeDismissableNavHost`, `AppScaffold` and `ScreenScaffold` behavior.
- Wear Material 3 `MotionScheme`: `standard()`, `fastEffectsSpec()`, `fastSpatialSpec()`.
- Compose animation guidance: springs are the default/interruption-friendly animation model; use graphics/draw-layer transforms when possible.
- Wear Material 3 `Picker`: recommends `PickerDefaults.rotarySnapBehavior`; for custom backgrounds, `gradientColor = Color.Unspecified` avoids mismatched gradient backgrounds.
- Wear Material 3 `TimePicker`: full-screen picker API.
