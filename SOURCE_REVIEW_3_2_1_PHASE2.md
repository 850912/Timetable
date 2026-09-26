# Timetable 3.2.1 Phase 2 review

## Scope
Built from the user-confirmed 3.2.0 Fix1 source. This phase keeps the China Hybrid Data Layer architecture and signing material unchanged while extending the 3.2 plan.

## Added
- On-demand premium weekly timetable renderer on mobile (`WeeklyScheduleView`).
  - Seven-day grid, time axis, course-color blocks, room labels.
  - Constructed only when the user opens “本周课表”, avoiding cold-start/render overhead.
- Sync status center on mobile.
  - Connected state, pending queue count, permanently failed count.
  - One-tap full sync and direct communication diagnostics.
- Wear in-class context enhancement.
  - Current course card keeps the progress bar and adds the next course + minutes-until-next inside the same card, avoiding a duplicated top status card.
- Semantic version 3.2.1 (mobile versionCode 4).

## Wear surface strategy
- Existing event-driven Tile/Complication refresh remains unchanged.
- Existing Tile timeline remains unchanged for predictable class boundaries.
- `play-services-wearable` remains 20.0.1; China legacy fallback remains selective.

## Static review
- XML parse errors: 0.
- Changed Kotlin source delimiter counts are balanced.
- All new Wear string references exist in default and zh-rCN resources.
- Keystore SHA-256 unchanged:
  `cd6f6307601cee8dc3c77491ae21bddd37e93cac2e111bf7d3ca0d371c26e233`
- Gradle local compilation could not start because `services.gradle.org` is not resolvable in the local environment. GitHub Actions remains authoritative.
