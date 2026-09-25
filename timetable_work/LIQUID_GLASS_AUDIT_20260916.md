# Liquid Glass / Glass Card audit — 2026-09-16

## Implementation
- Course cards now use a translucent glass surface by default: reduced opaque fill, brighter glass rim, retained course-color tint and restrained ambient highlight.
- Added Settings > 液态玻璃. Default OFF because Wear OS battery/GPU cost matters.
- When enabled, timetable cards use Kyant0/AndroidLiquidGlass Backdrop 2.0.1 (Apache-2.0, Maven Central): vibrancy + 5dp backdrop blur + lens refraction + chromatic aberration + ambient highlight + inner/drop shadow.
- Liquid mode records a static multi-tone backdrop. It intentionally does not animate the backdrop, avoiding continuous GPU re-recording while idle.
- Existing current/next course emphasis, progress, course tint, click navigation and TransformingLazyColumn transformations remain intact.

## Compatibility / performance policy
- The dependency is pinned to 2.0.1 rather than a moving tag.
- Liquid Glass is opt-in. Standard glass remains the low-cost fallback.
- No infinite animation was added.
- No per-frame coroutine or timer was added.

## Verification
- Preference flow -> AppConfig -> Settings toggle -> TimetablePager -> CourseCard wiring checked.
- Search confirmed a single liquid-glass setting and a single Backdrop dependency.
- Gradle compile was attempted but the execution environment cannot resolve services.gradle.org, so Gradle 9.4.1 could not be downloaded. This is an environment/network failure, not a compiler result.
