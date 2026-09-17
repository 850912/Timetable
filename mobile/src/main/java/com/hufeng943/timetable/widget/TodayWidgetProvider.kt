package com.hufeng943.timetable.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.widget.RemoteViews
import com.hufeng943.timetable.MainActivity
import com.hufeng943.timetable.R
import com.hufeng943.timetable.TimetableDatabaseProvider
import com.hufeng943.timetable.shared.model.resolveDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val text = buildWidgetText(context)
            val openAppIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            ids.forEach { id ->
                manager.updateAppWidget(id, RemoteViews(context.packageName, R.layout.widget_today).apply {
                    setTextViewText(R.id.widgetCourses, text)
                    setOnClickPendingIntent(R.id.widgetRoot, openAppIntent)
                })
            }
            pending.finish()
        }
    }

    private suspend fun buildWidgetText(context: Context): String {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
        return runCatching {
            val snapshot = TimetableDatabaseProvider.repository(context).getAllTimetables().first()
            snapshot.flatMap { table ->
                table.resolveDate(today).map { occurrence ->
                    WidgetCourse(
                        start = occurrence.startTime,
                        end = occurrence.endTime,
                        title = occurrence.course.name,
                        location = occurrence.location.orEmpty(),
                    )
                }
            }
                .sortedWith(compareBy<WidgetCourse> { it.end?.let { end -> end <= now } ?: false }.thenBy { it.start })
                .take(5)
                .joinToString("\n") { it.toLine() }
                .ifBlank { "今天没有课程" }
        }.getOrElse { "打开 Timetable 查看今日课程" }
    }

    companion object {
        fun requestRefresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, TodayWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
            )
        }
    }

    private data class WidgetCourse(
        val start: LocalTime,
        val end: LocalTime?,
        val title: String,
        val location: String
    ) {
        fun toLine(): String {
            val time = if (end != null) "$start-$end" else start.toString()
            val place = location.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
            return "$time  $title$place"
        }
    }
}
