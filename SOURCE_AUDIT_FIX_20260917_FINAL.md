# 2026-09-17 source audit follow-up

This pass is based on `Timetable-3.4.0-background-second-audit-fix(2).zip`.

## Changes applied

1. Background brightness now has one meaning end-to-end.
   - New installs default to direct 100% source luminance.
   - Existing persisted values from the old black-scrim implementation are interpreted with the legacy equivalent-luminance formula only until the user writes the setting again.
   - New writes set `background_brightness_direct=true` and store the actual 0.10..1.00 source luminance.
   - `AppBackground` no longer performs a second compatibility transform. The percentage shown in Settings therefore matches the value used by image and theme rendering.

2. The unobstructed background architecture is preserved.
   - No full-screen black/readability scrim was reintroduced.
   - Image and ambient theme backgrounds remain behind the single root Kyant LayerBackdrop.
   - Liquid Glass remains local to glass surfaces.

3. Course-card glass overdraw was reduced without disabling the optical effect.
   - In Liquid Glass mode, the extra Compose border pass is removed because the Kyant material already renders an optical edge/highlight.
   - The additional 1dp top-highlight draw is skipped in Liquid Glass mode.
   - The course-colour side accent remains because it conveys course identity rather than simulating the glass material.
   - Non-glass fallback retains its cheap border/highlight so it does not become visually flat.

## Deliberately not changed in this pass

- Nested `SwipeDismissableNavHost` instances were not mechanically flattened. The official Wear navigation architecture favors one app-level host, but these nested editors currently own local state and routes. Flattening them safely requires route/state hoisting rather than a regex-style replacement; doing that without compiler/device verification would risk data-loss and back-stack regressions.
- A generated Baseline Profile / Macrobenchmark module was not fabricated. The project already ships `profileinstaller`, but an app-specific profile should be generated from real Wear critical user journeys on a benchmark-capable device/emulator and then checked into the app.
- RGB_565 is retained for the 512px custom background decode to keep memory/bandwidth low on Wear. If banding is visible under refraction on-device, ARGB_8888 should be tested as a quality/performance tradeoff.

## Verification

- Static scan: no `scrimAlpha` or old full-screen black-scrim expression remains in Wear source.
- Kotlin brace counts remain balanced in all three modified Kotlin files.
- Local Gradle compile could not start because the sandbox has no cached Gradle 9.4.1 distribution and cannot resolve `services.gradle.org`; GitHub Actions remains the compiler oracle for this archive.
