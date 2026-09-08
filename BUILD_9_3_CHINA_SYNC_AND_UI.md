# Build 9.3 – China Wear OS sync + UI pass

## Wear OS China communication

- `play-services-wearable` is pinned to **10.2.0** for the main phone and Wear modules, matching the current Android Developers China guidance.
- `mobile/WearOsTransport` now uses `GoogleApiClient + Wearable.API + NodeApi + DataApi` for the actual phone → Wear incremental sync path.
- Connection attempts retry up to three times with a 2-second backoff.
- Connected nodes are selected with nearby nodes first.
- Data items remain `urgent` and keep the existing asset/envelope protocol.
- Release workflow verifies that phone and Wear APKs have the same signing certificate and package name.
- Existing `signing/timetable-release.jks` is preserved; no new signing key is introduced.

## Mobile

- Material You dynamic color is enabled through `DynamicColors` when supported.
- Added a **今日课程** section.
- Course/timetable colors are now visible as accent strips and tinted cards.
- Added **AI 导入课表** assistant: it copies a strict prompt and accepts pasted AI output. This intentionally does not embed an API key/provider in the APK.
- Existing JSON/ICS/CSV import remains unchanged.

## Wear

- Current course is highlighted with a theme-colored border.
- Current course shows `距下课还有 xx 分钟`, refreshed every 30 seconds.
- Next course gets a subtle pulse animation and `下一节` marker.
- Empty state now has a calendar icon.

## Build note

The source was statically checked in this environment, but Gradle could not be executed because the container has no network access and the Gradle 9.4.1 distribution is not cached. GitHub Actions remains the authoritative build environment.
