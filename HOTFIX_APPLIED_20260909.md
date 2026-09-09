# Hotfix applied on 2026-09-09

This package contains fixes for the GitHub build failure and the requested reliability/UI issues.

## Build fix
- Replaced `kotlin.test.*` imports in `WearFileTransferProtocolTest` with the already-configured JUnit 4 imports.

## Sync reliability
- Mobile now reuses one `WearOsTransport` instance for status checks, manual sync, and automatic sync.
- Manual "sync all" now sends a full JSON timetable snapshot instead of relying only on pending incremental records.
- Imported timetables are re-enqueued through the repository so automatic incremental sync has real records to retry.
- Automatic background sync uses `SyncCoordinator`, so failures update retry state and remain eligible for bounded retry.
- Phone-to-watch sending no longer depends on a recent Wear "hello" bootstrap message when Data Layer connected nodes are available.

## Wear UI / Tile
- Wear top `TimeText` now respects the app setting, preventing default overlap with the home course list.
- Course list header padding adapts when top time is enabled.
- Current course / next course state refreshes periodically and handles overnight slots.
- Course card status text uses localized resources, including “距离下课还有 xx 分钟”.
- Wear home cards and Samsung Tile now fall back to the timetable color when a course has no custom color.
- Samsung Tile now renders up to three compact course capsules instead of only one oversized card.
- Fixed malformed English `values/strings.xml` newline near `home_add_timetable` / `more_title`.

## Mobile / Widget
- Mobile status card keeps Material rounded corners and uses the existing Material You dynamic color setup.
- Android home-screen widget now displays up to five current/upcoming classes, includes time ranges and locations, uses rounded background, and opens the app when tapped.
