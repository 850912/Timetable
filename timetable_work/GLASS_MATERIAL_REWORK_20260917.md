# Glass material rework — 2026-09-17

## Goal
Fix Wear OS jank from many simultaneous backdrop effects and add an app-wide glass material.

## Architecture
- Added `isGlobalGlassMaterialEnabled` preference and UI toggle.
- Global material captures the app background once at `AppNavHost` and applies one full-screen blur/tint pass.
- Reusable capsules/cards switch to cheap translucent material drawing when global material is active; they do not each run backdrop blur/refraction.
- Course cards skip their bespoke backdrop shader while global material is enabled.
- Legacy Liquid Glass/Frosted Glass remain available, but general liquid surfaces now use much smaller blur and only allow a restrained lens in FLUID mode.
- Removed vibrancy/chromatic-aberration from general reusable surfaces to reduce GPU pressure and visual noise on Wear OS.

## Rationale
Android Compose documentation notes that blur/render effects require separate graphics layers/offscreen rendering. A list containing many independently blurred/refraction surfaces therefore scales poorly on a watch GPU. Kyant0 Backdrop supports sharing one backdrop source; this rework makes that shared source the default architecture for the new global material.

## Verification
Run `:wear:compileDebugKotlin` and release/profile benchmarks on a physical watch. Compare frame timing while rapidly scrolling settings and timetable with Global Glass Material on/off.
