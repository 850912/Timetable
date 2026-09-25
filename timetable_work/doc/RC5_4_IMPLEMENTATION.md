# Timetable RC5.4 Implementation

## Actual changes

### 1. Google Wear transport migration

Added `mobile/src/main/java/com/hufeng943/timetable/sync/WearOsTransport.kt`.

The mobile sync path now uses:

```text
MainActivity
  -> SyncManager
      -> WearOsTransport
          -> Google Wear Data Layer
```

`PhoneWearSyncManager` remains only as a deprecated compatibility facade and is no longer used by `MainActivity`.

### 2. Wear receive migration

Added `wear/src/main/java/com/hufeng943/timetable/sync/WearOsSyncReceiverService.kt`.

The Wear manifest now registers this service for `/timetable/file-transfer/v1`. It keeps the existing shared transfer protocol and `ImportService` atomic import behavior, so the migration changes the architecture without changing the current backup payload format.

### 3. Version alignment

The mobile module version name is aligned to `2.0.0`, matching the current release line.

## Validation

The source was statically checked for the old mobile sync entrypoint and remaining risky casts. The container could not execute Gradle because Gradle 9.4.1 was not cached and the environment could not resolve `services.gradle.org`; therefore no local compile result is claimed.

## Not included in RC5.4

Samsung Accessory, BLE, SyncRecord repository hooks, conflict resolution and soft-delete synchronization remain later stages.
