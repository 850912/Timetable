package com.hufeng943.timetable.probe

import android.content.Context
import android.os.Build
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        appendLine("设备：${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android：${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("GMS：${gmsInfo(context)}")

        val nodeClient = runCatching { Wearable.getNodeClient(context) }
        appendLine("Wearable NodeClient：${if (nodeClient.isSuccess) "可创建" else "创建失败 ${nodeClient.exceptionOrNull()?.short()}"}")
        if (nodeClient.isSuccess) {
            val client = nodeClient.getOrThrow()
            val local = runCatching { Tasks.await(client.localNode) }
            appendLine("localNode：${local.fold({ "OK ${it.displayName} / ${it.id}" }, { "FAIL ${it.short()}" })}")
            val connected = runCatching { Tasks.await(client.connectedNodes) }
            appendLine("connectedNodes：${connected.fold({ nodes -> "OK ${nodes.size} 个" + nodes.joinToString(prefix = if (nodes.isEmpty()) "" else "\n  ", separator = "\n  ") { "${it.displayName} / ${it.id} nearby=${it.isNearby}" } }, { "FAIL ${it.short()}" })}")
        }

        val dataClient = runCatching { Wearable.getDataClient(context) }
        appendLine("DataClient：${if (dataClient.isSuccess) "可创建" else "创建失败 ${dataClient.exceptionOrNull()?.short()}"}")
        val messageClient = runCatching { Wearable.getMessageClient(context) }
        appendLine("MessageClient：${if (messageClient.isSuccess) "可创建" else "创建失败 ${messageClient.exceptionOrNull()?.short()}"}")
        appendLine("最近接收：${lastEvent(context)}")
    }

    fun sendMessage(context: Context): String {
        val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
        if (nodes.isEmpty()) return "失败：connectedNodes=0"
        val payload = "PHONE_PROBE|${System.currentTimeMillis()}|${Build.MODEL}".toByteArray()
        val results = nodes.map { node ->
            runCatching {
                Tasks.await(Wearable.getMessageClient(context).sendMessage(node.id, MESSAGE_PATH, payload))
                "${node.displayName}: OK"
            }.getOrElse { "${node.displayName}: FAIL ${it.short()}" }
        }
        return results.joinToString("\n")
    }

    fun sendDataItem(context: Context): String {
        val request = PutDataMapRequest.create(DATA_PATH).apply {
            dataMap.putLong("timestamp", System.currentTimeMillis())
            dataMap.putString("from", "phone")
            dataMap.putString("model", Build.MODEL)
        }.asPutDataRequest().setUrgent()
        val item = Tasks.await(Wearable.getDataClient(context).putDataItem(request))
        return "OK：${item.uri}"
    }

    private fun nowText(): String = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    private fun Throwable.short(): String = "${javaClass.simpleName}: ${message ?: "无详细信息"}"
}
