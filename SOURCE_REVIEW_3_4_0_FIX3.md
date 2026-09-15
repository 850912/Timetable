# Timetable 3.4.0 Fix 3 source review

- Restored non-course-card Wear UI files from the supplied 3.3.1 baseline where prior 3.4.0 work had changed global styling.
- Kept the Samsung-inspired redesign isolated to `CourseCard.kt` and restored `DayFinishedCard` behavior.
- Restored the full About and Developer pages from the supplied 3.3.1 baseline.
- Multi-select lesson creation now selects weekdays (Mon–Sun) and materializes one independent TimeSlot per selected weekday; siblings share `batchGroupId` only for optional time synchronization.
- Reworked temporary holiday / shift / cancel / restore tools around the existing ScheduleBatchOperations implementation and original M3 list/picker interaction patterns.
- Reworked day arrangement and course adjustment selectors: explicit timetable submenu, expandable course/time-slot detail, date picker, final bottom confirmation.
- Added persistent adjustment snapshots so day arrangement/course adjustment can restore prior slot data, including permanent re-parenting and newly-created temporary slots.
- New/edit tool subpages suppress the global top TimeText; explicit TimeText instances in settings/import/export use transparent background.
- Dynamic theme glow uses the active primary/secondary colors rather than fixed purple/cyan while retaining original layout.
- XML parse audit passed (30 files). Release JKS SHA-256 remains cd6f6307601cee8dc3c77491ae21bddd37e93cac2e111bf7d3ca0d371c26e233.
- Full Gradle compile could not be run in the local sandbox because Gradle 9.4.1 distribution download is blocked by network/DNS; GitHub Actions remains the authoritative compile check.
