# Build 10.3 source audit — performance / time format / about

## Fixed

1. **Large schedules stutter on the Wear home screen**
   - `TimetableViewModel` previously converted every course and every time slot of every timetable into UI objects before selecting the requested day.
   - It now filters domain models for the selected day/week first and only converts matching entries.
   - Removed debug logging from the hot path.
   - Home current/next-course detection previously scanned the whole day's list once for every visible item. It is now computed once per minute in one linear pass and shared by all course cards.
   - The home ticker was reduced from an unconditional 30-second wake/recomposition to a minute-boundary update because the displayed value is minute-granularity.

2. **About page stale**
   - Removed the hard-coded `2.0.0` claim from the declaration; the package version shown by the UI is authoritative.
   - Updated the changelog to reflect reliability, performance, time-format, Tile and complication work.
   - Updated maintenance subtitle.

3. **Remaining time wording**
   - Wear app course card now displays `XX分钟下课` (English: `XX min until class ends`).
   - Wear Tile current course adds the same countdown before the course time/location.
   - Current-course complication title uses the same countdown.
   - The last partial minute is shown as at least 1 minute rather than `0分钟下课` while the course is still active.

4. **12-hour format not applying consistently**
   - Home course cards now format start/end using the app preference.
   - Wear Tile reads `PreferenceStorage.appConfigFlow` and formats course times accordingly.
   - Current/next course complications read the same preference and format times accordingly.
   - Top TimeText now uses `h:mm a` in 12-hour mode, making AM/PM explicit.
   - Course detail already used the shared TimeText component and did not need a change.

## Verification

- All XML resource files parse successfully.
- Static scans confirm the affected home/Tile/complication paths no longer contain hard-coded `%02d:%02d` rendering.
- Full Gradle compile could not run in this sandbox because Gradle Wrapper needs `https://services.gradle.org/distributions/gradle-9.4.1-bin.zip` and DNS/network access to that host is unavailable here.

## Recommended CI checks

```bash
./gradlew :shared:test
./gradlew :mobile:assembleRelease --no-daemon
./gradlew :wear:assembleRelease --no-daemon
```

## Notes

The Tile countdown currently refreshes when the Tile is requested/refreshed. For a continuously ticking countdown without refreshing the full Tile each minute, a future improvement can use Wear ProtoLayout dynamic expressions. The current implementation intentionally avoids aggressive refresh frequency to protect battery life.
