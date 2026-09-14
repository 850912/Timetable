# Timetable 3.2.0 Fix1 – Mobile compile fix

GitHub Actions failed in `:mobile:compileReleaseKotlin` because `com.google.android.material.R.attr.colorPrimary` is not available in the resolved Material resource API used by this build.

Fix:
- `MainActivity.resolvePrimaryColor()` now resolves the platform `android.R.attr.colorAccent`, which is guaranteed to exist at compile time and is supplied by the Material 3 activity theme.
- Keeps the existing hard-coded fallback `0xFF6750A4`.
- No signing, Data Layer, Room schema, sync protocol, Wear UI, or build configuration changes.

Static review:
- Only one Kotlin source line changed.
- No remaining `com.google.android.material.R.attr.colorPrimary` references in production source.
