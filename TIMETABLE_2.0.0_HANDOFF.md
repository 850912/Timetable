# Timetable 2.0.0 handoff

This archive is the current source tree with the remaining Wear-side fixes applied.

## Changes completed

1. Wear timetable editor navigation restored to the original component-owned nested
   `SwipeDismissableNavHost` architecture. `AppNavHost` now exposes only the root
   `NavRoutes.EDIT_TIMETABLE` destination and renders `EditTimetableScreen()`.

2. Wear release version prefix set to `2.0.0`.

3. Theme selection now feeds both Wear MaterialTheme and the project's custom
   `AppTheme.colors` tokens. The selected preset therefore affects screens that use
   those custom tokens as well.

4. Course/timetable `DynamicSubTheme` no longer overrides a manually selected theme
   preset. Seed-color subthemes are limited to the `SYSTEM_DYNAMIC` preset.

5. Export preview/export no longer cache the first timetable list forever. Every
   preview/export reads the current Room Flow, so newly added timetables are included
   in `ALL` exports.

6. JSON import accepts the versioned backup container, a legacy raw `List<Timetable>`,
   and a legacy single `Timetable` JSON object.

7. Public Downloads filesystem scanning is limited to pre-scoped-storage Android
   (API < 29). On modern Android, users should use the SAF picker to open shared files;
   this avoids relying on inaccessible raw Downloads paths after reinstall.

8. Added JVM tests covering multi-timetable JSON round-trip and multi-timetable CSV/ICS
   output.

## Validation

- Basic Kotlin source delimiter/structure checks passed.
- The environment did not run the Android Gradle build. Build through GitHub Actions.
- `release.jks` and local machine configuration are intentionally omitted from this
  clean archive.
