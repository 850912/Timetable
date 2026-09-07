package com.hufeng943.timetable.sync

import android.content.Context
import com.hufeng943.timetable.shared.sync.SyncTransport

object SyncTransportProvider {
    fun create(context: Context): List<SyncTransport> {
        return listOf(
            WearOsTransport(context)
        )
    }
}
