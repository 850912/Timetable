# Background / options audit — 2026-09-17

## Root cause fixed

The app-level background itself was being composed correctly, including the theme ambient layer and decoded custom image. The important covering layer was Wear Compose navigation: `BasicSwipeToDismissBox` exposes background/content scrim composition locals whose defaults are black. The app now provides transparent swipe scrims around the navigation host so the single app-level background remains visible through normal destinations and transitions.

The optional full-screen `Modifier.blur()` pass was also removed from `AppBackground`. It duplicated Haze's material blur path, reduced image/theme-background legibility, and created an extra full-screen render pass on Wear OS. The persisted keys remain for backward-compatible preference reads but no longer affect rendering or appear in settings.

The background readability scrim is retained for text contrast, but its maximum opacity is reduced from 0.70 to 0.35. New installs default background brightness to 0.82 instead of 0.62.

## Options cleanup

- Removed the duplicate top-level `玻璃材质 · 全局` and `全局磨砂玻璃` switches. The visible settings entry is now one Haze glass-material entry.
- Removed the Android 13 hard gate from glass settings. Haze owns platform fallback behavior.
- Removed `高饱和度色彩` from UI because the value was persisted but never consumed by the Haze renderer.
- Removed `显示模糊背景` and `背景模糊强度` from UI because they duplicated glass blur and obscured custom/theme backgrounds.
- Legacy global/frosted preference keys are retained for migration compatibility. Toggling the visible Haze glass switch normalizes those old modes into the single liquid/Haze path and clears the legacy flags.

## Background implementation

Custom images continue to use Android Photo Picker, are resized to a 720 px maximum side, encoded once into app-private storage, verified before activation, and decoded off the UI thread using RGB_565. Theme background remains a lightweight static Compose draw layer; no new animation/shader dependency was added because it would add continuous GPU work on Wear OS.

## External verification

- AndroidX Wear Compose documents `LocalSwipeToDismissBackgroundScrimColor` and `LocalSwipeToDismissContentScrimColor`; both default to black.
- AndroidX Wear Material 3 documents `AppScaffold.containerColor`; transparent is appropriate when a separately rendered app background is used.
- Haze 2 documents `HazeInput.Backdrop` as the normal built-in Blur/Glass input and owns fallback/source behavior.

## Build status

`:wear:compileDebugKotlin` was attempted. The wrapper could not obtain Gradle 9.4.1 because this execution environment could not resolve `services.gradle.org`, so a successful Gradle compile is not claimed.
