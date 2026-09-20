# Timetable 3.5.1 — background / liquid-glass performance fix and full-source review

Date: 2026-09-20

## Scope

This pass starts from `Timetable-3.5.0-reviewed` and uses the supplied 3.4.2 tree as a performance/regression baseline. It specifically addresses:

1. theme / image / image-tone-fluid backgrounds appearing black while a page is idle, even though the configured background can be seen during navigation;
2. Wear OS jank after enabling liquid glass;
3. preserving functional blur, refraction, chromatic-dispersion and profile controls without reintroducing the 3.5.0 cost regression;
4. a static review of the complete source tree after the changes.

Release version is bumped to **3.5.1** (`versionCode 3050100`) in Wear, mobile and Nacho watch-face modules.

## Root cause: configured background hidden by navigation

The global background itself was being drawn correctly. The regression was the value supplied to `LocalSwipeToDismissBackgroundScrimColor` around the app-level `SwipeDismissableNavHost`.

In Wear Compose Foundation, the foreground content of `BasicSwipeToDismissBox` receives `.background(backgroundScrimColor)`. That means this value is not only a temporary gesture overlay: an opaque value can become a full-screen paint layer behind/around the current destination. 3.5.0 supplied `AppTheme.colors.background` (black for the AMOLED preset), so the global theme/image/fluid background was hidden when the destination was at rest. During a swipe, clipping/translation exposed the real background, matching the supplied screenshot.

Fix in `wear/.../presentation/ui/AppNavHost.kt`:

- `LocalSwipeToDismissBackgroundScrimColor = Color.Transparent`;
- keep a light `LocalSwipeToDismissContentScrimColor` (`black @ 10%`) for gesture depth;
- retain a single app-owned `AppBackground` below the navigation host.

Reference source:
- AndroidX Wear Compose Foundation `BasicSwipeToDismissBox.kt`: foreground applies `.background(backgroundScrimColor)`.
- AndroidX Wear Compose Navigation `SwipeDismissableNavHost.kt`: uses the Foundation swipe container and the two composition locals.

## Fluid image background

`imageBackgroundFluidEnabled` now means **a generated moving background based on the selected image's tones**, rather than moving the original bitmap.

Implementation changes:

- sample four image regions; each tone averages a 5×5 grid instead of using a single pixel;
- generate one diagonal gradient plus two broad radial tone fields;
- animate only graphics-layer translation of the radial layers;
- do not run the infinite transition when global UI animations are disabled;
- if "background blur" is enabled together with fluid mode, use broader/softer gradients rather than a full-screen `RenderEffect` blur;
- custom image readability keeps a moderate contrast veil (`0.24 + brightness compensation`, capped at 0.50) so white Wear text remains usable without blacking out the photo.

The only remaining full-screen Compose blur in `AppBackground` is the optional **non-fluid custom-image blur**, one layer at 10dp.

## Wear OS liquid-glass performance

### 3.5.0 regression

3.5.0 changed the glass renderer so the user-facing 0–8 blur value could become up to **8dp live Backdrop blur on every visible glass surface**. A scrolling Wear list can draw several such surfaces simultaneously. This is the main code-level regression versus the supplied 3.4.2 baseline, which used a very small live blur (about 0.55dp only on its strongest path).

### 3.5.1 renderer

`wear/.../components/GlobalLiquidGlass.kt` was reworked with a Wear-budgeted path:

- keep `io.github.kyant0:backdrop:2.0.1`; it is the current upstream stable release and changing the rendering library is not required to fix this regression;
- map the 0–8 UI blur level monotonically to a sub-dp optical blur:
  - Soft max ~0.34dp
  - Balanced max ~0.52dp
  - Fluid max ~0.72dp
- use refraction/highlight/inner-shadow differences to make profiles visibly distinct instead of paying for large blur radii;
- run the expensive depth effect only for Fluid when lens strength is meaningful;
- keep chromatic dispersion opt-in and functional;
- use `LocalScreenIsActive` so an inactive Wear navigation page falls back to the cheap material renderer during swipe/navigation instead of running full backdrop shaders on both pages;
- low-RAM watches and API <33 use the lightweight cached gradient fallback;
- when the fallback will be used, `AppNavHost` does not install the global `layerBackdrop` capture at all;
- effective power-save mode still disables glass, background motion and background blur.

