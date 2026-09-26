package com.hufeng943.timetable.sync

import android.content.Context

/**
 * Runtime environment probe.
 *
 * Do not use Build.MANUFACTURER here:
 * the same watch model may exist with different regional ROMs.
 */
object WearEnvironmentDetector {
    fun isChinaCompatible(context: Context): Boolean {
        return !TransportSelector.isGoogleWearAvailable(context)
    }
}
