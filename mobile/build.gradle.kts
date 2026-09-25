import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
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
    namespace = "com.hufeng943.timetable"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.hufeng943.timetable"
        minSdk = 30
        targetSdk = 36
        versionCode = 3050400
        versionName = "3.5.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            // Keep the phone and watch release APKs signed with the same certificate.
            // This is required for reliable Wear Data Layer app pairing.
            // Production release signing is injected at build time; no keystore or password lives in source control.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(project(":shared"))
    ksp(libs.room.compiler)
}