Android's Wear Compose API explicitly documents `LocalScreenIsActive` as a hook for updating UI or performing optimizations on inactive screens during paging/swipe transitions.

The Backdrop project also has public reports of `LayerBackdrop` lag on lower-end devices/lazy lists, so keeping the number and strength of live effects small on Wear is deliberate.

## Liquid-glass settings

The advanced settings stay backwards-compatible with the existing stored `Float` 0–8 value, but the UI now labels it as an **effect level** rather than claiming the number is a literal dp blur radius. This reflects what the Wear renderer actually does.

- blur toggle changes whether the Backdrop blur pass runs;
- blur level 0–8 changes the sub-dp optical radius monotonically;
- lens distortion changes refraction geometry;
- chromatic dispersion remains an explicit toggle;
- Soft / Balanced / Fluid alter lens scaling, max blur, highlight and inner-shadow behavior.

## Full-source static review

Reviewed tree: 394 files total, including 210 Kotlin files, 6 Kotlin Gradle scripts, 36 XML files, 11 JSON files and the version catalog TOML.

Checks performed after the changes:

- all 36 XML files parse successfully;
- all 11 JSON files parse successfully;
- TOML parses successfully;
- no UTF-8 decode failures or NUL bytes in text/source files;
- no merge-conflict markers;
- no TODO/FIXME/HACK/XXX markers in non-Markdown source/config;
- no `GlobalScope` usages;
- no production `!!`; the three remaining `!!` are confined to Android instrumented tests;
- no obvious hard-coded private keys, GitHub tokens, Google API keys or long secret assignments;
- all statically referenced local Android resources resolve; `R.raw.aboutlibraries` is the expected generated resource from the AboutLibraries Gradle plugin;
- no duplicate Wear value-resource names in `values`, `values-round` or `values-zh-rCN`;
- 3.5.1 / 3050100 is consistent across Wear, mobile and watch-face modules;
- only one `rememberInfiniteTransition` remains in production source (the optional image-tone fluid background);
- only one `drawBackdrop` implementation remains (centralized in `GlobalLiquidGlass`), and only one global `layerBackdrop` capture remains;
- the two `while(true)` loops in Wear UI are suspending/event loops (`delay`/pointer-event wait), not busy loops;
- the three `Thread.sleep` calls are in legacy Data Layer retry code running on explicit/dedicated worker threads, not Compose rendering;
- exported complication/tile services use their corresponding bind permissions. Other exported app components are unchanged by this pass and should continue to be validated through normal Android component-level security testing.

A syntax-oriented `kotlinc` pass on the three changed Kotlin files found no parser diagnostics. Full Android symbol/type verification still requires the Gradle Android classpath.

## Build verification status

Attempted:

```text
./gradlew :shared:test :wear:compileDebugKotlin :mobile:compileDebugKotlin --stacktrace
```

The build cannot start in this execution environment because the Gradle wrapper distribution is not cached and downloading `https://services.gradle.org/distributions/gradle-9.4.1-bin.zip` fails with DNS/network `UnknownHostException`. A direct download attempt also failed. Therefore this review **does not claim a successful Gradle/Android compile**.

Recommended device validation after opening in Android Studio / a network-enabled CI runner:

1. run `:shared:test :wear:assembleDebug :mobile:assembleDebug`;
2. on an API 33+ physical Wear watch, check theme, custom image and fluid image-tone backgrounds while idle and while swiping;
3. profile a long settings/course list with glass Off, Soft, Balanced and Fluid; inspect frame time/JankStats or system trace;
4. toggle blur 0/4/8, lens 0/30/60%, and dispersion to confirm the optical difference without a large frame-time jump;
5. test low-RAM/power-save behavior and AOD/watch-face behavior separately.

## External references consulted

- AndroidX Wear Compose Foundation API / source for `LocalScreenIsActive`, swipe scrims and `BasicSwipeToDismissBox`.
- AndroidX Wear Compose Navigation source for `SwipeDismissableNavHost`.
- Android Developers Wear performance guidance.
- Kyant0/AndroidLiquidGlass (Backdrop) official repository/release/docs; 2.0.1 is the latest release visible during this review.

No new third-party library was added in this pass.
