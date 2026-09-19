# Timetable 3.4.2 startup crash fix — 2026-09-19

## Evidence and root cause

The supplied log shows a fatal exception during the first Wear composition:

`IllegalStateException: Expected an activity context for creating a HiltViewModelFactory but instead found android.app.ContextImpl`

Version 3.4.1 replaced Compose's Activity-backed `LocalContext` with the result of
`createConfigurationContext()` to update language resources without recreating the Activity.
That result is a plain `ContextImpl`. Destination `hiltViewModel()` calls require an Activity
context, so Hilt failed before the first screen could open.

## Correction

- Removed the `LocalContext` and `LocalConfiguration` override from `MainActivity`.
- Passed the Activity-created `AppConfigViewModel` explicitly into the root navigation host.
- Restored a lifecycle-aware, conflated locale event: language selection is persisted first and
  then the Activity is recreated, allowing `attachBaseContext()` to apply the locale safely.
- Reset the process default locale when returning to the system-language option.
- Bumped Wear, mobile, and backup metadata consistently to `3.4.2` / `3040200`.

## Second-pass checks

- The crash signature and every relevant context-creation call were traced against the source.
- No Compose provider replaces `LocalContext` with a non-Activity context.
- XML resources parse and English/Chinese string placeholders match.
- Version values match across Wear, mobile, backup metadata, and About text.
- The existing Xiaomi rotary-haptics compatibility workaround remains enabled.

## Build limitation

The Gradle 9.4.1 distribution is not cached and its download host is unavailable in this
workspace. Run `./gradlew :shared:test :wear:assembleDebug :mobile:assembleDebug` in CI and perform
one launch plus language-switch smoke test on Xiaomi Watch 5 before release signing.
