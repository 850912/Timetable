# Timetable 3.1.0 Fix1 source review

- Fixed Wear AboutScreen compile failure: `OneUiCapsuleSurface` uses `titleMaxLines` and `subtitleMaxLines`, not `maxLines`.
- Audited Wear source for other `OneUiCapsuleSurface` named-argument misuse; no remaining `maxLines` calls target that component.
- No signing configuration or keystore changes.
- Build verification remains delegated to GitHub Actions because the local environment cannot resolve services.gradle.org.
