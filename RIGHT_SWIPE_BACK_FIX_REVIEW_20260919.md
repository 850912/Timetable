# Wear OS right-swipe back fix review — 2026-09-19

## Scope
Focused review of Wear OS edge right-swipe / swipe-to-dismiss and predictive back ownership, especially while an internal/secondary page is open.

## Root cause
The app-level navigation used `SwipeDismissableNavHost`, but several full-screen child flows used a plain Navigation Compose `NavHost`. This split the visible child back stack from the Wear swipe gesture owner. On a child page, the outer Wear host could therefore react to the edge swipe and pop the outer destination instead of first popping the child's current page.

Affected child flows included Settings, timetable editing, time-slot editing, schedule tools, day arrangement, course adjustment, and the advanced liquid-glass adjustment flow.

## Fix
`WearInternalNavHost` now uses Wear Compose `SwipeDismissableNavHost` rather than a plain `NavHost`.

Every child flow now creates its controller with `rememberSwipeDismissableNavController()` and declares destinations with the Wear Compose navigation `composable` extension.

The internal host observes its back stack and enables `userSwipeEnabled` only when `previousBackStackEntry != null`. Therefore:

- child detail page: child host owns the right-swipe and pops exactly one child entry;
- child start page: child swipe handling is disabled and the parent Wear host can handle the gesture;
- app root: no app destination remains to pop, so system back-to-home remains available.

No custom `BackHandler` or `PredictiveBackHandler` was added, avoiding interception of the platform/navigation predictive-back pipeline.

## Expected navigation behavior
`Home -> Settings -> UI management -> Background`

Right swipe sequence:

1. Background -> UI management
2. UI management -> Settings main
3. Settings main -> Home
4. Home -> system/home (subject to system gesture behavior)

For `Settings -> UI management -> Liquid glass advanced -> numeric adjust`, the innermost host owns the gesture only on the numeric adjustment page. At the advanced page root, ownership falls through to the Settings child host, then to the app host.

## Platform compatibility rationale
Current Android documentation recommends `SwipeDismissableNavHost` for Navigation 2 Wear Compose navigation. Wear Compose 1.5+ uses predictive-back handling for this host on API 36+, while older versions retain Wear swipe-to-dismiss behavior. The project uses Wear Compose 1.6.2.

## Secondary review
Searched all Wear sources using `WearInternalNavHost`. All seven child-flow source files were migrated from `rememberNavController()` to `rememberSwipeDismissableNavController()` and from the Navigation Compose `composable` extension to the Wear Compose navigation extension.

The root activity/navigation path was checked for newly introduced `BackHandler` / `PredictiveBackHandler`; this fix introduces none.

## Build verification limitation
Attempted `./gradlew :wear:compileDebugKotlin --no-daemon`. The sandbox cannot resolve `services.gradle.org`, so Gradle 9.4.1 cannot be downloaded and a real Kotlin compilation cannot be completed in this environment. This is an environment/network limitation, not a reported compiler error from the modified source.

Recommended device tests: Wear OS <=5 / API <=35 edge swipe, API 36+ predictive back, cancelled half-swipe, rotary focus after returning, and repeated deep navigation through Settings and edit flows.
