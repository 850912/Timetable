package com.hufeng943.timetable.calendar

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.resolveDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import java.time.ZoneId

object SystemCalendarSync {
    data class Result(val inserted: Int, val calendarName: String)

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun sync(context: Context, timetable: Timetable): Result {
        check(hasPermission(context)) { "缺少系统日历权限" }
        val resolver = context.contentResolver
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val calendar = resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL}>=? AND ${CalendarContract.Calendars.VISIBLE}=1",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars._ID} ASC",
        )?.use { cursor ->
            if (!cursor.moveToFirst()) null else cursor.getLong(0) to cursor.getString(1)
        } ?: error("没有可写入的系统日历")

        val marker = "[Timetable:${timetable.timetableId}]"
        resolver.delete(
            CalendarContract.Events.CONTENT_URI,
            "${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf("$marker%"),
        )

        val start = timetable.semesterStart
        val end = timetable.semesterEnd ?: start.plus(180, DateTimeUnit.DAY)
        val maxEnd = start.plus(370, DateTimeUnit.DAY)
        val effectiveEnd = if (end > maxEnd) maxEnd else end
        val zone = ZoneId.systemDefault()
        var date = start
        var inserted = 0
        while (date <= effectiveEnd) {
            timetable.resolveDate(date).forEach { occurrence ->
                val jDate = java.time.LocalDate.parse(date.toString())
                val startMillis = jDate.atTime(occurrence.startTime.hour, occurrence.startTime.minute)
                    .atZone(zone).toInstant().toEpochMilli()
                var endDate = jDate
                if (occurrence.endTime <= occurrence.startTime) endDate = endDate.plusDays(1)
                val endMillis = endDate.atTime(occurrence.endTime.hour, occurrence.endTime.minute)
                    .atZone(zone).toInstant().toEpochMilli()
                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendar.first)
                    put(CalendarContract.Events.TITLE, occurrence.course.name.ifBlank { "课程" })
                    put(CalendarContract.Events.EVENT_LOCATION, occurrence.location)
                    put(CalendarContract.Events.DESCRIPTION, "$marker ${occurrence.course.teacher.orEmpty()} ${occurrence.override?.remark.orEmpty()}".trim())
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
                    put(CalendarContract.Events.HAS_ALARM, 0)
                }
                resolver.insert(CalendarContract.Events.CONTENT_URI, values)?.let { inserted++ }
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }
        return Result(inserted, calendar.second)
    }
}
