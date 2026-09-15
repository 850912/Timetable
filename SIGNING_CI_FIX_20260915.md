# Signing / CI fix — 2026-09-15

This source package was updated to keep the existing production signing identity without storing private signing material in source control.

Changes:
- Removed `signing/timetable-release.jks` from the source package.
- Removed hard-coded keystore/key passwords from `mobile/build.gradle.kts` and `wear/build.gradle.kts`.
- Release signing now reads `TIMETABLE_RELEASE_STORE_FILE`, `TIMETABLE_RELEASE_STORE_PASSWORD`, `TIMETABLE_RELEASE_KEY_ALIAS`, and `TIMETABLE_RELEASE_KEY_PASSWORD` from Gradle properties or environment variables.
- Release tasks fail when signing material is missing; they do not silently fall back to the debug certificate.
- Added `.gitignore` rules for JKS/keystore/signing property files.
- Added `.github/workflows/release.yml` to restore the JKS from `TIMETABLE_RELEASE_JKS_BASE64`, verify the alias, build mobile + Wear releases, upload APK artifacts, and delete the temporary keystore.
- Removed the hard-coded `/usr/local/bin/aapt2` override so hosted CI can use AGP/Maven-provided AAPT2.
- Fixed the two direct Wear Kotlin route errors identified by the supplied source review: stale `scheduleTools(...)` call and stale developer-probe route UI.

Validation performed:
- No `*.jks`, `*.keystore`, `*.p12`, or `*.pfx` files remain in this package.
- No original hard-coded signing password remains in the source tree.
- No Kotlin references to `scheduleTools` or `MORE_ABOUT_DEVELOPER_PROBE` remain.
- Full Gradle execution could not complete in the sandbox because `services.gradle.org` was not reachable for Wrapper download.
