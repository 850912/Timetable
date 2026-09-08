# Build 9.1 build fix

## Failure reproduced from GitHub Actions log

Gradle failed while configuring `legacyprobe-mobile` with:

`Error resolving plugin [id: org.jetbrains.kotlin.android, version: 2.4.0]`

`The request for this plugin could not be satisfied because the plugin is already on the classpath with an unknown version.`

## Root cause

This project uses Android Gradle Plugin 9.2.1. AGP 9.x enables built-in Kotlin support for Android application/library modules, so applying `org.jetbrains.kotlin.android` again in the two new legacy probe modules conflicts with AGP's built-in Kotlin runtime.

The existing `mobile` and `wear` modules already follow the AGP 9 pattern: they apply `com.android.application` without applying `org.jetbrains.kotlin.android`.

## Fix

Removed `alias(libs.plugins.kotlin.android)` from:

- `legacyprobe-mobile/build.gradle.kts`
- `legacyprobe-wear/build.gradle.kts`

Also removed the now-unused `kotlin-android` plugin alias from `gradle/libs.versions.toml` so it is not accidentally reintroduced.

No changes were made to the legacy probe transport itself: both probe modules still use:

`com.google.android.gms:play-services-wearable:10.2.0`

with `GoogleApiClient`, `Wearable.NodeApi`, `Wearable.MessageApi`, and `Wearable.DataApi`.

## Validation

- Confirmed there are no remaining `org.jetbrains.kotlin.android` / `libs.plugins.kotlin.android` applications in Android modules.
- Parsed both legacy probe AndroidManifest.xml files successfully.
- Full Gradle compilation cannot be executed in the local sandbox because the Gradle wrapper must download Gradle 9.4.1 from services.gradle.org and the sandbox cannot resolve that host. GitHub Actions should be used for the definitive compile result.
