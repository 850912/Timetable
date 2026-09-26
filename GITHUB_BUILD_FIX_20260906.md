# GitHub build fix — 2026-09-06

Fixed errors reported by `:wear:assembleRelease`:

1. `CourseCard.kt`: renamed the file-local capsule shape to `CourseCapsuleShape` to avoid declaration ambiguity with the shared `OneUiCapsuleShape`.
2. `DeveloperOptionsScreen.kt`: removed the out-of-scope `capsuleModifier()` helper and applies `minimumVerticalContentPadding` / `transformedHeight` directly inside each `TransformingLazyColumn` item.
3. `GalaxyAiEffects.kt`: renamed the outer alpha factor to `effectAlpha` so `graphicsLayer { alpha = 0.95f }` writes the layer property rather than trying to reassign a captured `val`.
4. `MainTileService.kt`: caches `semesterEnd` to local `endDate` before nullable comparison, allowing Kotlin smart cast across the shared-module API boundary.

No transfer protocol, parser, database schema, or file format changes were made.
