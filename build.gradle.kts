// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.hilt.android) apply false
}

// A WFF watch face is a separate resource-only APK, so a complete Wear distribution
// must build/install both the Wear app and the watch-face package.
tasks.register("assembleWearReleaseDistribution") {
    group = "build"
    description = "Builds the signed Wear app and Watch Face Format release APKs."
    dependsOn(":wear:assembleRelease", ":watchface-nacho:assembleRelease")
}

tasks.register("installWearDebug") {
    group = "install"
    description = "Installs both the Wear app and the WFF watch face on the connected watch."
    dependsOn(":wear:installDebug", ":watchface-nacho:installDebug")
}
