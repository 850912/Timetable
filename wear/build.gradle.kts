import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.aboutLibraries)
}

val versionPrefix = "3.5.3"
val releaseVersionCode = 3050300

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

val isRelease = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}

if (isRelease && !releaseSigningConfigured) {
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
        minSdk = 28
        targetSdk = 37

        versionCode = if (isRelease) releaseVersionCode else 1
        versionName = if (isRelease) versionPrefix else "$versionPrefix-dev"
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
            applicationIdSuffix = ".debug"
        }
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            // Production release signing is injected at build time; no keystore or password lives in source control.
        }
    }

    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }

    // APK 打包稳定性配置：
    // 保留原有 ABI / UI / 功能，仅规范资源打包。
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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
    implementation("io.github.kyant0:backdrop:2.0.1")
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
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.wear.compose.navigation3)
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
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
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

    // Apache-2.0: Kyant AndroidLiquidGlass / Backdrop, matching the reference app renderer.
}
