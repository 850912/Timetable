package com.hufeng943.timetable.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hufeng943.timetable.MainActivity
import com.hufeng943.timetable.R
import com.hufeng943.timetable.TimetableDatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class CourseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val course = intent.getStringExtra(EXTRA_COURSE).orEmpty().ifBlank { "课程" }
        val location = intent.getStringExtra(EXTRA_LOCATION)
        val start = intent.getStringExtra(EXTRA_START).orEmpty()
        val end = intent.getStringExtra(EXTRA_END).orEmpty()
        val offset = intent.getIntExtra(EXTRA_OFFSET, 10)
        val eventKey = intent.getIntExtra(EXTRA_EVENT_KEY, course.hashCode() * 31 + start.hashCode())
        val actionType = intent.getStringExtra(EXTRA_ACTION_TYPE) ?: ACTION_PRE_START
        val manager = context.getSystemService(NotificationManager::class.java)

        if (actionType == ACTION_REFRESH) {
            refreshScheduleAsync(context)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "课程提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "课前提醒和上课中的持续状态"
                }
            )
        }
        if (actionType == ACTION_END) {
            manager.cancel(eventKey)
            refreshScheduleAsync(context)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val detail = buildString {
            append("$start–$end")
            if (!location.isNullOrBlank()) append(" · $location")
        }
        val inProgress = actionType == ACTION_START
        val title = if (inProgress) "正在上课 · $course" else if (offset > 0) "$offset 分钟后上课 · $course" else "即将上课 · $course"
        manager.notify(
            eventKey,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_course_notification)
                .setContentTitle(title)
                .setContentText(detail)
                .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setOnlyAlertOnce(inProgress)
                .setOngoing(inProgress)
                .setAutoCancel(!inProgress)
                .setContentIntent(openApp)
                .build()
        )
    }

    private fun refreshScheduleAsync(context: Context) {
        val pending = goAsync()
        Thread {
            try {
                val tables = runBlocking(Dispatchers.IO) {
                    TimetableDatabaseProvider.repository(context).getAllTimetables().first()
                }
                CourseReminderScheduler.schedule(context, tables)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val EXTRA_COURSE = "course"
        const val EXTRA_LOCATION = "location"
        const val EXTRA_START = "start"
        const val EXTRA_END = "end"
        const val EXTRA_OFFSET = "offset"
        const val EXTRA_EVENT_KEY = "event_key"
        const val EXTRA_ACTION_TYPE = "action_type"
        const val ACTION_PRE_START = "pre_start"
        const val ACTION_START = "start"
        const val ACTION_END = "end"
        const val ACTION_REFRESH = "refresh"
        private const val CHANNEL_ID = "course_reminders"

        fun cancelAllCourseNotifications(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.activeNotifications
                .filter { it.notification.channelId == CHANNEL_ID }
                .forEach { manager.cancel(it.id) }
        }
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                val tables = runBlocking(Dispatchers.IO) {
                    TimetableDatabaseProvider.repository(context).getAllTimetables().first()
                }
                CourseReminderScheduler.schedule(context, tables)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
