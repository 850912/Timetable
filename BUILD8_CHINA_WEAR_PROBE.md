# Build 8 - China Wear communication probe

## Purpose
This build adds a non-destructive diagnostic probe for a China-market Samsung Android phone paired with a China-market Samsung Wear OS watch. Existing timetable, Tile, transfer, and repository logic is left intact.

## Current dependency under test
Both phone and wear modules keep `com.google.android.gms:play-services-wearable:20.0.1` from Build 7. Build 8 intentionally does not downgrade the project. The probe is designed to establish what the existing dependency can actually do on the target China ROM.

## Phone entry
Home screen -> `中国区 Wear 通信探针`

The phone probe reports:
- Device / Android version
- `com.google.android.gms` package visibility and version, when present
- Whether `Wearable.getNodeClient()` can be created
- `localNode`
- `connectedNodes` including node id and nearby state
- Whether DataClient / MessageClient can be created
- Last inbound probe event

Actions:
- Phone -> watch MessageClient test
- Phone -> watch DataClient DataItem test
- Refresh last received probe event

## Watch entry
More -> About -> Developer options -> `中国区 Wear 通信探针`

Actions mirror the phone side:
- Full diagnostic
- Watch -> phone MessageClient test
- Watch -> phone DataClient DataItem test
- Refresh last received probe event

## Probe paths
- `/timetable/probe/v1/message`
- `/timetable/probe/v1/ack`
- `/timetable/probe/v1/data`

Dedicated listener services are used on both sides so the normal timetable transfer paths are not changed.

## How to test on the real devices
1. Build and install the same build type on both phone and watch. The application id and signing certificate must match between the paired apps for Data Layer communication.
2. Confirm Galaxy Wearable shows the watch as connected.
3. On the phone, open the probe and run `运行完整诊断`.
4. Record `GMS`, `localNode`, and `connectedNodes`.
5. Run phone -> watch Message. On the watch, open the probe and refresh the last received event.
6. Run phone -> watch DataItem and refresh on the watch.
7. Repeat Message and DataItem from watch -> phone.
8. Capture the full diagnostic text and any FAIL / exception name from both devices.

## Interpretation
- `connectedNodes > 0` + Message success + ACK received: current Data Layer message path works on this ROM.
- Message works but DataItem fails: use MessageClient for small immediate commands and investigate China Data Layer DataClient compatibility separately.
- NodeClient works but `connectedNodes=0`: pairing/app identity/signature/service compatibility needs investigation before replacing transport.
- NodeClient / localNode fails with API unavailable / missing service: current 20.0.1 path is not viable on the device; next experiment should be a dedicated China compatibility transport rather than changing the existing global transport in place.
- Both Data Layer paths unavailable: move to a separate non-GMS transport (for this project, BLE is the leading candidate).

## Build verification note
The source was statically checked in this environment. Gradle compilation could not start because the wrapper attempted to download Gradle 9.4.1 from `services.gradle.org`, while the execution sandbox had no DNS/network access to that host. This is an environment download failure, not a compiler result.
