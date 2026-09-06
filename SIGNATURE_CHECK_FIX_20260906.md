# APK signature verification fix — 2026-09-06

GitHub Actions previously parsed only this apksigner format:

`Signer #1 certificate SHA-256 digest: ...`

Current Android build-tools output is instead prefixed like:

`V2 Signer: certificate SHA-256 digest: ...`

The APKs were valid and all used the same certificate, but the parser returned an empty digest and failed the workflow.

The workflow now matches any signer prefix ending in `certificate SHA-256 digest:` and compares every Wear release APK against the Mobile release APK.
