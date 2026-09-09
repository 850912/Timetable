package com.hufeng943.timetable

import android.app.Application
import com.hufeng943.timetable.sync.WearBridgeBootstrap
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TimetableApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WearBridgeBootstrap.start(this)
    }
}
