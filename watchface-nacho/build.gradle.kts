import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
}

fun releaseSecret(name: String): String? =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orNull
        ?.takeIf { it.isNotBlank() }

val releaseStoreFilePath = releaseSecret("TIMETABLE_RELEASE_STORE_FILE")
val releaseStorePassword = releaseSecret("TIMETABLE_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSecret("TIMETABLE_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSecret("TIMETABLE_RELEASE_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }
val releaseTaskRequested = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}

if (releaseTaskRequested && !releaseSigningConfigured) {
    throw GradleException(
        "Release signing is not configured. Set TIMETABLE_RELEASE_STORE_FILE, " +
            "TIMETABLE_RELEASE_STORE_PASSWORD, TIMETABLE_RELEASE_KEY_ALIAS and " +
            "TIMETABLE_RELEASE_KEY_PASSWORD via Gradle properties or environment variables."
    )
}

configure<ApplicationExtension> {
    namespace = "com.hufeng943.timetable.watchface.nacho"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hufeng943.timetable.watchface.nacho"
        minSdk = 33
        targetSdk = 36
        versionCode = 3050300
        versionName = "3.5.3"
    }

    signingConfigs {
        create("release") {
            if (releaseSigningConfigured) {
                storeFile = rootProject.file(requireNotNull(releaseStoreFilePath))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            // WFF packages are resource-only; R8 has no code to optimize.
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}
