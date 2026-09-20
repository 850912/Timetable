# Timetable 3.5.0 source review — 2026-09-19

## Implemented
- Version advanced to 3.5.0 / 3050000 for mobile, wear, and the new watch-face package; About/Developer-facing hard-coded release text updated.
- Liquid glass advanced controls now feed the renderer directly: blur uses the configured 0–8 dp range, lens strength is profile-scaled, chromatic aberration is no longer silently restricted to the Fluid profile, and Soft/Balanced/Fluid alter optical strength rather than disabling controls.
- Custom-image “fluid background” now renders animated fluid gradients sampled from the selected image's own tones instead of moving/rotating the original bitmap.
- Custom image backgrounds receive a baseline contrast scrim so white text and translucent cards remain readable.
- Navigation transition scrims are opaque/stronger to prevent the outgoing transparent destination from remaining visibly shifted at the left edge during child-page entry/dismiss transitions.
- Added `watchface-nacho`, a code-free Watch Face Format v1 package with a cute original gray/pink cat motif, digital time, preview asset, and a reduced-pixel AOD/ambient variant. No third-party Nachoneko artwork is redistributed.

## Review checks
- Searched mobile/shared/wear/watchface sources for unresolved merge-conflict markers: none found.
- Searched Kotlin sources for FIXME/TODO call-sites: none found by the audit command.
- Parsed new WFF manifest, metadata, and watchface XML as well-formed XML.
- Build was attempted for mobile, wear, and watchface. It could not start because the sandbox has no network access and Gradle 9.4.1 is not cached; wrapper download failed with `UnknownHostException: services.gradle.org`. This is an environment limitation, not a successful compile claim.

## Follow-up verification on a development machine
Run `./gradlew :mobile:assembleDebug :wear:assembleDebug :watchface-nacho:assembleDebug`, then validate the WFF file with Android Studio's Watch Face Format validation and test interactive/AOD modes on round Wear OS 4+ hardware/emulators.
