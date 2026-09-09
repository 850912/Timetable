package com.hufeng943.timetable.probe

import android.content.Context
import android.os.Build
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.hufeng943.timetable.sync.LegacyWearIo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object WearProbe {
    const val MESSAGE_PATH = "/timetable/probe/v1/message"
    const val ACK_PATH = "/timetable/probe/v1/ack"
    const val DATA_PATH = "/timetable/probe/v1/data"
    private const val PREFS = "wear_probe"

    fun saveEvent(context: Context, text: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("last_event", "${nowText()}  $text").apply()
    }

    fun lastEvent(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("last_event", "尚未收到探针事件") ?: "尚未收到探针事件"

    fun gmsInfo(context: Context): String = try {
        val info = context.packageManager.getPackageInfo("com.google.android.gms", 0)
        val versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        "已安装 · ${info.versionName ?: "?"} · code=$versionCode"
    } catch (t: Throwable) {
        "未安装/不可见 · ${t.javaClass.simpleName}"
    }

    fun runDiagnostics(context: Context): String = buildString {
        appendLine("Build 10.1 · 正式 App Legacy Probe")
        appendLine("角色：手表")
        appendLine("设备：${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android：${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("GMS：${gmsInfo(context)}")
        appendLine("接口：GoogleApiClient + Wearable.*Api")
        appendLine()
        append(
            runCatching {
                LegacyWearIo.withClient(context) { client ->
                    buildString {
                        appendLine("GoogleApiClient：CONNECTED")
                        appendLine("hasConnectedApi(Wearable.API)：${client.hasConnectedApi(Wearable.API)}")
                        val local = Wearable.NodeApi.getLocalNode(client).await(10, TimeUnit.SECONDS)
                        appendLine("localNode：status=${local.status.statusCode} success=${local.status.isSuccess}")
                        if (local.status.isSuccess) appendLine("  ${local.node.displayName} / ${local.node.id}")
                        val connected = Wearable.NodeApi.getConnectedNodes(client).await(10, TimeUnit.SECONDS)
                        appendLine("connectedNodes：status=${connected.status.statusCode} success=${connected.status.isSuccess} count=${connected.nodes.size}")
                        connected.nodes.forEach { appendLine("  ${it.displayName} / ${it.id} nearby=${it.isNearby}") }
                        appendLine("最近接收：${lastEvent(context)}")
                    }
                }
            }.getOrElse { "FAIL ${it.javaClass.simpleName}: ${it.message ?: "无详细信息"}" }
        )
    }

    fun sendMessage(context: Context): String = runCatching {
        LegacyWearIo.withClient(context) { client ->
            val nodes = Wearable.NodeApi.getConnectedNodes(client).await(10, TimeUnit.SECONDS)
            if (!nodes.status.isSuccess) return@withClient "NodeApi FAIL status=${nodes.status.statusCode}"
            if (nodes.nodes.isEmpty()) return@withClient "失败：connectedNodes=0"
            val payload = "WATCH_PROBE|${System.currentTimeMillis()}|${Build.MODEL}".toByteArray()
            nodes.nodes.joinToString("\n") { node ->
                val result = Wearable.MessageApi.sendMessage(client, node.id, MESSAGE_PATH, payload).await(10, TimeUnit.SECONDS)
                "${node.displayName}: status=${result.status.statusCode} success=${result.status.isSuccess} requestId=${result.requestId}"
            }
        }
    }.getOrElse { "FAIL ${it.javaClass.simpleName}: ${it.message ?: "无详细信息"}" }

    fun sendDataItem(context: Context): String = runCatching {
        LegacyWearIo.withClient(context) { client ->
            val request = PutDataMapRequest.create(DATA_PATH).apply {
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putString("from", "watch")
                dataMap.putString("model", Build.MODEL)
            }.asPutDataRequest().setUrgent()
            val result = Wearable.DataApi.putDataItem(client, request).await(10, TimeUnit.SECONDS)
            "DataApi.putDataItem status=${result.status.statusCode} success=${result.status.isSuccess} uri=${result.dataItem?.uri ?: "null"}"
        }
    }.getOrElse { "FAIL ${it.javaClass.simpleName}: ${it.message ?: "无详细信息"}" }

    private fun nowText(): String = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
}
