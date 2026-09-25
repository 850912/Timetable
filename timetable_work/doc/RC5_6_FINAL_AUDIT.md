# RC5.6 final audit

Date: 2026-09-06

## Round 1 findings and fixes

1. Hard-delete DAO methods remained after soft-delete migration -> removed.
2. Wear ACK parser still used `!!` -> replaced with explicit null failure.
3. Empty pending-record sync could crash through `first()` -> request IDs no longer depend on a non-empty list.
4. Empty sync could not pull watch-side changes -> empty SyncEnvelope is now a real sync request.
5. Partial remote application could cause premature DataItem deletion -> DataItem is deleted only when all records are accepted.
6. Watch-side pending changes were not returned to phone -> ACK piggybacks pending watch records.
7. Phone must acknowledge watch-side records -> final ACK marks watch records synced.

## Round 2 verification

- No production `@Delete`, direct SQL hard-delete, or `!!` remains in shared/mobile/wear source.
- 5->6 migration is registered on both mobile and Wear.
- SQLite simulation of the 3->6 SQL shape passes and preserves the base tables.
- Stable sync IDs are generated for new entities and backfilled during migration.
- Parent references in sync payloads use sync IDs rather than local auto-increment IDs.
- LWW comparison is deterministic: revision -> updatedAt -> deviceId.
- Tombstones prevent an older update from resurrecting a deleted entity.
- Data Layer transfer uses an Asset and urgent DataItem; failed/partial application retains the DataItem for retry.

## Remaining build-time artifact

The repository currently contains exported Room schemas 1-3. Versions 4-6 are generated artifacts and should be committed after the first successful GitHub Gradle build. Runtime migration SQL has been independently simulated through 3->6. No fake identity hashes were added to source control.

## Build status

Release Gradle compilation was intentionally not claimed here because this environment cannot download Gradle 9.4.1. Run the project's GitHub Actions Release workflow for the authoritative compiler/KSP/R8 result.
