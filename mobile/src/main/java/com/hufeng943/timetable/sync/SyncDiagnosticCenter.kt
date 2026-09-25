package com.hufeng943.timetable.sync

import android.content.Context
import android.os.Build

/** Build16 diagnostic center: capability + recent events + clear/export-ready plain text. */
object SyncDiagnosticCenter {
    fun capability(context: Context): WearTransportCapability {
        val google = TransportSelector.isGoogleWearAvailable(context)
        val bluetoothEnabled = ChinaWearBleCapabilities.isBluetoothEnabled(context)
        val chinaHardware = ChinaWearBleCapabilities.isHardwareSupported(context)
        val chinaPermission = ChinaWearBleCapabilities.hasRuntimePermission(context)
        val china = ChinaWearBleCapabilities.isSupported(context)
        val selected = SyncTransportProvider.create(context).firstOrNull()?.name ?: "None"
        val reason = when {
            google -> "Google Play services 环境可用；优先使用 Wear Data Layer"
            !chinaHardware -> "Google 不可用，且设备不支持 BLE"
            !bluetoothEnabled -> "Google 不可用，但蓝牙未开启"
            !chinaPermission -> "Google 不可用，但 BLE 运行时权限未授予"
            china -> "Google 不可用，使用 China Wear BLE 兼容通道"
            else -> "未找到可用同步通道"
        }
        return WearTransportCapability(
            googleDataLayer = google,
            chinaBleSupported = chinaHardware,
            chinaBlePermissionGranted = chinaPermission,
            bluetoothEnabled = bluetoothEnabled,
            selected = selected,
            reason = reason,
        )
    }

    fun diagnosticText(context: Context): String {
        val c = capability(context)
        val events = SyncDiagnosticStore.latest(context)
        return buildString {
            appendLine("Timetable 同步诊断")
            appendLine("系统: Android ${Build.VERSION.SDK_INT}")
            appendLine("Google Data Layer: ${if (c.googleDataLayer) "可用" else "不可用"}")
            appendLine("China BLE 硬件: ${if (c.chinaBleSupported) "支持" else "不支持"}")
            appendLine("China BLE 权限: ${if (c.chinaBlePermissionGranted) "已授予" else "未授予"}")
            appendLine("蓝牙: ${if (c.bluetoothEnabled) "已开启" else "未开启"}")
            appendLine("当前 Transport: ${c.selected}")
            appendLine("判定: ${c.reason}")
            appendLine()
            appendLine("最近事件（最多 20 条）:")
            if (events.isEmpty()) appendLine("无")
            else events.forEach { event -> appendLine("${event.timestamp} | ${event.type} | ${event.message}") }
        }.trimEnd()
    }

    fun snapshotWithCapability(context: Context, pending: Int, failed: Int, connectionText: String): String {
        val c = capability(context)
        val recent = SyncDiagnosticStore.latest(context).takeLast(6)
        return buildString {
            append("手表连接：$connectionText\n")
            append("待同步变更：$pending\n")
            append("失败变更：$failed\n")
            append("Google Data Layer：${if (c.googleDataLayer) "可用" else "不可用"}\n")
            append("China BLE：${when { !c.chinaBleSupported -> "不支持"; !c.chinaBlePermissionGranted -> "缺少权限"; !c.bluetoothEnabled -> "蓝牙关闭"; else -> "可用" }}\n")
            append("当前 Transport：${c.selected}\n")
            append("判定：${c.reason}\n\n")
            val latest = SyncDiagnosticStore.latest(context).lastOrNull()
            if (latest != null) append("最近状态：${latest.type} · ${latest.message}\n\n")
            append("最近诊断：\n")
            if (recent.isEmpty()) append("暂无事件")
            else recent.forEach { append("${it.type} · ${it.message}\n") }
        }.trimEnd()
    }

    fun clear(context: Context) {
        context.getSharedPreferences("sync_diagnostic_store", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    fun snapshot(context: Context): String = diagnosticText(context)
}
