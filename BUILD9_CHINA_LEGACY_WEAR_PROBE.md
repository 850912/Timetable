# Build 9 - China Legacy Wear Probe

## Goal
Build 8 established that the China-market Galaxy S25+ can see Google Play services but the modern Wearable API path (`play-services-wearable:20.0.1`, `Wearable.getNodeClient`) returns `API_UNAVAILABLE`, while the Galaxy Watch7 can still see the phone as a connected node.

Build 9 therefore tests the China-specific legacy path documented by Android Developers:

- `com.google.android.gms:play-services-wearable:10.2.0`
- `GoogleApiClient`
- `Wearable.NodeApi`
- `Wearable.MessageApi`
- `Wearable.DataApi`

## Important isolation decision
The existing `mobile` and `wear` apps are left unchanged on `play-services-wearable:20.0.1`.

Build 9 adds two standalone probe modules instead:

- `legacyprobe-mobile`
- `legacyprobe-wear`

Both use the same application id `com.hufeng943.timetable.legacyprobe` and the same release signing certificate. This lets us test the legacy China Data Layer without refactoring or risking the normal timetable synchronization code.

## Build commands

```bash
./gradlew :legacyprobe-mobile:assembleRelease :legacyprobe-wear:assembleRelease
```

Expected outputs:

- `legacyprobe-mobile/build/outputs/apk/release/*.apk`
- `legacyprobe-wear/build/outputs/apk/release/*.apk`

## Test order
1. Install the legacy probe release APK on the phone and watch.
2. Confirm Galaxy Wearable still reports the watch connected.
3. Phone: `运行 Legacy 完整诊断`.
4. Watch: `运行 Legacy 完整诊断`.
5. Phone: Message test. Watch: refresh last event.
6. Watch: Message test. Phone: refresh last event.
7. Phone: DataItem test. Watch: refresh last event.
8. Watch: DataItem test. Phone: refresh last event.

## What counts as success
The strongest positive result is:

- `GoogleApiClient: CONNECTED`
- `hasConnectedApi(Wearable.API): true`
- `NodeApi.connectedNodes ... count=1`
- Message send returns `success=true`
- the opposite device records the Message and returns an ACK
- DataItem send returns `success=true`
- the opposite device records the DataItem

If the phone still returns `CONNECT FAIL ... API_UNAVAILABLE`, the 10.2.0 legacy path is also unavailable on this specific China ROM and we should stop investing in GMS Data Layer for this device combination.
