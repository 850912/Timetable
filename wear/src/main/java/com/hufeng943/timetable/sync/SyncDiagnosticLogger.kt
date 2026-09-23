package com.hufeng943.timetable.sync

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.common.GoogleApiAvailability
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight Wear-side diagnostics for Build16.
 *
 * The Wear module intentionally keeps its own store because the mobile app and
 * watch do not share a process or a preferences file. No account or course
 * content is recorded here.
 */
object SyncDiagnosticLogger {
    private const val TAG = "TimetableSyncWear"
    private const val PREFS = "wear_sync_diagnostics"
    private const val KEY_EVENTS = "events"
    private const val MAX_EVENTS = 20

    fun record(context: Context, message: String) {
        val timestamp = System.currentTimeMillis()
        val line = "$timestamp|${message.replace('\n', ' ')}"
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val events = latestLines(context)
        prefs.edit().putString(KEY_EVENTS, (events + line).takeLast(MAX_EVENTS).joinToString("\n")).apply()
        Log.i(TAG, message)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_EVENTS).apply()
    }

    fun diagnosticText(context: Context): String {
        val google = runCatching {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == 0
        }.getOrDefault(false)
        val bluetoothManager = context.getSystemService(android.bluetooth.BluetoothManager::class.java)
        val bluetoothEnabled = runCatching { bluetoothManager?.adapter?.isEnabled == true }.getOrDefault(false)
        val bleHardware = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val permissions = if (Build.VERSION.SDK_INT >= 31) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            ).all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        } else {
            true
        }
        return buildString {
            appendLine("Timetable Wear 同步诊断")
            appendLine("Android API: ${Build.VERSION.SDK_INT}")
            appendLine("Google Play services: ${if (google) "可用" else "不可用"}")
            appendLine("BLE 硬件: ${if (bleHardware) "支持" else "不支持"}")
            appendLine("蓝牙: ${if (bluetoothEnabled) "已开启" else "未开启"}")
            appendLine("BLE 权限: ${if (permissions) "已授予" else "未授予"}")
            appendLine("当前通道: ${if (google) "Google Wear Data Layer" else "China Wear BLE"}")
            appendLine()
            appendLine("最近事件（最多 $MAX_EVENTS 条）:")
            val events = latest(context)
            if (events.isEmpty()) {
                appendLine("无")
            } else {
                events.forEach { appendLine(it) }
            }
        }.trimEnd()
    }

    fun copyToClipboard(context: Context): Boolean {
        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java) ?: return false
        return runCatching {
            clipboard.setPrimaryClip(ClipData.newPlainText("Timetable Wear 同步诊断", diagnosticText(context)))
            true
        }.getOrDefault(false)
    }

    fun latest(context: Context): List<String> = latestLines(context).mapNotNull { line ->
        val index = line.indexOf('|')
        if (index <= 0) return@mapNotNull null
        val timestamp = line.substring(0, index).toLongOrNull() ?: return@mapNotNull null
        val message = line.substring(index + 1)
        val formatted = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        "$formatted | $message"
    }

    private fun latestLines(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EVENTS, "")
            .orEmpty()
        return raw.lineSequence().filter { it.isNotBlank() }.takeLast(MAX_EVENTS).toList()
    }
}
