# Build 10.5 Fix4 — Mobile input UI & calendar cleanup

- Fixed Material text-field label/placeholder overlap by using `TextInputLayout.placeholderText` rather than a second child hint.
- Added a dedicated two-button calendar row: sync calendar / clear calendar.
- Clear calendar removes only events whose description starts with the current timetable marker `[Timetable:<id>]`.
- Calendar permission continuation preserves whether the requested operation was sync or clear.
- Includes the Fix3 cross-module nullable `LocalTime` compile fix.
- Signing configuration and release keystore are unchanged.

Local Gradle compilation was not repeated because the current environment previously failed while resolving the Gradle distribution. Static source review was performed instead.
