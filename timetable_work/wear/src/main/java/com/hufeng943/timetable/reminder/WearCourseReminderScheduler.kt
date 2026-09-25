package com.hufeng943.timetable.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.hilt.EntryPoint
import androidx.hilt.InstallIn
import androidx.hilt.android.EntryPointAccessors
import com.hufeng943.timetable.data.PreferenceStorage
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.model.NextCourseEngine
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderDependenciesEntryPoint {
    fun repository(): TimetableRepository
    fun preferenceStorage(): PreferenceStorage
}

object WearCourseReminderScheduler {
    private const val PREFS = "wear_course_reminder_alarms"
    private const val KEY_REQUEST_CODES = "request_codes"
    private const val LOOK_AHEAD_DAYS = 14
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun rescheduleAsync(context: Context) {
        val app = context.applicationContext
        scope.launch { runCatching { reschedule(app) } }
    }

    suspend fun reschedule(context: Context) {
        val app = context.applicationContext
        val entry = EntryPointAccessors.fromApplication(app, ReminderDependenciesEntryPoint::class.java)
        val offset = entry.preferenceStorage().appConfigFlow.first().courseReminderMinutes
        val alarmManager = app.getSystemService(AlarmManager::class.java)
        cancelTracked(app, alarmManager)
        if (offset == null) return

        val tables = entry.repository().getAllTimetables().first()
        if (tables.isEmpty()) return
        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(zone).date
        val nowMillis = now.toEpochMilliseconds()
        val requestCodes = linkedSetOf<Int>()

        for (dayOffset in 0..LOOK_AHEAD_DAYS) {
            val date = today.plus(dayOffset, DateTimeUnit.DAY)
            for (occurrence in NextCourseEngine.occurrencesForDate(tables, date, zone)) {
                val triggerAt = occurrence.startInstant.toEpochMilliseconds() - offset * 60_000L
                if (triggerAt <= nowMillis) continue
                val requestCode = stableRequestCode(occurrence.timeSlotId, triggerAt, offset)
                val intent = Intent(app, WearCourseReminderReceiver::class.java).apply {
                    action = WearCourseReminderReceiver.ACTION_COURSE_REMINDER
                    putExtra(WearCourseReminderReceiver.EXTRA_COURSE_NAME, occurrence.courseName)
                    putExtra(WearCourseReminderReceiver.EXTRA_LOCATION, occurrence.location)
                    putExtra(WearCourseReminderReceiver.EXTRA_START_EPOCH, occurrence.startInstant.toEpochMilliseconds())
                    putExtra(WearCourseReminderReceiver.EXTRA_OFFSET_MINUTES, offset)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    app,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                // No exact-alarm entitlement is required. This survives Doze with bounded timing
                // variance, which is appropriate for ordinary class reminders on a watch.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                requestCodes += requestCode
            }
        }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_REQUEST_CODES, requestCodes.map(Int::toString).toSet()).apply()
    }

    private fun cancelTracked(context: Context, alarmManager: AlarmManager) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val codes = prefs.getStringSet(KEY_REQUEST_CODES, emptySet()).orEmpty()
        for (raw in codes) {
            val code = raw.toIntOrNull() ?: continue
            val pending = PendingIntent.getBroadcast(
                context,
                code,
                Intent(context, WearCourseReminderReceiver::class.java).apply {
                    action = WearCourseReminderReceiver.ACTION_COURSE_REMINDER
                },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pending != null) alarmManager.cancel(pending)
        }
        prefs.edit().remove(KEY_REQUEST_CODES).apply()
    }

    private fun stableRequestCode(timeSlotId: Long, triggerAt: Long, offset: Int): Int {
        var hash = 17
        hash = 31 * hash + timeSlotId.hashCode()
        hash = 31 * hash + triggerAt.hashCode()
        hash = 31 * hash + offset
        return hash and Int.MAX_VALUE
    }
}

class WearReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                runCatching { WearCourseReminderScheduler.reschedule(app) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
