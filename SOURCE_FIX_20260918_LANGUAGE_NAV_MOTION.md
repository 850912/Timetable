# Wear language crash + navigation motion fix (2026-09-18)

## Root causes addressed

1. The language picker represented the system locale with `<item>@null</item>` inside a `string-array`, then treated values returned by `Resources.getStringArray()` as ordinary non-null strings and also used them as lazy-list keys. This is fragile because `@null` is a resource null sentinel, not the literal string `"@null"`. The picker and settings summary now use typed Kotlin options with explicit nullable locale tags and stable non-null keys. The obsolete resource arrays were removed.
2. Language selection previously started two navigation/lifecycle transitions at once: it popped the internal settings route immediately and asynchronously emitted an Activity `recreate()` event after persistence. The pop was removed; locale recreation is now the single transition after the preference is stored. Selecting the already-active language is a no-op.
3. Internal feature flows used a plain Navigation Compose `NavHost` with custom alpha fades, while app-level pages used Wear `SwipeDismissableNavHost`. That produced two different motion systems. `WearInternalNavHost` now uses the same official Wear navigation host. Its swipe gesture is enabled only when the internal controller has a page to pop, so the nested host does not steal the outer host's swipe gesture at its start destination.

## Verification

- Source scan confirms no custom `fadeIn`/`fadeOut`/slide/tween destination transition remains in Wear navigation code.
- Source scan confirms `language_values`, `language_labels`, and the `@null` language sentinel are no longer referenced.
- Local Gradle compilation cannot be completed in this sandbox because the Gradle wrapper needs to download Gradle 9.4.1 from `services.gradle.org`, which is not reachable from the container. Final compile/device verification should run in the existing CI environment.

## Reference

Android's Wear Compose navigation guidance recommends Wear-specific navigation (`SwipeDismissableNavHost`) rather than mobile Navigation Compose for Wear navigation, and places it under `AppScaffold` with screen-level `ScreenScaffold`. The project remains on stable Wear Compose 1.6.2 rather than moving this hotfix to the 1.7.0 release candidate.
