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

        if (actionType == ACTION_ACADEMIC_EVENT) {
            showAcademicEventNotification(context, intent, manager, eventKey, offset)
            return
        }

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

    private fun showAcademicEventNotification(
        context: Context,
        intent: Intent,
        manager: NotificationManager,
        eventKey: Int,
        offset: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(EVENT_CHANNEL_ID, "作业与考试", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "作业、考试、实验和其他学业事件提醒"
                }
            )
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val title = intent.getStringExtra(EXTRA_EVENT_TITLE).orEmpty().ifBlank { "学业待办" }
        val course = intent.getStringExtra(EXTRA_EVENT_COURSE)
        val location = intent.getStringExtra(EXTRA_EVENT_LOCATION)
        val date = intent.getStringExtra(EXTRA_EVENT_DATE).orEmpty()
        val time = intent.getStringExtra(EXTRA_EVENT_TIME)
        val typeLabel = when (intent.getStringExtra(EXTRA_EVENT_TYPE)) {
            "ASSIGNMENT" -> "作业"
            "EXAM" -> "考试"
            "LAB" -> "实验"
            else -> "待办"
        }
        val detail = buildString {
            append(typeLabel)
            if (!course.isNullOrBlank()) append(" · $course")
            if (date.isNotBlank()) append(" · $date")
            if (!time.isNullOrBlank()) append(" $time")
            if (!location.isNullOrBlank()) append(" · $location")
        }
        val contentTitle = if (offset > 0) "${formatOffset(offset)}后 · $title" else title
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.notify(
            eventKey,
            NotificationCompat.Builder(context, EVENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_course_notification)
                .setContentTitle(contentTitle)
                .setContentText(detail)
                .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build()
        )
    }


    private fun formatOffset(minutes: Int): String = when {
        minutes >= 1440 && minutes % 1440 == 0 -> "${minutes / 1440} 天"
        minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60} 小时"
        else -> "$minutes 分钟"
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
        const val EXTRA_EVENT_TITLE = "academic_event_title"
        const val EXTRA_EVENT_COURSE = "academic_event_course"
        const val EXTRA_EVENT_LOCATION = "academic_event_location"
        const val EXTRA_EVENT_DATE = "academic_event_date"
        const val EXTRA_EVENT_TIME = "academic_event_time"
        const val EXTRA_EVENT_TYPE = "academic_event_type"
        const val ACTION_PRE_START = "pre_start"
        const val ACTION_START = "start"
        const val ACTION_END = "end"
        const val ACTION_REFRESH = "refresh"
        const val ACTION_ACADEMIC_EVENT = "academic_event"
        private const val CHANNEL_ID = "course_reminders"
        private const val EVENT_CHANNEL_ID = "academic_event_reminders"

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
