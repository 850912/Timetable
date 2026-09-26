# Wear OS 6 navigation audit — 2026-09-20

## Problem reproduced from source structure

The Wear app was running two different Compose navigation implementations at the same time:

- app-level pages: `androidx.wear.compose.navigation.SwipeDismissableNavHost`
- settings/edit/tool child pages: `androidx.navigation.compose.NavHost`

That split explains why entering/leaving child pages could visibly follow two different motion models. The previous workaround disabled all child transitions, which removed one animation but did not make navigation consistent.

## Reference checks

- Android's current Wear OS documentation for Compose Navigation 1.6.2 says to use the Wear-specific `compose-navigation` implementation in place of the generic `androidx.navigation:navigation-compose` host for Wear navigation.
- On API 36 / Wear OS 6, `SwipeDismissableNavHost` uses the platform predictive-back path.
- Static inspection of the supplied reference APK found the Wear navigation graph builder present and no generic Navigation-Compose graph builder reference in its primary DEX. No source/assets from the APK were copied.

## Applied change

`WearInternalNavHost` is now Wear-native:

- uses `SwipeDismissableNavHost`
- every internal controller is created with `rememberSwipeDismissableNavController()`
- every internal destination uses the Wear `composable` builder
- generic `androidx.navigation:navigation-compose` is no longer a direct Wear module dependency
- at an internal graph's root, inner swipe handling is disabled so the outer host owns Back
- when an internal child page is open, the inner host owns swipe-to-dismiss and reveals the correct previous child page

This keeps a single transition/gesture model for settings, timetable editing, timeslot editing, schedule tools, date/day selectors, liquid-glass adjustment pages and the root navigation graph.

## Static source checks

- no `rememberNavController()` remains under `wear/src/main/java`
- no generic Navigation-Compose `composable` import remains under `wear/src/main/java`
- no reference-app branding was added to source or resources

## Build limitation

A Gradle compile was attempted, but the wrapper distribution `gradle-9.4.1-bin.zip` is not cached in this environment and `services.gradle.org` cannot be resolved here. The compile therefore stops before project configuration; this is not a Kotlin compiler result.
