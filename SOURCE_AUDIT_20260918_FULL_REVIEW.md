# Timetable 3.4.0 — full-source review (2026-09-18)

Scope: all 378 repository files / 207 Kotlin sources across `mobile`, `wear`, `shared`, manifests, Gradle, CI, Room schema/migrations, import/export, sync, Tile/Complication, settings and UI. Historical `*_AUDIT.md` files were not treated as proof; current source was inspected directly.

## Fixes applied in this pass

1. **Removed all nested Wear swipe-dismiss navigation hosts.** `EditTimetableScreen`, `EditTimeSlotScreen`, `ScheduleToolsScreen`, `CourseAdjustmentScreen`, and `DayArrangementScreen` were still creating a `SwipeDismissableNavHost` inside the app-level `SwipeDismissableNavHost`. They now use ordinary Compose `NavHost` internally. Only `AppNavHost` owns swipe-dismiss. This closes the same class of old-page-left-edge/ghost-frame transition bug outside Settings too.
2. **Made power-saver degradation reactive.** `AppNavHost` previously read `PowerManager.isPowerSaveMode` only during composition triggered by unrelated state. A runtime power-save toggle could therefore leave expensive blur/chromatic animation active until another recomposition. It now listens for `ACTION_POWER_SAVE_MODE_CHANGED`, updates Compose state, and unregisters the receiver on dispose.
3. **Removed now-unused nested-navigation imports/state.** No UI styling/content was intentionally changed by these navigation fixes.

## Findings / status

### Navigation and Wear UI
- Settings and Liquid Glass internal navigation already use plain `NavHost`; this is correct.
- Before this pass five editing/tool flows still nested swipe-dismiss hosts. Fixed.
- App-level swipe-dismiss remains intact, preserving native Wear back gesture behavior.
- Rotary/snap scrolling is broadly wired through Wear lists. Haptic helper uses platform haptic constants rather than a custom vibrator waveform.
- Long labels are primarily handled by capsule layouts rather than screen-level `Text(maxLines=1)` truncation; no direct screen-level single-line ellipsis pattern was found in the audit scan.

### Power/performance
- Power saver disables nonessential UI animation, chromatic aberration and glass blur and selects the soft liquid-glass effect. Runtime switching is now reactive.
- Backdrop capture is shared at app scope rather than recreated per card, which is the right architecture for the glass renderer.
- Remaining performance debt: `mobile/MainActivity.kt` is ~88 KB and combines UI, permissions, import/export, sync, calendar and repository operations. It should be split in a future refactor, but doing so in this bug-fix release would carry disproportionate regression risk.
- `LegacyWearIo` uses short `Thread.sleep` retry delays. Calls are currently on IO-oriented paths, so this is not a UI-thread freeze found by this review, but coroutine `delay` would be cleaner in a later protocol refactor.

### Settings / glass
- The visible liquid-glass switch is authoritative when written: `updateLiquidGlassEnabled` clears legacy frosted/global flags.
- `FROSTED_GLASS_ENABLED` and `GLOBAL_GLASS_MATERIAL_ENABLED` remain as compatibility fields/read paths. They are not separate visible switches. Keeping the read path avoids silently changing existing users' stored appearance; removing the compatibility fields should be done only with an explicit preference migration.
- Glass opacity and clarity are separate controls, preserving the requested low-opacity liquid appearance while allowing clarity to be tuned independently.

### Data / sync / import-export
- Shared module contains real unit/instrumentation coverage for schedule resolution, batch operations, export round trips, importer edge cases, sync manager/applier and Room migrations.
- Data Layer listener services are exported because Wearable binds them; file-provider remains non-exported with URI grants.
- No production `TODO`, `FIXME`, `NotImplementedError`, or `GlobalScope` blocker was found in the scan.

### Build/release
- Wear release enables R8/resource shrinking; mobile release currently leaves minification disabled. This is not a correctness bug, but is an APK/performance inconsistency. It was not changed in this pass because enabling R8 on the phone without a successful release build/instrumentation pass can introduce serialization/reflection regressions.
- Mobile still contains Android Studio placeholder `ExampleUnitTest` / `ExampleInstrumentedTest`; these provide no useful regression coverage and should eventually be replaced by phone UI/sync/reminder tests.
- Wear has no dedicated UI/navigation regression tests. Recommended future tests: Settings -> UI management -> glass navigation, edit timetable/time-slot child navigation, rotary selectors, power-save configuration transition.

## Verification limitation

Attempted:

`./gradlew :shared:test :wear:compileDebugKotlin :mobile:compileDebugKotlin`

The wrapper needs Gradle 9.4.1 and this execution environment cannot resolve `services.gradle.org` (`UnknownHostException`). Therefore this review does **not** claim a successful Gradle compilation. The source was statically re-audited after the changes. GitHub Actions should run the compile/test matrix before release.
