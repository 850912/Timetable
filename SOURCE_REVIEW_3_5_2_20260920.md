# Timetable 3.5.2 source review — 2026-09-20

## Scope

Reviewed the complete `mobile`, `wear`, `shared`, Gradle, resources, and GitHub Actions source after the 3.5.2 changes. The Nacho watch face is intentionally removed from the Timetable multi-module project and delivered as a separate standalone WFF project.

## Navigation regression: root cause and fix

The regression is in the app-level `SwipeDismissableNavHost`, not the nested `WearInternalNavHost`.

Wear Compose 1.6.x keeps the previous route as the background and animates the newly pushed route from roughly 75% scale to 100%. The 3.5.1 background fix changed `LocalSwipeToDismissBackgroundScrimColor` to transparent so the selected wallpaper could stay visible. That also made the retained previous route visible around the circular edge during the forward zoom, recreating the apparent “previous page moves / disappears” artifact.

For 3.5.2 the host uses Wear Compose's supported `LocalReduceMotion` / `ReduceMotion` path for the app-level navigation host. This makes forward navigation snap directly to the new route instead of running the problematic scale-in while retaining swipe-to-dismiss for back navigation. The global wallpaper remains transparent through the host, so the 3.5.1 black-background regression is not reintroduced.

Upstream reference: `SwipeDismissableNavHost` checks `LocalReduceMotion` before its forward `Animatable` zoom. AndroidX source: https://android.googlesource.com/platform/frameworks/support/+/6a0598b19b43993d4186ef16a013a6ee8d631d41/wear/compose/compose-navigation/src/main/java/androidx/wear/compose/navigation/SwipeDismissableNavHost.kt

## Global Liquid Glass

The current glass renderer is now applied through the shared UI surfaces used across the Wear app, including:

- course cards and status cards;
- setting / selection / toggle rows through `OneUiCapsuleSurface`, `OneUiSwitchCapsule`, `OneUiCapsuleButton`, and `OneUiInfoCapsule`;
- editor and import/export option containers;
- pull-to-date “Today” control;
- horizontal date cells.

All current advanced controls remain wired into the shared renderer: material profile (Soft / Balanced / Fluid), glass opacity/tint, clarity, chromatic aberration, lens distortion, blur enable and blur level. Lens distortion is no longer hidden for the Soft profile; the profile still scales its intensity inside the renderer.

A fallback bug was also fixed: on low-RAM devices or devices where live Backdrop is unavailable, `globalLiquidGlass()` draws a lightweight glass fallback. Several containers then painted an opaque `surfaceContainer` on top because transparency previously depended on `LocalLiquidGlassBackdrop != null`. In 3.5.2 container opacity depends on the user glass toggle instead, so the fallback remains visible.

The live renderer still avoids the expensive path on inactive navigation pages, low-RAM devices, pre-Android-13 devices, and effective power-save mode.

## Standalone Nacho watch face

The watch face is no longer included as a Gradle module in Timetable. Installing the Timetable Wear APK therefore does not install this watch face.

A separate project is produced as `Nacho-WatchFace-3.5.2-standalone.zip`. It is a resource-only Watch Face Format project (`android:hasCode="false"`), WFF version 1 / minSdk 33, with an Ambient/AOD variant and its own GitHub Actions build workflow.

Official WFF packaging guidance explicitly recommends keeping Watch Face Format bundles separate from Wear OS app logic: https://developer.android.com/training/wearables/wff/setup

## GitHub Actions review

The supplied prior run `35492422128 / job 106029529599` is reported by GitHub as succeeded on 2026-09-20. Public unauthenticated access does not expose the full step logs, but the job page shows the Node 20 deprecation warning for `actions/upload-artifact@v5`.

The workflow has been updated to `actions/upload-artifact@v7`, whose current action metadata runs on Node 24. Reference: https://github.com/actions/upload-artifact

## Static source audit

Project totals after separation: 210 Kotlin files, 5 Kotlin Gradle scripts, 32 XML resources/manifests, 11 JSON files, 1 TOML version catalog, plus resources/docs.

Checks completed:

- XML: all 32 Timetable XML files parse successfully.
- JSON: all 11 JSON files parse successfully.
- TOML: version catalog parses successfully.
- Merge-conflict markers: none in source/config.
- Production Kotlin `TODO` / `FIXME`: none.
- Production Kotlin `GlobalScope`: none.
- Production Kotlin `!!`: none; three occurrences remain only in Android instrumentation tests where fixture existence is asserted.
- Obvious hard-coded secret assignment scan: no hits.
- Changed Kotlin files were also passed through `kotlinc` as a parser sanity check. Compilation necessarily reports unresolved Android/Compose symbols without the Android classpath, but no Kotlin parser / “expecting token” errors were found.

## Build verification limitation

This environment does not contain an Android SDK/Gradle distribution cache and does not provide direct network access to Gradle/Maven from the container, so a full Android Gradle build of these new changes could not be executed locally. The supplied GitHub run verifies the preceding 3.5.1-era project could complete its CI release job; 3.5.2 should still be built in GitHub Actions before release.

The standalone watch-face project includes its own workflow that runs `:app:assembleDebug` and uploads the installable debug APK as an artifact.
