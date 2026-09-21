# Wear OS 6 alignment — stage 3

This stage continues the 3.5.3 Wear OS 6 / Google alignment work.

## Navigation

- Removed feature-level `SwipeDismissableNavHost` instances.
- Kept a single app-level `SwipeDismissableNavHost` in `AppNavHost`.
- Promoted Settings, Liquid Glass numeric pages, timetable editing, time-slot editing,
  schedule tools, day arrangement and course adjustment to app-level destinations.
- Scoped editor ViewModels to their navigation graphs so draft state survives child pages.

## Scaffold / time

- Liquid Glass numeric picker pages use `ScreenScaffold(timeText = {})` so their title does
  not collide with the global Wear `TimeText`.
- Existing app-level 24-hour / 12-hour `TimeText` behavior is preserved.

## Home schedule

- Added Morning / Afternoon section headers.
- Home course cards use `toScheduleCompactString()` (`08:00`).
- Time-slot editor uses `toScheduleEditorString()` (`上午 08:00`).

## Watch face

- Uses the user-supplied image directly; no AI redraw.
- Background image is cropped to 450×450 and stored as `watchface_catgirl_bg.png`.
- WFF interactive mode: photo + time + date + next-class complication.
- WFF ambient mode: black background + time/date; photo and complication hidden.
- The next-class complication prefers `NextCourseComplicationService`, with `NEXT_EVENT`
  as the system fallback.
- Watch face editing is enabled.

## Verification status

Static checks completed:

- only one `SwipeDismissableNavHost` / `rememberSwipeDismissableNavController` remains;
- no references to the removed internal navigation helpers remain;
- all XML resources parse as well-formed XML;
- all `NavRoutes` references resolve to defined route constants;
- all app-host destination composables referenced by `AppNavHost` exist;
- the new watch-face artwork and preview are 450×450 PNGs.

Full Gradle compilation could not be executed in this sandbox because the project wrapper requires
Gradle 9.4.1 and the environment cannot reach `services.gradle.org`. The wrapper fails before Kotlin
compilation begins. Real-device swipe animation / WFF rendering still needs a Wear OS 6 device or
emulator pass, especially Xiaomi Watch 5 validation.
