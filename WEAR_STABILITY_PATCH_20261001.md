# Wear Stability Patch — 2026-10-01

## Goal

Improve cold-start and runtime stability without changing the verified China-device transfer wire protocol.

## Applied changes

1. **Legacy Wear Data Layer blocking isolation**
   - Kept `GoogleApiClient`, `Wearable.NodeApi`, `Wearable.DataApi`, and `Wearable.MessageApi` on the compatibility path.
   - `LegacyWearIo` is now suspend-based and forces blocking connect/await work onto `Dispatchers.IO`.
   - Kept the existing 3 connection attempts, 8-second connect timeout, 10-second operation timeout, and 2-second retry cadence.
   - Replaced `Thread.sleep()` with cancellable `delay()`.
   - Cancellation is rethrown instead of being swallowed by `catch (Throwable)`.
   - `DataApi.GetFdForAssetResult` is explicitly released after asset bytes are copied.

2. **Wear bootstrap lifecycle**
   - Replaced the unmanaged raw `Thread` in `WearBridgeBootstrap` with an application-owned `SupervisorJob + Dispatchers.IO` scope.
   - Prevents duplicate concurrent bootstrap jobs.
   - Preserves the watch-first `WATCH_HELLO` handshake required by prior target-device testing.

3. **DataStore recovery**
   - Added `ReplaceFileCorruptionHandler { emptyPreferences() }` for app settings and theme DataStores.
   - Added `IOException` read fallback to default preferences.
   - This prevents a damaged/transiently unreadable settings file from killing the long-lived configuration flow used by the Wear UI.

4. **Reminder scheduling**
   - Settings changes now call `WearCourseReminderScheduler.rescheduleAsync()` instead of rebuilding alarms inline from the ViewModel coroutine.
   - Reminder rebuild cancellation is preserved instead of swallowed.

5. **All-day Quick View**
   - Holiday greetings now also appear in `QuickViewPager`, including days with no classes.

6. **Sync cancellation correctness**
   - `WearOsTransport` rethrows coroutine cancellation and cancels its ACK tracker instead of converting cancellation into a normal failed-sync result.
   - `AutoSyncJobService` no longer catches every `Throwable`; cancellation remains handled separately and ordinary failures use `Exception`.

7. **Source hygiene**
   - Removed the stale non-Gradle `timetable_work/` mirror directory so future fixes cannot accidentally target an obsolete source copy.

## China-device compatibility guard

The following production China BLE protocol/transport implementation files were intentionally not modified:

- `mobile/.../ChinaWearBleCapabilities.kt`
- `mobile/.../ChinaWearBleClient.kt`
- `mobile/.../ChinaWearBleReceiverService.kt`
- `wear/.../ChinaWearBleClient.kt`
- `wear/.../ChinaWearBleService.kt`
- `shared/.../ChinaWearBleProtocol.kt`
- `shared/.../ChinaWearProtocol.kt`

The shared Google Data Layer routing/path constants in `WearBridgeProtocol.kt` and `WearFileTransferProtocol.kt` are also unchanged.

A before/after SHA-256 comparison of the China BLE files is identical. A standalone smoke test of `ChinaWearBleProtocol` v1/v2 encode/decode and CRC corruption rejection passes.

## Validation performed

- Parsed all 31 project XML files successfully.
- Kotlin parser/front-end pass on all modified Kotlin files found no syntax errors or invalid suspend-call placement after fixes (Android/project dependencies are not available to this standalone compiler invocation, so unresolved Android symbols are expected).
- Production source scan: no remaining `Thread.sleep`, raw `Thread { ... }`, `runBlocking`, `GlobalScope`, `allowMainThreadQueries`, Kotlin `!!`, or `catch(Throwable)` in `mobile/src/main`, `wear/src/main`, or `shared/src/main`.
- China BLE protocol smoke test: PASS.

## Build limitation in this environment

A full Gradle Android build could not be executed because the Gradle 9.4.1 distribution and Android dependency cache are not available locally, and the environment cannot fetch the wrapper distribution. Final release verification should run at minimum:

```bash
./gradlew :shared:test :mobile:compileDebugKotlin :wear:compileDebugKotlin
./gradlew :mobile:assembleDebug :wear:assembleDebug
```

Then verify on at least one Google Wear OS pair and the previously validated China phone/watch pair.
