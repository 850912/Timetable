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
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.MainActivity
import java.text.DateFormat
import java.util.Date

class WearCourseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_COURSE_REMINDER) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val name = intent.getStringExtra(EXTRA_COURSE_NAME).orEmpty().ifBlank {
            context.getString(R.string.tile_unnamed_course)
        }
        val location = intent.getStringExtra(EXTRA_LOCATION).orEmpty()
        val startEpoch = intent.getLongExtra(EXTRA_START_EPOCH, 0L)
        val offset = intent.getIntExtra(EXTRA_OFFSET_MINUTES, 15)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                description = context.getString(R.string.reminder_channel_description)
            }
        )

        val openIntent = PendingIntent.getActivity(
            context,
            7001,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val startText = if (startEpoch > 0L) DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(startEpoch)) else ""
        val detail = buildString {
            append(context.getString(R.string.reminder_body, offset, startText))
            if (location.isNotBlank()) append(" · ").append(location)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(name)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0L, 180L, 90L, 180L))
            .setContentIntent(openIntent)
            .addAction(0, context.getString(R.string.reminder_open_action), openIntent)
            .build()
        manager.notify((startEpoch xor name.hashCode().toLong()).hashCode(), notification)

        WearCourseReminderScheduler.rescheduleAsync(context)
    }

    companion object {
        const val ACTION_COURSE_REMINDER = "com.hufeng943.timetable.action.COURSE_REMINDER"
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_LOCATION = "location"
        const val EXTRA_START_EPOCH = "start_epoch"
        const val EXTRA_OFFSET_MINUTES = "offset_minutes"
        private const val CHANNEL_ID = "wear_course_reminders"
    }
}
