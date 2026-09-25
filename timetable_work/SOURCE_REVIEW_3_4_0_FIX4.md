# Timetable 3.4.0 Fix 4 — source audit

## Scope
Audited the uploaded Fix 3 tree (193 Kotlin files / 30 XML files), compared existing Wear UI files against the supplied 3.3.1 modified-source baseline, inspected navigation, timetable editing, multi-weekday creation, batch schedule operations, day arrangement/course adjustment, database migration/sync fields, and signing inputs.

## Definite defects fixed
1. `NavRoutes.editTimetable(null)` generated `edit_timetable/null`, while the editor expects a numeric route argument. It now uses the existing `-1` new-item sentinel.
2. `NavRoutes.editTimeSlot(courseId, null)` had the same `.../null` defect. It now uses `-1`.
3. Timetable List's Add button navigated to the route template literal (`edit_timetable/{tableId}`) instead of a concrete destination. It now calls `editTimetable()`.
4. `TimeSlotListScreen` still called the deleted `NavRoutes.multiDateSlot(...)`. This was a guaranteed compile error and contradicted the requirement that multi-select be integrated into the normal TimeSlot editor. The obsolete entry/button was removed.
5. `EditTimeSlotScreen` passed a removed `is24HourFormat` named argument to `EditTimeSlotMainPager`, another guaranteed compile error. Removed.
6. Quick schedule tools had lost single-course scope and always passed `courseId=null`. Course scope selection is restored and forwarded to both normal actions and temporary-holiday actions.
7. Timetable picker showed every timetable's courses at once and immediately returned on timetable tap, so the requested timetable → course → lesson drill-down could not actually be browsed. It now expands one timetable, then one course, then shows lesson details, and commits the selected timetable with the standard bottom confirmation button.
8. The old standalone “multi-date creation” navigation was removed. Weekday multi-select remains inside Edit TimeSlot and persists selected weekdays as independent TimeSlot rows linked by `batchGroupId`.

## Static validation
- Parsed all 30 XML files successfully.
- Checked for stale `multiDateSlot` navigation: none remains.
- Checked concrete new-item route generation for timetable/time-slot editors.
- Verified `SimpleMessageScreen` exists.
- Verified `batchGroupId` remains in the shared persistence/sync model.
- Verified schedule tools forward selected course scope.
- Verified the hierarchical timetable picker has explicit bottom confirmation.
- Bundled release JKS SHA-256 remains `cd6f6307601cee8dc3c77491ae21bddd37e93cac2e111bf7d3ca0d371c26e233`.

## Build limitation
Attempted `:shared:test :wear:compileReleaseKotlin :mobile:compileReleaseKotlin`. The wrapper cannot start in this sandbox because `services.gradle.org` cannot be resolved, so Gradle 9.4.1 cannot be downloaded. Full Kotlin/Compose compilation must still be verified by GitHub Actions. This is an environment/network limitation, not a passing-build claim.
