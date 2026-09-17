# Timetable 3.1.0 Fix6 – Wear UX / Tile static review

## Scope

This patch is based on 3.1.0 Fix5 and only changes Wear OS presentation / launch behavior. The hybrid China Data Layer, database, sync protocol, mobile module and signing material are intentionally unchanged.

## Changes

- Removed the duplicated current-course summary capsule from the home page.
- When a class is in progress, the list automatically positions the highlighted real course card on screen.
- Course title/location/teacher can use two lines; current-state copy is shortened to avoid ellipsis on round screens.
- Date-aware home headers: Today / Tomorrow / M/D classes (今日课程 / 明日课程 / M月D日课程).
- Date-aware empty states for tomorrow and arbitrary dates.
- Wear Material 3 `TimeText` keeps system behavior but uses a transparent background.
- MainActivity uses a single task with the normal app affinity to prevent Tile launches from stacking duplicate Activity tasks.
- Tile visual hierarchy refreshed: status label, primary course capsule, optional secondary course capsule, responsive insets.
- Tile now distinguishes “no classes today” from “today's classes are finished”.
- Tile preview updated to 400x400 to reflect the new hierarchy.
- Tile resource version bumped to 3.

## Static checks

- Parsed all project XML resources successfully.
- Verified all Wear `R.string.*` references resolve.
- Verified no duplicate string names in default / zh-rCN string resources.
- Checked bracket balance in all modified Kotlin files.
- Compared `mobile/build.gradle.kts`, `wear/build.gradle.kts`, and `signing/timetable-release.jks` byte-for-byte with the Fix5 baseline: unchanged.
- Local Gradle compilation was attempted but Gradle 9.4.1 could not be downloaded because `services.gradle.org` is not resolvable in the current environment. GitHub Actions remains the build verification step.

## External API checks

- Wear Compose Material 3 1.6.2 `TimeText` supports `backgroundColor`, so transparent TimeText does not require a custom clock implementation.
- Wear OS quality guidance requires text and controls not to overlap or be clipped on 192dp+ round screens.
- Tiles are non-scrollable; the refreshed Tile deliberately limits the visible hierarchy to status + one primary course + one secondary course.
