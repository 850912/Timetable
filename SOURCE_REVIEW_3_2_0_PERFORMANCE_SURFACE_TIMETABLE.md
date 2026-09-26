# Timetable 3.2.0 source review

Base: Timetable 3.1.0 Fix7 Performance / Premium Timetable.

## Scope

- Wear cold-start contention reduction.
- Event-driven Tile / Complication refresh after local edits.
- Tile Timeline retained as the time-bound update mechanism.
- Current-class progress visualization.
- Semester week progress (current / total weeks when semesterEnd exists).
- Mobile Today timetable visual hierarchy.
- Semantic version update to 3.2.0.

## Cold start

- Removed the `onPostResume()` Tile refresh that ran 1.5 seconds after every foreground entry.
- China Wear bridge bootstrap remains enabled but is delayed from 2.5 seconds to 7 seconds so the launcher/Room/Compose cold path settles first.
- No Hybrid Data Layer transport implementation was replaced or globally downgraded.

## Wear surfaces

- Local Wear timetable/course/time-slot upsert and delete operations now call `WearSurfaceRefresher.refresh()` immediately after repository mutation.
- Phone -> watch sync receiver already refreshed surfaces after a completed sync; that path is preserved.
- Tile resource version is now 5.
- Tile freshness fallback is 60 minutes. Course transition timing still uses Tile Timeline validity intervals, so the fallback is not the primary class-boundary update mechanism.

## Timetable UI

- Current Wear course card adds a lightweight progress bar based on class duration and the existing minute ticker.
- No pulse animation or additional graphics layer was introduced for the progress bar.
- When a semester end date is available, Wear header shows `Week N/Total` / `第N/总周数周`.
- Mobile timetable cards now contain a dedicated Today timetable section with course color, current-course emphasis, time/location, and click-through to the existing course actions.
- Existing full course management is kept below the Today section.

## Compatibility / non-goals

- Room schema unchanged.
- Sync protocol unchanged.
- `play-services-wearable` remains 20.0.1 for formal mobile/wear modules.
- Existing Legacy GoogleApiClient / NodeApi / DataApi / MessageApi compatibility paths are preserved.
- Signing configuration blocks were not changed. Only version fields in build files changed.

## Static checks

- XML parse: 0 errors.
- Modified Kotlin files: balanced braces and parentheses in static scan.
- English/Chinese new Wear string added in both locales.
- Formal Wearable dependency remains 20.0.1.
- Keystore SHA-256 remains `cd6f6307601cee8dc3c77491ae21bddd37e93cac2e111bf7d3ca0d371c26e233`.

## Local Gradle limitation

Attempted:

`./gradlew :wear:compileReleaseKotlin :mobile:compileReleaseKotlin --no-daemon --stacktrace`

The local environment failed before Gradle/Kotlin compilation because `services.gradle.org` could not be resolved (`UnknownHostException`). GitHub Actions remains the authoritative compile/build check.
