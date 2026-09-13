# Timetable 3.1.0 Fix7 — Wear performance / Tile freshness / visual polish

Base: Timetable-3.1.0-Fix6-WearUX-TileRefresh

## Changes

### Cold-start responsiveness
- Deferred the China Wear Data Layer bootstrap by 2.5 seconds in `TimetableApp` so Google Play services connection work no longer competes with the first Compose/Room frame.
- Preserved the existing hybrid Data Layer implementation; no dependency/API downgrade was introduced.
- Removed the per-card `graphicsLayer` from the ambient highlight effect to avoid unnecessary render layers on the timetable list.

### Samsung Tile freshness
- Tile freshness interval reduced from 6 hours to 15 minutes.
- Tile resource version bumped from 3 to 4.
- Added a cheap Tile-only refresh request after activity resume (delayed 1.5 s).
- Added full Wear surface refresh when the activity leaves the foreground so local timetable edits appear on the Tile/complications without waiting for periodic refresh.
- Existing exact timeline validity windows remain intact for current/next/finished course transitions.

### Destructive actions
- Destructive capsules now use a vivid red accent (`#FF453A`), a visible red outline, and stronger red title/subtitle/icon treatment.

### Timetable visual polish
- Course cards use a calmer tinted surface with a subtle hairline border.
- Current/next course hierarchy is stronger without adding animation.
- Added a thin highlight sheen and refined ambient tint.
- Course number badge now follows each course color and gets a subtle outline.
- Current-course title and remaining-time emphasis increased.
- Finished-day card receives the same refined visual language.

## Scope / safety
Only Wear UI/startup/surface-refresh source files were changed. Mobile sync, database schema, transport protocol, Gradle signing configuration, and keystore were not changed.

## Static review
- XML parse errors: 0
- Delimiter balance in all changed Kotlin files: 0 imbalance
- `mobile/build.gradle.kts` unchanged
- `wear/build.gradle.kts` unchanged
- Keystore SHA-256 unchanged: `cd6f6307601cee8dc3c77491ae21bddd37e93cac2e111bf7d3ca0d371c26e233`

## Local build note
`./gradlew :wear:compileReleaseKotlin --no-daemon --offline --stacktrace` could not enter Gradle because this environment cannot resolve `services.gradle.org` while the wrapper distribution is absent. GitHub Actions remains the compile verifier.
