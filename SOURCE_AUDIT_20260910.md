# Timetable source audit — 2026-09-10

## Corrected in this audit

1. **Samsung Multi-Info Tile claim corrected**
   - The added Current/Next services are Wear OS watch-face complication data sources, not Samsung One UI 8 Multi-Info Tile widgets.
   - Public Wear OS APIs do not provide a third-party API that makes an app widget join Samsung's first-party Multi-Info Tile stack. Keep the standard Wear OS Tile and complications as separate surfaces.
2. **Wear Tile clipping / card reliability**
   - Wear Tiles are non-scrollable. The tile now renders at most two course cards with a compact time/location subtitle instead of trying to fit three verbose cards.
   - Tile label changed from the leftover `Example tile` to `Today’s classes`; complication label changed from `Example complication` to `Timetable`.
3. **Tile/complication stale data after sync**
   - Added `WearSurfaceRefresher` and request Tile + complication refresh after a successful import/sync.
4. **Partial incremental-sync acknowledgement bug**
   - `processSyncBatch()` previously ended in `runCatching { ... complete }.isSuccess`, which returned true even when `complete == false`; this could delete a partially-applied DataItem. It now returns the actual Boolean via `getOrDefault(false)`.
5. **Wear Tiles dependency**
   - Updated Tiles/tooling 1.6.1 -> 1.6.2 to match the current API-37 Tile surface APIs.
6. **About/developer wording**
   - Removed wording that implied complications were Samsung Multi-Info capsules; the UI now identifies the feature as a standard Wear OS Tile compatible with Galaxy Watch.

## Important findings not silently changed

### High: release signing secrets are committed in source
`mobile/build.gradle.kts`, `wear/build.gradle.kts`, `legacyprobe-mobile/build.gradle.kts`, and `legacyprobe-wear/build.gradle.kts` contain the release keystore password, and the repository contains the release keystore. Rotate this signing material if the repository has ever been shared/public, then inject passwords through GitHub Actions secrets or local Gradle properties. Changing this automatically would break the current release workflow without replacement secrets.

### Medium: legacy Wear Data Layer code remains
The project contains both current and deprecated/legacy GoogleApiClient Wear paths. This is understandable for China-device compatibility, but it increases maintenance and duplicate-path risk. Keep the legacy probe modules isolated from production behavior where possible.

### Medium: static About version text
The About declaration says `2.0.0` in resources while the screen already reads the actual package version dynamically. Prefer removing the static version sentence in a future cleanup to avoid drift.

## Validation performed
- Parsed all XML resources/manifests with Python XML parser: no malformed XML found.
- Searched production Kotlin for `!!`, `first()`, TODO/FIXME and obvious nullable hazards.
- Reviewed Wear Tile, complication services, sync receiver, export flow, manifests, Gradle dependencies, release workflow, and About resources.
- Full Gradle compilation cannot be guaranteed in this container if the Gradle distribution/dependencies are not locally cached; GitHub Actions remains the authoritative compile check.
