package com.hufeng943.timetable.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.hufeng943.timetable.R
import com.hufeng943.timetable.TimetableDatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val text = runCatching {
                val snapshot = TimetableDatabaseProvider.repository(context).getAllTimetables().first()
                snapshot.flatMap { table ->
                    val week = ((today.toEpochDays() - table.semesterStart.toEpochDays()) / 7 + 1).toInt().coerceAtLeast(1)
                    table.allCourses.flatMap { course ->
                        course.timeSlots.filter { it.dayOfWeek == today.dayOfWeek }.mapNotNull { slot ->
                            val ok = when (slot.recurrence.name) {
                                "ODD_WEEK" -> week % 2 == 1
                                "EVEN_WEEK" -> week % 2 == 0
                                else -> true
                            }
                            if (ok) "${slot.startTime ?: ""}  ${course.name}" else null
                        }
                    }
                }.sorted().take(3).joinToString("\n").ifBlank { "今天没有课程" }
            }.getOrElse { "打开 Timetable 查看今日课程" }
            ids.forEach { id ->
                manager.updateAppWidget(id, RemoteViews(context.packageName, R.layout.widget_today).apply {
                    setTextViewText(R.id.widgetCourses, text)
                })
            }
            pending.finish()
        }
    }
}
