# Background second-audit fix — 2026-09-17

Baseline: Timetable-3.4.0-background-wear-performance-fix(2).zip

## Fixed after second audit

1. Removed the brightness-regression introduced by direct RGB multiplication.
   - Legacy behavior used black scrim alpha `(1 - setting) * 0.35`.
   - Equivalent source luminance is therefore `0.65 + 0.35 * setting`.
   - Example: the historical 82% default now maps to 93.7% source luminance, rather than incorrectly reducing RGB to 82%.
   - No full-screen scrim is reintroduced.

2. Decoupled fluid-background brightness from ambient-effect opacity.
   - `GalaxyAiAmbientLayer.strength` stays at 1.0 for the root fluid background.
   - New `sourceLuminance` changes source colors directly.
   - Existing component uses of `strength` remain unchanged.

3. Corrected custom-image downsampling.
   - The old loop could decode up to roughly 2x the requested 512 px longest side.
   - The new loop keeps power-of-two sampled decode at or below the 512 px target.
   - RGB_565 remains enabled because the background does not require alpha.
   - `Bitmap.prepareToDraw()` is called after decode to begin texture preparation before first visible draw.

4. Kept the backdrop architecture lean.
   - One root LayerBackdrop only when Liquid Glass is enabled and API >= 33.
   - No full-screen readability scrim.
   - No per-course backdrop capture.

5. Fixed a separate consistency issue found during the same audit.
   - Glass lens distortion persistence is now clamped to 0..0.60 on both read and write, matching the visible UI/renderer range.

## Verification

Static scan confirms the root background no longer contains the old scrim path and no `strength = brightness` root-fluid coupling remains.

Local Gradle compilation could not start because the sandbox cannot resolve `services.gradle.org`; GitHub Actions is still required for compiler verification.
