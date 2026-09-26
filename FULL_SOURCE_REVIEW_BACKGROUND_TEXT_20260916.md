# Full source review — background + long text (2026-09-16)

## Scope
Reviewed all 201 Kotlin source/test files across shared, mobile and wear after the changes below, with targeted checks for the previous Wear compiler failures, Room/sync/adjustment logic, blocking/background work, text overflow, and the Liquid Glass rendering path.

## Changes
- Added timetable background modes on Wear: solid, theme-derived static glow, and custom image.
- Theme glow is the default and is static (no perpetual animation).
- Custom images are decoded once when selected, downscaled to at most 512 px on the longest side, compressed into app-private storage, then decoded off the main thread for the timetable surface.
- The selected background is also the Backdrop refraction source when Liquid Glass is enabled; no duplicate animated background layer is introduced.
- Added DataStore persistence for background mode/image path and a settings subpage using the existing SwipeDismissableNavHost navigation.
- Removed avoidable truncation from generic Wear capsule cards: titles/subtitles now wrap to content height by default.
- Course names can use up to 3 lines on the home glass card; legacy watch course/location/teacher text can wrap.
- Details course title and info rows now wrap instead of relying on endless marquee for user content.
- Mobile timetable name, upcoming event text, and the long quick-action hint now wrap to two lines.
- Remaining one-line Wear texts are short status labels (for example current/next course), not user-authored names/descriptions.

## Review findings
- Previous compiler fixes remain present: `resolveDate` is imported in CourseAdjustmentScreen, no external access to private `selectedTimeSlot` remains in that screen, and the invalid foundation RectangleShape import is absent.
- Background settings are presentation/preferences only and do not modify schedule, Room, sync, reminder, adjustment or undo semantics.
- Image preprocessing runs on Dispatchers.IO. The timetable bitmap decode also runs on Dispatchers.IO via produceState.
- No new polling loop, timer, GlobalScope, or blocking database call was added.
- Existing `while(true)` loops in TimetablePager are suspending event/time-boundary loops, not busy loops.
- Existing production `runBlocking` / legacy Wear transport sleeps remain technical debt and were not mechanically rewritten because their callback/service lifetime needs a dedicated migration.
- XML resource parsing passed for all strings.xml files.
- Input ZIP and output ZIP integrity checks passed.

## Build verification
Attempted:
`./gradlew --no-daemon :shared:test :mobile:testDebugUnitTest :wear:testDebugUnitTest`

The local sandbox still cannot resolve `services.gradle.org`, so Gradle 9.4.1 cannot be downloaded here and Kotlin compilation cannot start. The prior real CI compiler errors have been specifically rechecked in source, but this artifact still requires CI for authoritative compilation.
