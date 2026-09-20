plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hufeng943.timetable.watchface.nacho"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hufeng943.timetable.watchface.nacho"
        minSdk = 33
        targetSdk = 36
        versionCode = 3050300
        versionName = "3.5.3"
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
