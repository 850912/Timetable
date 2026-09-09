# Build 10.1 · China Wear Listener Fix

Real-device symptoms addressed:
- Phone formal probe: ApiException 17 / Wearable.API unavailable.
- Watch reports send success but phone receives no probe event.
- Phone incremental sync reports no available transport.

Changes:
1. Restore a dedicated legacy `com.google.android.gms.wearable.BIND_LISTENER` intent-filter on all production/probe WearableListenerService implementations.
2. Remove the incorrect service `android:permission="com.google.android.gms.wearable.BIND_LISTENER"` declaration.
3. Keep modern path-filtered MESSAGE_RECEIVED/DATA_CHANGED filters as a secondary route.
4. Convert the built-in formal probe send/diagnostic path to the same LegacyWearIo / GoogleApiClient + Wearable.*Api path used by production transport.
5. Convert probe ACK replies to LegacyWearIo.

The BIND_LISTENER action is intentionally kept in its own intent-filter with no `<data>` element, so the bind intent is not rejected by URI matching.
