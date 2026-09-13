# Timetable 3.1.0 · China Data Layer compatibility fix

## Root cause
Production mobile/wear modules still resolved `com.google.android.gms:play-services-wearable:20.0.1` while only part of the transport had been migrated to the China-compatible legacy API surface. The Wear transfer service also retained `Wearable.getNodeClient()` / `Wearable.getDataClient()` calls. On the target China phone this combination reports `API_UNAVAILABLE(16)`.

## Fix
- Production `play-services-wearable` version is now `10.2.0` through the version catalog for both mobile and wear modules.
- Production transport uses `GoogleApiClient + Wearable.NodeApi + Wearable.MessageApi + Wearable.DataApi`.
- Removed remaining production `getNodeClient/getDataClient` calls from the Wear transfer service.
- Kept the existing HELLO/READY watch bootstrap as a cold-start compatibility fallback.
- Updated probe labels to describe the actual legacy China Data Layer path.
- Timetable payload/database/sync protocol are unchanged.

## Static review
- No `Wearable.get*Client`, `NodeClient`, `DataClient`, or `MessageClient` calls remain in mobile/wear production Kotlin sources.
- Both mobile and wear resolve the same wearable dependency from the shared version catalog.
- Signing configuration and release keystore are intentionally unchanged.
- Full Gradle build is left to GitHub Actions when local Gradle distribution download is unavailable.
