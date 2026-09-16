# Timetable 3.4.0 — Full source re-audit (2026-09-16)

## Scope
- 202 Kotlin files across `wear`, `mobile`, `shared`.
- All XML resources parsed successfully.
- Rechecked Liquid Glass settings/persistence/render path, global background, timetable list navigation, batch schedule operations, animation loops, Room v11 migration/adjustment history, sync paths, and production blocking calls.

## Changes in this pass
- Liquid Glass advanced controls now persist independently: high-saturation/vibrancy, chromatic aberration, lens distortion (0–100%), glass blur enable/radius (0–8dp), blurred global background enable/radius (0–12dp), glass opacity (5–95%), background brightness (10–100%).
- Percentage/radius controls use rotary-friendly value pickers rather than coarse cycling presets.
- Course-card AGSL stack only runs on the already-limited current/next cards; each expensive pass can be disabled. Chromatic aberration is no longer implicitly forced by a preset.
- One shared root `LayerBackdrop` now captures the app background. TimetablePager reuses it instead of decoding/drawing/capturing a second full-screen background.
- Optional background blur is a single root background blur layer, not per-card blur.
- Restored `批量日程工具` at the bottom of the timetable-list page. Global invocation exposes timetable switching; the edit-timetable quick-modify entry remains fixed to its timetable and does not show a timetable selector.
- Existing quick-modify ±30-minute business validation remains intact.

## Performance / battery review
- No new infinite background animation or timer.
- Real refraction remains limited to current/next course cards.
- Global background blur is opt-in and one layer.
- High saturation, chromatic aberration, lens distortion and glass blur can each be disabled.
- Shared backdrop removes duplicate timetable-page background decode/draw/capture work.
- Existing TransformingLazyColumn + rotary snap behavior remains in the modified settings/list paths.

## Existing technical debt (not introduced here)
- Production `runBlocking` remains in Wear/phone Data Layer services, PhoneWearSyncManager and CourseReminderReceiver. This requires lifecycle-aware service/receiver redesign rather than mechanical replacement.
- Two `while(true)` loops in TimetablePager were rechecked; they suspend on timing/input work and are not CPU busy loops.

## Build verification
Attempted:
`./gradlew :wear:compileDebugKotlin :shared:test :mobile:testDebugUnitTest --no-daemon`

The wrapper could not resolve `services.gradle.org` in this execution environment (`UnknownHostException`) before Gradle/Kotlin compilation began. Therefore this audit does not claim a successful local compile. XML parsing and source-level consistency checks passed.
