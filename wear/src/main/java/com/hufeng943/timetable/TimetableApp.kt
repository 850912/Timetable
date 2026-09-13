package com.hufeng943.timetable

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.hufeng943.timetable.sync.WearBridgeBootstrap
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TimetableApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Data Layer bootstrap touches Google Play services and can contend with the
        // first Compose/Room frame on a watch. Keep the China compatibility bootstrap,
        // but let the launcher render first.
        Handler(Looper.getMainLooper()).postDelayed(
            { WearBridgeBootstrap.start(this) },
            2_500L,
        )
    }
}
