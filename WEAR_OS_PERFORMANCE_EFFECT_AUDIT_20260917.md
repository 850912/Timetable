# Wear OS performance & effects optimization — 2026-09-17

## Applied
- Kept Kyant AndroidLiquidGlass/Backdrop 2.0.1 and the single shared root backdrop.
- Added automatic low-RAM-device adaptation via ActivityManager.isLowRamDevice.
- Removed routine blur from SOFT/BALANCED glass; FLUID uses at most a 0.55dp blur on normal-memory devices.
- Low-RAM watches disable depth-effect and chromatic aberration and cap lens strength.
- Retained vibrancy, lens refraction, highlight and surface tint so the glass still reads as optical glass rather than plain transparency.
- Press feedback now animates only scale through graphicsLayer; removed alpha animation to reduce transparent overdraw during interaction.
- Custom watch background decode target reduced from 720px to 512px while retaining RGB_565 to lower bitmap memory/bandwidth.
- Existing TransformingLazyColumn + snap rotary behavior and stable keys were retained.
- Existing release R8/resource shrinking retained.

## Why
Wear OS devices have much tighter CPU/GPU and memory budgets. Android guidance recommends release/R8, baseline profiles, avoiding layout-phase animations, and reducing transparency/overdraw. The project already uses graphicsLayer for press scale and TransformingLazyColumn; this pass targets the expensive GPU effects instead of removing the visual identity.

## Follow-up measurement
For measurable rather than subjective performance, add a Macrobenchmark/Baseline Profile module and collect startup + scroll + navigation CUJs on physical watches. This was not added blindly because profile generation should be recorded against the actual release application/device journey.
