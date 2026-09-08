import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val releaseStoreFile = rootProject.file("signing/timetable-release.jks")

configure<ApplicationExtension> {
    namespace = "com.hufeng943.timetable.legacyprobe"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hufeng943.timetable.legacyprobe"
        minSdk = 30
        targetSdk = 36
        versionCode = 9
        versionName = "9.0-china-legacy-probe"
    }

    signingConfigs {
        create("release") {
            storeFile = releaseStoreFile
            storePassword = "Timetable2026!"
            keyAlias = "timetable-release"
            keyPassword = "Timetable2026!"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
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
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-wearable:10.2.0")
}
