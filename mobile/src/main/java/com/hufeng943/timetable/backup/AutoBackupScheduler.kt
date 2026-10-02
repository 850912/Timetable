package com.hufeng943.timetable.backup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hufeng943.timetable.TimetableDatabaseProvider
import com.hufeng943.timetable.shared.export.BackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AutoBackupScheduler {
    private const val PREFS = "automatic_backups"
    private const val ENABLED = "enabled"
    private const val REQUEST_CODE = 94501
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    fun enabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, true)

    fun setEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, value).apply()
        if (value) schedule(context) else cancel(context)
    }

    fun schedule(context: Context) {
        if (!enabled(context)) return
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context)
        val first = System.currentTimeMillis() + DAY_MILLIS
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, DAY_MILLIS, pending)
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, AutoBackupReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun backupDirectory(context: Context): File = context.getExternalFilesDir("backups")
        ?.also { it.mkdirs() } ?: File(context.filesDir, "backups").also { it.mkdirs() }
}

class AutoBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!AutoBackupScheduler.enabled(context)) return
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AutoBackupScheduler.schedule(context)
            return
        }
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val tables = TimetableDatabaseProvider.repository(context).getAllTimetables().first()
                if (tables.isNotEmpty()) {
                    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                    val output = File(AutoBackupScheduler.backupDirectory(context), "timetable-auto-$stamp.json")
                    output.outputStream().use { BackupManager.backup(it, tables) }
                    output.parentFile?.listFiles()
                        ?.filter { it.name.startsWith("timetable-auto-") && it.extension == "json" }
                        ?.sortedByDescending(File::lastModified)
                        ?.drop(7)
                        ?.forEach(File::delete)
                }
            } finally { pending.finish() }
        }
    }
}
