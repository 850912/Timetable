import com.android.build.api.dsl.ApplicationExtension

val versionPrefix = "2.0.0"

val commitCountProvider = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.map { output ->
    output.trim().toIntOrNull() ?: 1
}.orElse(1)

val isRelease = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.aboutLibraries)
}

val releaseStoreFile = rootProject.file("signing/timetable-release.jks")
val releaseStorePassword = "Timetable2026!"
val releaseKeyAlias = "timetable-release"
val releaseKeyPassword = "Timetable2026!"

configure<ApplicationExtension> {
    namespace = "com.hufeng943.timetable"
    compileSdk = 37


    defaultConfig {
        applicationId = "com.hufeng943.timetable"
        minSdk = 28
        targetSdk = 37

        versionCode = if (isRelease) commitCountProvider.get() else 1
        versionName = if (isRelease) {
            commitCountProvider.map { count -> "$versionPrefix ($count)" }.get()
        } else {
            "$versionPrefix-debug"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = releaseStoreFile
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            // Release signing is injected from TIMETABLE_RELEASE_* properties.
        }
    }

    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")
    // 核心基础与 AndroidX
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    implementation(project(":shared"))

    // Compose 基础体系
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // Wear OS 核心与 Compose Material 3
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.wear.remote)

    // Wear Tiles 与 Complications (Horologist)
    implementation(libs.androidx.tiles)
    implementation(libs.androidx.tiles.tooling.preview)
    implementation(libs.androidx.watchface.complications.data.source.ktx)
    implementation(libs.horologist.compose.tools)
    implementation(libs.horologist.tiles)

    // 依赖注入 (Hilt)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // 数据存储 (Room & DataStore)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.datastore.preferences)

    // KotlinX 扩展与辅助工具
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.aboutlibraries.compose.m3)

    implementation(libs.materialKolor)
}