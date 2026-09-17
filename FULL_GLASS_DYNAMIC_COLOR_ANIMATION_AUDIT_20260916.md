# Full glass / dynamic color / animation audit — 2026-09-16

## Scope
Re-reviewed wear, mobile and shared source after the visual-system changes. 202 Kotlin files were scanned. The business/database/sync changes from the prior v11 adjustment-history fix were intentionally left untouched.

## Changes
- Fixed Wear Material 3 dynamic color: the DataStore toggle is now consumed by `TimetableTheme`; when enabled, `dynamicColorScheme(context)` has priority and the custom `AppTheme` colors are derived from the same scheme. Previously the toggle was persisted but never passed into the theme, and dynamic color was only attempted under `SYSTEM_DYNAMIC`.
- Added a visible dynamic-color switch to Settings.
- Added a Liquid Glass advanced second-level Settings page: enable/disable, glass opacity, Soft/Balanced/Fluid refraction profile, and background brightness/readability control.
- Applied the lightweight translucent glass material to the reusable capsule/info/watch-card surfaces across the app. Full AGSL refraction is deliberately limited to current/next timetable cards so long lists do not run one expensive shader stack per card.
- Re-tuned the refraction profiles: lower blur, controlled lens strength, chromatic aberration only where appropriate. This follows the common liquid-glass pattern of clear refraction plus a restrained surface tint rather than heavy blur on every element.
- Background readability protection now applies a static black contrast scrim driven by the brightness setting. This is preferable to only lowering text alpha or adding animated blur because it preserves contrast at almost zero ongoing cost.
- Reworked the theme-derived background into a clearly visible static multi-glow field (linear + radial theme-color glows). No perpetual animation/timer was added.
- The theme-derived ambient background is also present behind the global navigation shell, so glass surfaces outside the timetable page have visual depth instead of appearing as ordinary translucent black cards.
- Removed remaining user-content 2/3-line limits in course/watch/detail/event surfaces; About capsules already use unlimited lines. Detail marquee was removed so long content wraps instead of endlessly moving.
- Removed three unnecessary `AnimatedContent` header transitions from edit screens. Navigation continues to use `SwipeDismissableNavHost`; UI-state `AnimatedContent` intentionally has `EnterTransition.None/ExitTransition.None`; pull-date rebound and rotary snap scrolling remain because they are interaction feedback rather than decorative page transitions.
- Rotary/virtual-bezel paths were reviewed. Transforming lists share their state with `ScreenScaffold`; the horizontal date picker explicitly requests focus and uses Wear `rotaryScrollable` + `RotaryScrollableDefaults.snapBehavior`. No extra pointer-only bezel implementation was added that would compete with Samsung's rotary event delivery.

## Performance decision
The original implementation applied vibrancy + blur + lens to every visible course card. On a watch, that scales with the number of visible cards and is the main reason Liquid Glass can stutter. The revised design uses shader-free translucent glass for ordinary surfaces and reserves AGSL refraction for the current/next course cards. The advanced Fluid profile increases lens strength but does not multiply the number of shader nodes.

## Static review
- `strings.xml` parses successfully for all Wear locales; no duplicate string keys introduced.
- No stale wrong `RectangleShape` import or external access to private `selectedTimeSlot` was found.
- Remaining one-line text limits in `CourseCard` are fixed-format status/time labels, not arbitrary user-entered content.
- Production `while(true)` loops remain suspension-based course-boundary/pointer-event loops, not CPU busy loops.
- Existing Data Layer `runBlocking`/legacy blocking bridges remain technical debt but were not changed in this visual pass because changing service lifetime semantics without instrumentation would be riskier than leaving them isolated on their existing IO paths.

## Build verification limitation
Attempted `./gradlew --no-daemon :wear:compileDebugKotlin :shared:test :mobile:compileDebugKotlin`. The sandbox still cannot resolve `services.gradle.org`, so Gradle 9.4.1 cannot be downloaded and Kotlin compilation cannot start here. CI should be used for the final compiler check.
