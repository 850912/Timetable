plugins { alias(libs.plugins.android.application) }

android {
    namespace = "com.hufeng943.timetable.watchface.nacho"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.hufeng943.timetable.watchface.nacho"
        minSdk = 33
        targetSdk = 37
        versionCode = 3050100
        versionName = "3.5.1"
    }
}
