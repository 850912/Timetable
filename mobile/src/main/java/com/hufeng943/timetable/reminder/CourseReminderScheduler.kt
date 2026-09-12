package com.hufeng943.timetable.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.resolveDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import java.time.ZoneId

object CourseReminderScheduler {
    private const val DAYS_AHEAD = 14
    private const val MAINTENANCE_INTERVAL_MILLIS = 7L * 24 * 60 * 60 * 1000
    private const val MAINTENANCE_REQUEST_CODE = Int.MIN_VALUE

    fun schedule(context: Context, timetables: List<Timetable>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        cancelPreviouslyScheduled(context, alarmManager)
        if (!ReminderSettings.enabled(context)) {
            CourseReminderReceiver.cancelAllCourseNotifications(context)
            return
        }

        val offsetMinutes = ReminderSettings.offsetMinutes(context)
        val offsetMillis = offsetMinutes * 60_000L
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val now = System.currentTimeMillis()
        val requestCodes = mutableSetOf<Int>()

        repeat(DAYS_AHEAD + 1) { dayOffset ->
            val date = today.plus(dayOffset, DateTimeUnit.DAY)
            timetables.flatMap { it.resolveDate(date) }.forEach { occurrence ->
                val javaDate = java.time.LocalDate.parse(date.toString())
                val zone = ZoneId.systemDefault()
                val start = occurrence.startTime
                val end = occurrence.endTime
                val startAt = javaDate.atTime(start.hour, start.minute).atZone(zone).toInstant().toEpochMilli()
                val endDate = if (end <= start) javaDate.plusDays(1) else javaDate
                val endAt = endDate.atTime(end.hour, end.minute).atZone(zone).toInstant().toEpochMilli()
                val eventKey = eventKey(occurrence.timeSlot.id, date.toEpochDays())

                scheduleOne(
                    context, alarmManager, requestCodes,
                    requestCode(eventKey, 0), startAt - offsetMillis, now,
                    buildIntent(context, occurrence.course.name, occurrence.location, start.toString(), end.toString(), offsetMinutes, eventKey, CourseReminderReceiver.ACTION_PRE_START)
                )
                val startTrigger = if (startAt <= now && endAt > now) now + 1_000L else startAt
                scheduleOne(
                    context, alarmManager, requestCodes,
                    requestCode(eventKey, 1), startTrigger, now,
                    buildIntent(context, occurrence.course.name, occurrence.location, start.toString(), end.toString(), offsetMinutes, eventKey, CourseReminderReceiver.ACTION_START)
                )
                scheduleOne(
                    context, alarmManager, requestCodes,
                    requestCode(eventKey, 2), endAt, now,
                    buildIntent(context, occurrence.course.name, occurrence.location, start.toString(), end.toString(), offsetMinutes, eventKey, CourseReminderReceiver.ACTION_END)
                )
            }
        }
        // Keep the rolling window alive even across long holidays where no course END
        // alarm would otherwise run to refill the next 14 days.
        scheduleOne(
            context = context,
            manager = alarmManager,
            requestCodes = requestCodes,
            requestCode = MAINTENANCE_REQUEST_CODE,
            triggerAt = now + MAINTENANCE_INTERVAL_MILLIS,
            now = now,
            intent = Intent(context, CourseReminderReceiver::class.java).apply {
                putExtra(CourseReminderReceiver.EXTRA_ACTION_TYPE, CourseReminderReceiver.ACTION_REFRESH)
            },
            preferExact = false,
        )
        ReminderSettings.saveScheduledRequestCodes(context, requestCodes)
    }

    private fun buildIntent(
        context: Context,
        course: String,
        location: String?,
        start: String,
        end: String,
        offset: Int,
        eventKey: Int,
        actionType: String,
    ) = Intent(context, CourseReminderReceiver::class.java).apply {
        putExtra(CourseReminderReceiver.EXTRA_COURSE, course)
        putExtra(CourseReminderReceiver.EXTRA_LOCATION, location)
        putExtra(CourseReminderReceiver.EXTRA_START, start)
        putExtra(CourseReminderReceiver.EXTRA_END, end)
        putExtra(CourseReminderReceiver.EXTRA_OFFSET, offset)
        putExtra(CourseReminderReceiver.EXTRA_EVENT_KEY, eventKey)
        putExtra(CourseReminderReceiver.EXTRA_ACTION_TYPE, actionType)
    }

    private fun scheduleOne(
        context: Context,
        manager: AlarmManager,
        requestCodes: MutableSet<Int>,
        requestCode: Int,
        triggerAt: Long,
        now: Long,
        intent: Intent,
        preferExact: Boolean = true,
    ) {
        if (triggerAt <= now) return
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val canUseExact = preferExact && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms())
        if (canUseExact) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
        requestCodes += requestCode
    }

    private fun cancelPreviouslyScheduled(context: Context, manager: AlarmManager) {
        ReminderSettings.scheduledRequestCodes(context).forEach { code ->
            PendingIntent.getBroadcast(
                context, code, Intent(context, CourseReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )?.let {
                manager.cancel(it)
                it.cancel()
            }
        }
        ReminderSettings.saveScheduledRequestCodes(context, emptySet())
    }

    private fun eventKey(slotId: Long, epochDay: Int): Int =
        ((31L * slotId + epochDay.toLong()) xor (slotId ushr 32)).toInt()

    private fun requestCode(eventKey: Int, phase: Int): Int = (eventKey and 0x1FFF_FFFF) * 4 + phase
}
