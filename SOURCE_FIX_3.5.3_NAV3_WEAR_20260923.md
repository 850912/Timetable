# 3.5.3 Wear Navigation / Performance Fix — 2026-09-23

## Scope
Audited the uploaded `Timetable-3.5.3-apk-zip-fix-v3` source as the source of truth. The reported screenshot shows the old page exposed on the left while entering a child page. The old Wear Navigation 2 `SwipeDismissableNavHost` path was the central navigation layer.

## Root cause
The previous Wear Navigation implementation used `SwipeDismissableNavHost`, whose predictive-back implementation owns a horizontal enter/exit transition. AndroidX Wear's predictive-back host uses horizontal slide transitions for forward navigation, while the previous destination remains composed during the transition. On a glass-heavy screen this can become a visible mid-transition frame when rendering stalls.

The app also rendered liquid-glass backdrop effects for inactive destinations. That allowed the previous page to keep expensive backdrop/shader work during navigation.

## Changes

### 1. Wear navigation migrated to Navigation 3
- Replaced the app-level Wear Navigation 2 `SwipeDismissableNavHost`.
- Added:
  - `androidx.navigation3:navigation3-runtime:1.2.0-rc01`
  - `androidx.navigation3:navigation3-ui:1.2.0-rc01`
  - `androidx.wear.compose:compose-navigation3:1.6.2`
- Added Kotlin serialization plugin to the Wear module.
- Added a serializable `TimetableRouteKey`.
- Replaced the navigation graph tree with one persistent `NavBackStack`.
- `SwipeDismissableSceneStrategy` remains responsible for Wear swipe/predictive-back behavior.
- Existing route constants are retained so screen code and existing deep-link-like route construction remain stable.

### 2. ViewModel scoping preserved without Navigation 2 graph entries
The former graph-scoped ViewModels now use stable Hilt keys:
- `edit-timetable:<tableId>`
- `edit-course:<tableId>:<courseId>`
- `edit-timeslot:<courseId>:<timeSlotId>`
- `schedule-tools`
- `day-arrangement`
- `course-adjustment`

This keeps the same editor state across its child pages without recreating Navigation 2 nested graphs.

### 3. Navigation compatibility layer
`LocalNavController` is now a small `TimetableNavigator` abstraction. Existing screen calls such as `navigateSingle()` and `popSafe()` remain valid, but UI code no longer depends on a concrete Navigation 2 controller.

Parent arguments for editor child pages are automatically carried forward when a child route is requested.

### 4. Wear rendering optimization
`globalLiquidGlass()` now checks `LocalScreenIsActive`. Inactive destinations fall back to a cheap translucent draw path instead of running Kyant backdrop shaders.

This specifically targets the reported transition stall: the previous page can remain composed for navigation, but it no longer competes with the active page for expensive GPU work.

The global backdrop is also no longer created when liquid-glass effects are disabled.

### 5. Dead-source cleanup
Removed source that had no references in the actual Kotlin/XML source:
- `OneUiWatchCard.kt` compatibility/card implementation
- `ScheduleTicker.kt`
- `ExportTargetSelector.kt`
- unused `ExportViewModel.executePhoneExport()`
- unused `ExportViewModel.executeDirectExport()`
- unused `LocalBackupManager.getPreferredBackupDir()`
- unused `FirstDayOfTheWeek.fromDayOfWeek()`
- unused Navigation Compose 2 catalog dependency
- unused Hilt Navigation Compose dependency

### 6. Static re-audit
Checked:
- Navigation 2 Wear host/controller references: none remain in production source.
- Navigation 3 dependencies and serialization plugin: present.
- AppNavHost brace/parenthesis balance: matched.
- All screen symbols referenced by the new AppNavHost resolve to current source declarations.
- Removed dead symbols have no remaining production-code references.
- Route construction sites were enumerated and the graph-entry routes were normalized for the new flat back stack.

## Verification limitation
No local Gradle/JVM build was run. The requested workflow remains GitHub Actions/device validation. The source-level audit passed; the final CI build and real-device navigation test are still required.

## Device test focus
After the next GitHub Actions build, specifically test:
1. More → Settings → Export
2. Settings → UI management → Theme/Background/Liquid Glass
3. Timetable → Edit → Name/Date/Color
4. Course → Edit → Name/Location/Teacher/Color
5. Time slot → Edit → Start/End/Dates
6. Forward navigation immediately followed by back gesture
7. Repeated enter/back on a glass-enabled configuration
8. Xiaomi/China-ROM Wear device with liquid glass enabled
9. Power-save mode navigation


## CI failure from the previous v4 artifact
The first Navigation 3 migration artifact (`v4`) did not compile in GitHub Actions. The failure was limited to `wear/presentation/ui/AppNavHost.kt`: the back stack is typed as `NavKey`, so direct `.route` access on `lastOrNull()`/`it` was invalid; the Wear scene strategy was instantiated as `SwipeDismissableSceneStrategy<TimetableRouteKey>` while `NavDisplay` was expecting `SceneStrategy<NavKey>`; and the compatibility extension `navigateSingle()` was used without importing the extension. GitHub Actions reported those errors at lines 201-208 of the uploaded failure log.

## v5 corrections
- Instantiate `rememberSwipeDismissableSceneStrategy<NavKey>()`, matching the current Wear Navigation 3 migration guidance.
- Cast `NavKey` values to `TimetableRouteKey` before reading `route`.
- Import `navigateSingle` for the existing navigation compatibility layer.
- Add a Navigation 3 `TransitionKey` entry metadata override that uses a short cross-fade for forward navigation. This removes the horizontal forward slide while preserving Wear's pop/predictive-back handling.

## Verification
The source was statically re-checked after these edits. No local Gradle build was run; GitHub Actions remains the authoritative compile test for this project.
