package com.hufeng943.timetable

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.hufeng943.timetable.sync.AutoSyncJobService

class TimetableMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        AutoSyncJobService.schedulePeriodic(this)
    }
}
