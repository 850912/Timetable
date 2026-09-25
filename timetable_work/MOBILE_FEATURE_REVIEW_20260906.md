# Mobile timetable creation feature review — 2026-09-06

## Implemented

- Mobile timetable list backed by the shared Room database model.
- Manual timetable creation (name, semester start/end).
- Manual course creation (course, teacher, location, weekday, start/end time, recurrence).
- Quick text creation using `课程|星期|时间|地点|教师|重复` lines.
  - Repeated rows with the same course metadata are merged into one course with multiple time slots.
- Mobile JSON / ICS / CSV import through Android Storage Access Framework.
- Phone-to-Wear push using Wear Data Layer Assets.
- Wear-side idempotent phone sync: same timetable name + semester start date is replaced instead of duplicated.
- Existing Wear-requested phone file selection and Wear export-to-phone behavior remains supported.
- Shared file parser extracted so phone and watch use the same format detection logic.

## Files added

- `mobile/src/main/java/com/hufeng943/timetable/TimetableDatabaseProvider.kt`
- `mobile/src/main/java/com/hufeng943/timetable/transfer/PhoneWearSyncManager.kt`
- `shared/src/main/java/com/hufeng943/timetable/shared/importexport/TimetableFileParser.kt`

## Main files changed

- `mobile/src/main/java/com/hufeng943/timetable/MainActivity.kt`
- `mobile/src/main/res/layout/activity_main.xml`
- `mobile/src/main/res/values/strings.xml`
- `mobile/build.gradle.kts`
- `gradle/libs.versions.toml`
- `shared/src/main/java/com/hufeng943/timetable/shared/data/dao/TimetableDao.kt`
- `shared/src/main/java/com/hufeng943/timetable/shared/importexport/ImportService.kt`
- `shared/src/main/java/com/hufeng943/timetable/shared/importexport/WearFileTransferProtocol.kt`
- `wear/src/main/java/com/hufeng943/timetable/transfer/WearDataLayerTransferService.kt`

## Review performed

- XML parsing check passed for changed mobile layout/resources/manifest.
- TOML parsing check passed for version catalog changes.
- Kotlin delimiter/syntax-integrity static check passed for changed Kotlin sources.
- Phone/Wear protocol constants and receiver branches were cross-checked.
- Database replacement behavior uses the existing foreign-key CASCADE relationships, so replacing a synced timetable removes its old courses/time slots atomically.
- Existing GitHub Actions workflow still builds `shared`, `mobile`, and `wear`, then validates matching APK signatures.

## Build environment note

A local Gradle build was invoked with:

`./gradlew :shared:test :mobile:assembleDebug :wear:assembleDebug --stacktrace`

The sandbox could not download the Gradle 9.4.1 wrapper because outbound network access to `services.gradle.org` is unavailable. The failure occurred before Gradle loaded the project, so there was no compiler/build error from the source itself in this environment. GitHub Actions has network access and remains the definitive compile check.
