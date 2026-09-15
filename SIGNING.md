# Release signing

The repository no longer contains a keystore or hard-coded signing passwords.

For production builds that must update an already-installed release, provide these Gradle properties or environment variables with your existing release key:

- `TIMETABLE_RELEASE_STORE_FILE`
- `TIMETABLE_RELEASE_STORE_PASSWORD`
- `TIMETABLE_RELEASE_KEY_ALIAS`
- `TIMETABLE_RELEASE_KEY_PASSWORD`

When they are absent, release builds fall back to the standard debug signing key. This keeps GitHub Actions and source-only test builds installable, but those APKs are **not suitable as production update artifacts** because their signing identity is not stable.

For GitHub Actions, store the real keystore outside the repository (for example as an encrypted GitHub Actions secret), materialize it during the workflow, and set the four variables above before running Gradle.
