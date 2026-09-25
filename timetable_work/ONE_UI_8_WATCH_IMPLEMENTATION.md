# Timetable 2.0.0 — One UI 8 Watch implementation pass

Target device: Samsung Galaxy Watch7

## Implemented in this pass

- Replaced the placeholder Wear Tile (`Hello World!`) with real timetable data.
- Tile reads the active timetable for today, filters odd/even week recurrence, sorts by start time and shows up to two courses.
- Tile uses rounded capsule containers and opens Timetable when tapped.
- Tile refresh interval is 15 minutes.
- Reworked the home course card into a compact One UI-style capsule.
- Course color is now an accent instead of a full saturated background.
- Added a lightweight Galaxy AI-inspired blue/purple/cyan ambient gradient layer without runtime blur/shaders.
- Added reusable `OneUiCapsuleButton` and applied it to the More page and core Settings entries.
- Renamed theme presets to a more unified One UI naming scheme.

## Performance choices

The Galaxy AI light treatment is intentionally static and local. It does not use RenderEffect blur, animated shaders or full-screen gradients. This keeps GPU cost and battery impact low on Galaxy Watch7.

## Intentionally unchanged

- `/timetable/file-transfer/v1`
- ICS / CSV / JSON parsers
- Wear Data Layer transfer protocol
- requestId/concurrency behavior
- database schema

## Build verification

`./gradlew :wear:compileDebugKotlin` was attempted. The execution environment could not resolve `services.gradle.org` to download Gradle 9.4.1, so a complete Gradle compile could not be performed here.

## UI unification pass (2026-09-06)

This pass continues the One UI 8 Watch rewrite for Galaxy Watch7:

- Added reusable `OneUiCapsuleSurface` and `OneUiInfoCapsule` components.
- Unified semester, course and timeslot edit cards to the capsule system.
- Reworked course details into capsule information rows with a Galaxy AI-highlighted course header.
- Reworked import screen around Galaxy phone transfer plus local backup capsules.
- Reworked export screen with capsule-based format/scope selection and an emphasized phone-transfer action.
- Reworked delete confirmation to use the same capsule language while retaining destructive colors.
- Kept existing navigation, database model, import/export parsers and Wear Data Layer flow intact.

### Build validation

Use GitHub Actions or a local Android/Gradle environment for the final compile check. Suggested first command:

```bash
./gradlew :wear:compileDebugKotlin
```

Then:

```bash
./gradlew :wear:assembleDebug
```

If both pass, install the resulting Wear APK on Galaxy Watch7 and verify Tile rendering, rotary scrolling, long-press editing, and phone transfer from Galaxy S25+.

## 2026-09-06 UI unification pass 2
- Unified Loading/Error states with Galaxy AI ambient accents.
- Replaced recurrence/day/language/time-format/first-day/theme selection radio UI with One UI capsule selections.
- Reworked course, timetable and time-slot edit forms to use shared capsule surfaces.
- Reworked export format/scope selection to capsule controls.
- Updated color picker entry card to the shared capsule component.
- Removed stray AboutScreen backup source file.
- Gradle verification remains delegated to GitHub Actions because this environment cannot resolve services.gradle.org.

## 2026-09-06 UI unification pass 2

- Normalized all non-dynamic themes to AMOLED black background + neutral dark capsules; presets now change accent colors rather than tinting the entire screen.
- Replaced the remaining destructive FilledTonalButton with the shared OneUiCapsuleSurface pattern.
- Replaced the course detail edit action with the same capsule component and preserved click + long-press behavior.
- Replaced the export primary action with a Galaxy AI emphasized capsule and disabled it while exporting.
- Kept transfer protocol, parsers, Room/database schema, navigation routes, and Data Layer path unchanged.
- Local compile remains unavailable in this environment because the Gradle 9.4.1 wrapper distribution is not cached and services.gradle.org cannot be reached; use GitHub Actions for compile verification.

## About / Developer identity update
- Restored developer identity as **黑白君 (HeiBaiJun)**.
- About page uses the shared One UI capsule design.
- Version text contains a hidden 7-tap developer-options entry.
- Developer Options is read-only and exposes version, fixed Data Layer path, supported formats, Tile/Complication status, and Galaxy AI ambient-effects status.
- No database mutation, destructive debug action, or transfer-protocol behavior is introduced by this screen.
