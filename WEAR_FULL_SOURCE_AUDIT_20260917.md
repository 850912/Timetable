# Wear OS full source audit — 2026-09-17

## Scope

Reviewed the uploaded project with emphasis on Wear OS navigation, liquid-glass settings/rendering,
home timetable presentation, background rendering, lazy-list patterns and rendering cost.

## Navigation / return animation

- Kept the app-level `AppScaffold -> SwipeDismissableNavHost -> ScreenScaffold` architecture used by
  Android's Wear Compose guidance.
- Removed custom `LocalSwipeToDismissBackgroundScrimColor` and
  `LocalSwipeToDismissContentScrimColor` overrides from the root host. The transition and scrim are
  now owned by Wear Compose instead of being partially customized by the app.
- Home paging now uses Wear Material 3 `HorizontalPagerScaffold` and `AnimatedPage`, with the
  official spring snap behavior from `PagerScaffoldDefaults`.
- Existing nested editors keep Wear navigation for now because flattening them into the root graph
  requires moving editor-local state into shared ViewModels/SavedStateHandle. No custom enter/exit
  transition has been added.

## Liquid glass settings

All visible controls now map to active rendering parameters:

- Master switch -> enables/disables the shared backdrop capture.
- Effect profile -> changes lens multiplier, blur multiplier, surface alpha, highlight and depth.
- Glass opacity -> renderer range is 10–70%; UI range is now also 10–70% (previously the UI allowed
  values that the renderer clipped, which made part of the control appear non-functional).
- Refraction strength -> directly changes both lens refraction height and amount over the full 0–60% range.
- Light blur -> active in every profile when enabled; profile only scales cost/intensity.
- Blur strength -> stored/rendered range is now consistently 0–2 dp.
- Chromatic aberration -> active in Balanced/Enhanced when enabled.
- Background brightness -> continues to control the root background readability scrim.

Retired frosted/global glass flags are now compatibility-only DataStore keys. They are folded into
the visible liquid-glass master switch when reading old settings, and are cleared when the switch is
changed. The duplicate fields were removed from `AppConfig` and active UI/rendering code.

No active Wear source contains the previous reference-app brand name.

## Timetable home visual rework

- Glass course cards now use the theme surface as the optical glass base instead of filling the glass
  with the course color.
- Course color is retained as a subtle reflected tint, accent rail, status border and progress accent.
- Heavy colored borders were reduced and neutral highlights increased.
- In glass mode, the per-card three-gradient ambient layer is removed; the shared backdrop already
  provides depth. This reduces repeated GPU draw work while keeping a single inexpensive course tint.

## Wear OS performance

- One shared backdrop capture remains at the app background; cards sample it rather than each creating
  an independent source.
- Low-RAM devices scale the *full* refraction/blur control range rather than clipping at a threshold,
  so controls remain responsive while maximum cost stays lower.
- Home pager indicator and page motion are delegated to Material 3 scaffold/AnimatedPage rather than
  a separate AnimatedVisibility layer.
- Existing TransformingLazyColumn item keys and lifecycle-aware Flow collection were retained.
- Custom image background stays downsampled to 512 px and RGB_565.
- R8/profileinstaller configuration was not weakened.

## Source-wide checks

- No direct `collectAsState()` calls remain in Wear UI; observed flows use lifecycle-aware collection.
- Dynamic lazy lists reviewed in the main timetable/edit/settings flows use stable keys where data
  identity matters.
- No TODO/FIXME/HACK markers were found in active Wear Kotlin source.
- No active references remain to retired `isFrostedGlassEnabled` / `isGlobalGlassMaterialEnabled` fields.

## Build verification

A local `:wear:compileDebugKotlin` run could not start because the sandbox cannot resolve
`services.gradle.org`, so Gradle 9.4.1 could not be downloaded. The changes therefore still require
GitHub Actions / a normal Android build environment for compiler verification.
