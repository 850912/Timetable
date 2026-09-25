# Full UI / background / liquid-glass / navigation audit — 2026-09-16

## Changes applied
- Custom timetable background now uses a document picker, bounds-first sampled decoding, a 720px maximum stored copy, JPEG output in app-private storage, and file validation before the preference is committed.
- IMAGE mode falls back to the theme ambient layer if the stored image cannot be decoded instead of producing an empty/black background.
- Theme ambient background no longer weakens its own glow as brightness is reduced; brightness is handled by the global readability scrim instead.
- Added `GlobalLiquidGlass.kt`: reusable One UI surfaces now sample the single root `LayerBackdrop`, so liquid glass is no longer restricted to current/next timetable cards.
- Applied global glass to `OneUiCapsuleSurface`, `OneUiCapsuleButton`, `OneUiInfoCapsule`, and `OneUiWatchCard`. The root backdrop remains single-instance to avoid multiplying full-screen capture layers.
- Liquid Glass advanced numeric subpages now live in their own `SwipeDismissableNavHost`; swipe/back returns to Liquid Glass Advanced, not Settings.
- The advanced numeric confirm action now uses the same check icon as the other save/confirm EdgeButtons.
- Navigation audit confirmed top-level routes and edit/settings/tool subroutes use `SwipeDismissableNavHost`; the advanced numeric page was the local-state exception and is now migrated to the same navigation pattern.

## External guidance checked
- Android Wear Compose navigation guidance recommends `SwipeDismissableNavHost` for Wear OS back navigation and built-in swipe-back transitions.
- Android Photo Picker / document access guidance recommends system pickers rather than broad storage permissions; this app immediately copies the selected image into app-private storage.
- Kyant Backdrop examples use one captured `LayerBackdrop` and `drawBackdrop` on foreground surfaces; this build follows that architecture globally.

## Build verification
Local Gradle compilation could not start because this execution environment cannot resolve `services.gradle.org` while downloading Gradle 9.4.1. Static source checks, conflict-marker checks, Markdown contamination checks and ZIP integrity checks were performed. GitHub Actions remains the authoritative Kotlin compile check.
