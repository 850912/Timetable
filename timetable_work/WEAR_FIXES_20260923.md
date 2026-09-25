# Wear OS fixes — 2026-09-23

This revision addresses the nine Wear OS issues reported against Timetable 3.5.3.

## 1. Child-page navigation artifact

- `androidx.wear.compose:compose-navigation` is pinned to 1.4.1 while Wear Material 3 and Foundation remain on 1.6.2.
- This deliberately avoids the API 36+ PredictiveBack navigation implementation whose forward transition translates the outgoing destination before it disappears.
- The app no longer forces swipe-dismiss scrims transparent, and the root nav host is clipped to its bounds.
- No liquid-glass renderer was removed to achieve this fix.

## 2. Export destinations are phone-side only

`ExportTarget` is now exactly:

- `PHONE_APP`: import a canonical JSON backup into the paired phone app.
- `PHONE_FILE`: save the selected ICS/CSV/JSON file under the phone's `Downloads/Timetable` folder.
- `BOTH`: do both phone-side operations.

The watch no longer launches a local Storage Access Framework document picker for export. Google Data Layer and the China-ROM BLE fallback both carry the same destination flags. Older requests that do not contain the new flags retain the previous compatibility behavior.

## 3. About > Personal page

The previous custom watch-to-phone profile transport, phone receiver/service, and shared profile payload classes were removed. The button now uses AndroidX Wear `RemoteActivityHelper` with an `ACTION_VIEW`, browsable HTTPS intent so the official Wear OS remote-activity path opens the page on the companion phone.

## 4. Custom image blur

- Added persistent `customImageBlurRadius` preference.
- Added a 0–24 dp adjustment page using the shared wheel picker.
- The custom background image applies `Modifier.blur(...)` only when the value is above zero.
- Built-in background choices preserve the previous one-tap return behavior; custom image selection stays on the page so blur can be tuned immediately.

## 5. Unified scrolling selectors

All app-owned `Picker`, `TimePicker`, and `DatePicker` use the shared Wear Material 3 picker surface and the app's selected/unselected color treatment. Holiday-day counts, adjustment offsets, weekdays, integer settings, time selection, and date selection now share the same presentation and haptic behavior.

## 6. App watch face removed

The `watchface-nacho` module and its Watch Face Format resources were removed from the project and from `settings.gradle.kts`. Existing complication data-source services remain intentionally available for users who add Timetable complications to a system or third-party watch face; they are not an app-owned watch face.

## 7. Tile rewritten with Jetpack

`MainTileService` now extends Jetpack `Material3TileService` and builds a ProtoLayout Material 3 `primaryLayout` with an expanding `titleCard`. This delegates resources, Material styling, dynamic theme behavior, and responsive screen margins to the current Jetpack Tiles/ProtoLayout stack. Horologist tile dependencies were removed.

## 8. Wear OS / Xiaomi Watch 5 support

- Xiaomi is no longer forced onto a reduced liquid-glass renderer in normal mode.
- User-initiated BLE sync/export requests high connection priority and uses low-latency scanning.
- MTU negotiation now safely falls back to the standard MTU if a vendor stack fails to deliver the callback instead of aborting the whole transfer.
- Shared picker haptics continue to use the existing Xiaomi Wear haptic compatibility path.
- The rewritten Tile is responsive instead of using fixed-size content that could crop on larger round displays.

## 9. Smoothness and visual effects

The normal liquid-glass path stays enabled on Xiaomi and other Wear OS devices; only the app's explicit power-save policy can disable effects. Material 3/Foundation remain current at 1.6.2, and the navigation fix is isolated to the navigation artifact. Existing press/motion effects are retained rather than replaced by a static UI.

## Validation performed in this environment

- `git diff --check`: pass.
- Modified XML resources/manifests: parsed successfully.
- `gradle/libs.versions.toml`: parsed successfully.
- Modified Kotlin files were passed through the installed Kotlin compiler for syntax diagnostics; no parser-level errors such as unmatched braces or unexpected tokens were found. Android/project symbols are unresolved without the Gradle classpath, so this is not a replacement for a real Android build.

A full Gradle compile could not be executed in this sandbox because the Gradle wrapper requires downloading Gradle 9.4.1 and the environment cannot resolve/access `services.gradle.org`. Run the normal debug build on a networked Android development machine before release, then verify navigation on the affected Wear OS 6/API 36 device and on Xiaomi Watch 5 hardware.
