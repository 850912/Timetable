# Haze / Android 37.2 build fix — 2026-09-17

## Failure verified from GitHub Actions

The CI build reached `:wear:checkDebugAarMetadata` and failed before Kotlin compilation.
`haze`, `haze-blur`, `haze-glass`, and `haze-utils` 2.0.0-rc01 require compile SDK 37.2 or newer, while the project declared compile SDK 37.0.

## Fix

Updated `compileSdk` in `shared`, `mobile`, and `wear` from integer API 37 to the AGP minor-SDK DSL:

```kotlin
compileSdk {
    version = release(37) {
        minorApiLevel = 2
    }
}
```

`targetSdk` and `minSdk` are intentionally unchanged. Updating compile SDK does not opt devices into new runtime behavior.

The project already uses AGP 9.2.1, which supports the expanded `CompileSdkSpec` DSL.

## Why all three modules

Only Wear currently consumes Haze directly, but all Android modules are aligned to one compile SDK to avoid cross-module AAR metadata and source compatibility drift.

## CI note

GitHub-hosted command-line builds can let Gradle download a missing SDK platform when the corresponding Android SDK licenses are accepted on the runner. If a future run reports that Android 37.2 itself is missing, add an explicit SDK installation step in CI; do not downgrade Haze merely to hide the AAR metadata requirement.
