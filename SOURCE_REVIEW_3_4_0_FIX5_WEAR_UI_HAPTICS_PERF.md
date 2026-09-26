# 3.4.0 Fix5 — Wear UI / haptics / performance review

## Applied
- Removed the redundant “after class / next course / minutes” line from the active-course card on the home timetable.
- Added consistent haptic feedback to custom switches; existing Wear rotary snap behavior explicitly keeps haptics enabled.
- Reworked numeric glass adjustment into center-snap, clock-like scrolling: the centered value is the selected value, rotary movement ticks, confirmation has feedback, and touch/rotary share snap behavior.
- Split the former glass opacity concept into **glass tint density** (`glassOpacity`) and **glass clarity** (`glassClarity`). This preserves the visually pleasing low-opacity liquid look while allowing independent control of tint and optical transparency.
- Replaced nested `SwipeDismissableNavHost`s inside Settings / glass adjustment with normal Compose `NavHost`s. This removes the nested forward-transition artifact where the previous settings page shifted to the left and remained briefly visible. The app-level Wear swipe-dismiss navigation remains intact.
- Added Power Saver degradation: expensive blur/chromatic passes and nonessential UI animation are disabled at runtime while Android Power Saver is active, and liquid glass uses the lightweight profile. This targets throttled Galaxy Watch / Wear OS operation without changing normal-mode visuals.
- Shortened the UI-management summary to avoid unnecessary truncation on round displays.
- Kept legacy frosted/global-glass preference keys only for migration compatibility; they are no longer presented as active user-facing modes.
- Added Navigation Compose explicitly because nested settings navigation now uses the standard host.

## Source/API review
Wear Compose 1.6.x already provides rotary haptics by default for `RotaryScrollableDefaults.snapBehavior`; explicit `hapticFeedbackEnabled = true` is used on the clock-like adjuster. Google recommends pairing rotary snap behavior with `TransformingLazyColumnDefaults.snapFlingBehavior` for consistent touch and rotary snapping.

## Build verification limitation
A local Gradle compile was attempted. The sandbox did not have the Gradle 9.4.1 distribution cached and outbound access from the build shell to `services.gradle.org` failed with `UnknownHostException`, so a full compiler pass could not be completed here. GitHub Actions should resolve the wrapper normally.
