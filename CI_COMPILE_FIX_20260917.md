# CI compile fix — 2026-09-17

GitHub Actions reached `:wear:compileDebugKotlin` and failed only on `CourseCard.kt` because `androidx.compose.foundation.layout.matchParentSize` was unresolved with the project's current Compose dependency set.

Fix applied:
- removed the unresolved `matchParentSize` import;
- replaced the decorative full-card overlay with `Modifier.fillMaxSize()`;
- added the supported `fillMaxSize` import.

This preserves the overlay behavior inside the bounded course-card `Box` without relying on the unavailable extension.

Local Gradle verification could not run because this execution environment cannot resolve `services.gradle.org`; GitHub Actions should be used for the compiler verification.
