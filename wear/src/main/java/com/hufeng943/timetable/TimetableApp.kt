package com.hufeng943.timetable

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.google.android.gms.common.GoogleApiAvailability
import com.hufeng943.timetable.sync.WearBridgeBootstrap
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TimetableApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Data Layer bootstrap touches Google Play services and can contend with the
        // first Compose/Room frame on a watch. Keep the China compatibility bootstrap,
        // but let Compose/Room finish the cold-start path first.
        val googleServicesAvailable = runCatching {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == 0
        }.getOrDefault(false)
        if (googleServicesAvailable) {
            Handler(Looper.getMainLooper()).postDelayed(
                { WearBridgeBootstrap.start(this) },
                7_000L,
            )
        }
    }
}
