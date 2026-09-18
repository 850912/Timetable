# Second full-source review — 2026-09-18

Scope: all 385 files in the supplied archive, with focused static review of Wear navigation/motion, locale switching, lifecycle, resources, persistence, sync/error paths and build configuration.

## Fix applied in this pass

### Wear nested navigation used the wrong controller/DSL
`WearInternalNavHost` is a `SwipeDismissableNavHost`, but seven child flows still created a generic `androidx.navigation.compose.rememberNavController()` and imported the generic Navigation Compose `composable` DSL. The app-level host already used the Wear-specific pair.

All child Wear flows now consistently use:
- `androidx.wear.compose.navigation.rememberSwipeDismissableNavController`
- `androidx.wear.compose.navigation.composable`
- `androidx.wear.compose.navigation.SwipeDismissableNavHost`

Affected flows: Settings, Liquid Glass advanced settings, Edit timetable, Edit time slot, Schedule tools, Day arrangement, Course adjustment.

This removes a concrete mismatch in navigator/controller construction and makes the nested graphs follow the same Wear navigation stack as the root graph.

## Rechecked
- Language options use Kotlin data with stable keys and nullable locale tags; no `@null` string-array item is used.
- Language selection no longer pops the nested back stack immediately before Activity recreation.
- Locale persistence mirrors the selected tag synchronously for `attachBaseContext`; DataStore remains the observable configuration source.
- Navigation motion has no custom slide/fade/tween transition layered over `SwipeDismissableNavHost`.
- Remaining `AnimatedVisibility`/`AnimatedContent` occurrences are local content state, not page navigation; the edit-state AnimatedContent explicitly uses no enter/exit transition.
- No production `!!` hits were found in the scanned source set.

## Items deliberately not changed
- Locale implementation still uses `attachBaseContext` + recreation. Android 13+ supports system per-app language APIs, but replacing the existing cross-version mechanism is a migration rather than a minimal crash fix and should be tested on actual Wear OS versions before release.
- `error(...)` calls in sync/import/domain paths remain. They are mostly explicit invariant/protocol failures and should be handled in a separate reliability pass if malformed external data must never abort an operation.
- Blocking `Thread.sleep` exists in legacy/background Wear transport retry code. It is not part of UI navigation and was not changed without tracing the calling dispatcher/service lifecycle.

## Build verification
Attempted `./gradlew :wear:compileDebugKotlin --stacktrace --offline`. The wrapper is not cached and tries to fetch Gradle 9.4.1; this environment cannot resolve `services.gradle.org`, so compilation could not be completed here. Static verification therefore does not claim a successful build.
