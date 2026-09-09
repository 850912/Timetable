# Build 10 · China Wear Auto Bootstrap

## Goal
Turn the real-device observation into protocol behavior: on the target Samsung phone/watch pair, the watch must initiate Wear Data Layer traffic before the phone-side legacy GoogleApiClient reliably leaves API_UNAVAILABLE(16).

## Changes
- Added shared WearBridgeProtocol HELLO/READY paths and bridge state.
- Wear TimetableApp automatically starts WearBridgeBootstrap on process startup.
- Watch sends WATCH_HELLO to connected phone nodes using Legacy GoogleApiClient + Wearable.MessageApi.
- Phone PhoneWearDataLayerService now receives HELLO, stores READY state, and sends PHONE_READY back.
- Production WearOsTransport requires a recent Watch bootstrap before phone-initiated sync.
- Production sync receiver asset read/delete/ACK operations moved to Legacy Wearable.DataApi/NodeApi.
- Phone file picker return transfer moved to Legacy Wearable.DataApi.
- Watch export-to-phone and local-node resolution moved to Legacy APIs.
- Existing timetable serialization, SyncEnvelope, SyncAck, database application logic, and Data Layer paths remain unchanged.

## Dependency note
The project-level play-services-wearable dependency remains 20.0.1 to avoid destabilizing unrelated modern probe/dead compatibility code. The production transport paths listed above intentionally use the legacy API surface already proven to compile in this project and verified on the target Samsung device pair.

## Expected cold-start flow
1. Open Timetable on Watch.
2. Watch automatically sends HELLO.
3. Phone listener records bridge READY.
4. Phone-initiated sync/import may now use the verified legacy transport.

## Validation
Local Gradle compilation could not run in the artifact environment because Gradle Wrapper attempted to download Gradle 9.4.1 from services.gradle.org and outbound network resolution was unavailable. Both AndroidManifest.xml files were parsed successfully as XML. Use GitHub Actions for the authoritative build result.
