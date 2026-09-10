package com.hufeng943.timetable.surface

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.hufeng943.timetable.complication.CurrentCourseComplicationService
import com.hufeng943.timetable.complication.MainComplicationService
import com.hufeng943.timetable.complication.NextCourseComplicationService
import com.hufeng943.timetable.tile.MainTileService

/** Refreshes system-rendered Wear surfaces after local timetable data changes. */
object WearSurfaceRefresher {
    fun refresh(context: Context) {
        runCatching {
            TileService.getUpdater(context.applicationContext)
                .requestUpdate(MainTileService::class.java)
        }
        listOf(
            MainComplicationService::class.java,
            CurrentCourseComplicationService::class.java,
            NextCourseComplicationService::class.java,
        ).forEach { service ->
            runCatching {
                ComplicationDataSourceUpdateRequester.create(
                    context.applicationContext,
                    ComponentName(context.applicationContext, service),
                ).requestUpdateAll()
            }
        }
    }
}
