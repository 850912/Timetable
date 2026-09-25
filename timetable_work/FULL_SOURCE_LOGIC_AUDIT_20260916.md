# Full source logic audit — 2026-09-16

Scope: mobile, wear and shared Kotlin sources, manifests, Room schema/migrations, schedule adjustment/day arrangement/quick tools, sync, reminder/widget/tile/complication, and Liquid Glass integration.

## Fixes applied
- Schedule adjustment mutations are now atomic Room transactions. Permanent SWAP no longer commits A and B as separate transactions.
- Schedule-adjustment undo journal moved to Room (`schedule_adjustment_history`, DB v11). Mutation + undo snapshot are committed in the same transaction; restore + journal clear are also atomic.
- Legacy SharedPreferences undo data remains read-only-compatible for one-time restoration; new operations no longer write there.
- Quick SHIFT business layer now enforces -30..30 minutes and rejects zero, so callers cannot bypass the UI constraint.
- Removed unused `WearDataLayerTransferService`; the registered `WearOsSyncReceiverService` remains the single Wear Data Layer listener.
- Removed the unnecessary infinite marquee from the settings top-time description.
- Added migration 10→11 to both phone and watch DB builders and updated migration instrumentation tests/schema asset.

## Re-audit results
- 200 Kotlin source/test files after the change (two Room files added, one dead service removed).
- No empty catch blocks or `GlobalScope` found.
- The two `while(true)` loops inspected are suspending event/time-boundary loops, not CPU busy loops.
- Liquid Glass remains presentation-only and API-gated; no schedule/sync domain coupling found.
- Data Layer sync retains request-id idempotency, revision conflict ordering and tombstone handling.
- Reminder/widget/tile paths show no obvious self-rescheduling tight loop.

## Residual non-blocking debt
- Several Data Layer/receiver paths still bridge to suspend APIs with `runBlocking`. They are not UI-thread Compose paths, but should eventually move to lifecycle-owned coroutine scopes / async receiver completion to reduce blocking risk.
- Some Wear UI strings remain hard-coded Chinese; this is localization debt, not schedule correctness.
- Detail screens intentionally retain marquee for overflow text.

## Validation limitation
Attempted `:shared:test :wear:compileDebugKotlin :mobile:compileDebugKotlin --offline --no-daemon`. The Gradle wrapper still tries to obtain Gradle 9.4.1 and fails DNS resolution for services.gradle.org before source compilation begins. Therefore this audit does not claim a successful Kotlin/Room compile. Static cross-reference and migration wiring checks were completed.
