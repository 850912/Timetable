# Timetable 3.1.0 Fix4 — Hybrid Data Layer review

## Scope

This fix reverts the global China Data Layer downgrade introduced by Fix3.

- `play-services-wearable` is restored to 20.0.1 for the production mobile/wear modules.
- The previously working selective legacy path remains: production sync may use `GoogleApiClient` + `Wearable.NodeApi/MessageApi/DataApi` where the project already used it for China compatibility.
- Modern APIs remain available where the existing app used them, notably Wear file-transfer asset handling.
- No database, backup schema, timetable payload, signing, or package identity changes are made.

## Build diagnostics

GitHub Actions now writes each Gradle invocation to `build-logs/*.log` using `tee`, enables `--stacktrace --warning-mode all`, preserves pipeline failure with `set -o pipefail`, and uploads the logs as `android-build-logs` when the workflow fails.

## Static checks

- Version catalog confirmed: `playServicesWearable = "20.0.1"`.
- Selective legacy call sites remain present in the same production paths as the pre-Fix3 source.
- Modern Wearable client call sites remain available where the pre-Fix3 source used them.
- Signing files were not edited.
