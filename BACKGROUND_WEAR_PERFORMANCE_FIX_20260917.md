# Background visibility + Wear OS performance fix — 2026-09-17

## Baseline
Based directly on `Timetable-3.4.0-optical-glass-home-rework(2).zip`.

## Root cause fixed
`AppBackground()` rendered the selected THEME/IMAGE background and then always rendered a full-screen black alpha scrim above it:
`((1f - backgroundBrightness) * 0.35f)`.
That layer physically obscured both the fluid/theme background and custom image background and added a full-screen alpha blend every frame.

## Changes
- Removed the full-screen black readability scrim entirely.
- Background brightness now modifies the source, not an overlay:
  - THEME: feeds `backgroundBrightness` into `GalaxyAiAmbientLayer.strength`.
  - IMAGE: applies a remembered `ColorMatrix` / `ColorFilter` directly to the image.
- The theme base color remains only underneath the selected background; it is not drawn above it.
- Shared Kyant `LayerBackdrop` is retained so glass samples the real visible background.
- Backdrop capture is now skipped entirely below Android 13, where AGSL liquid glass falls back anyway. This removes needless capture work on unsupported devices.
- Existing 512px sampled RGB_565 background decode is retained to keep bitmap memory bounded on Wear OS.
- Existing low-RAM liquid-glass scaling, shared backdrop, drawWithCache ambient layer, stable lazy-list keys and lifecycle-aware state collection are retained.

## External guidance checked
- Android: reduce overdraw by removing unnecessary backgrounds and transparency.
- Android Wear Compose: measure release builds; use R8 and Baseline Profiles for production performance.
- Android Compose: remember expensive calculations, stable lazy keys, derived state where appropriate.
- Kyant AndroidLiquidGlass: backdrop source + drawBackdrop is the intended architecture.

## Verification
Static source audit confirms the old `scrimAlpha` / full-screen `Color.Black` overlay is gone from AppBackground.
Local Gradle compilation could not run because the sandbox cannot resolve `services.gradle.org`; CI must remain the compiler oracle.
