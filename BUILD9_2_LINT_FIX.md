# Build 9.2 - Legacy Wear listener lint fix

GitHub Actions reached Kotlin compilation successfully for both legacy probe modules, then failed in `:legacyprobe-mobile:lintVitalRelease` because Android Lint treats the deprecated `com.google.android.gms.wearable.BIND_LISTENER` action as a fatal `WearableBindListener` issue for release builds.

For this dedicated China legacy compatibility probe the legacy listener action is intentional: the experiment specifically targets the old Google Play services Wearable 10.2.0 / GoogleApiClient path. Removing the action would invalidate the inbound Message/Data event test.

Build 9.2 therefore keeps the listener and applies a narrow manifest-only lint suppression:

```xml
<action
    android:name="com.google.android.gms.wearable.BIND_LISTENER"
    tools:ignore="WearableBindListener" />
```

The same suppression is applied to both `legacyprobe-mobile` and `legacyprobe-wear`, because the wear module would otherwise be expected to hit the same release-lint check after the mobile module passes.

No normal timetable synchronization code is changed.
