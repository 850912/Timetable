package com.hufeng943.timetable.legacyprobe

import android.content.Context
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object LegacyProbe {
    const val MESSAGE_PATH = "/timetable/probe/v2legacy/message"
    const val ACK_PATH = "/timetable/probe/v2legacy/ack"
    const val DATA_PATH = "/timetable/probe/v2legacy/data"
    private const val PREFS = "legacy_wear_probe"

    fun saveEvent(context: Context, text: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("last_event", "${nowText()}  $text").apply()
    }

    fun lastEvent(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("last_event", "尚未收到 Legacy 探针事件") ?: "尚未收到 Legacy 探针事件"

    fun gmsInfo(context: Context): String = try {
        val info = context.packageManager.getPackageInfo("com.google.android.gms", 0)
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        "已安装 · ${info.versionName ?: "?"} · code=$code"
    } catch (t: Throwable) {
        "未安装/不可见 · ${t.javaClass.simpleName}: ${t.message ?: ""}"
    }

    fun withClient(context: Context, block: (GoogleApiClient) -> String): String {
        val client = GoogleApiClient.Builder(context.applicationContext)
            .addApi(Wearable.API)
            .build()
        return try {
            val result = client.blockingConnect(12, TimeUnit.SECONDS)
            if (!result.isSuccess) {
                "CONNECT FAIL code=${result.errorCode} (${connectionName(result)}) message=${result.errorMessage ?: "null"} resolution=${result.hasResolution()}"
            } else {
                block(client)
            }
        } catch (t: Throwable) {
            "FAIL ${t.javaClass.simpleName}: ${t.message ?: "无详细信息"}"
        } finally {
            if (client.isConnected || client.isConnecting) client.disconnect()
        }
    }

    fun runDiagnostics(context: Context, role: String): String = buildString {
        appendLine("Build 9 · China Legacy Wear Probe")
        appendLine("角色：$role")
        appendLine("设备：${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android：${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("GMS：${gmsInfo(context)}")
        appendLine("依赖：play-services-wearable 10.2.0")
        appendLine("接口：GoogleApiClient + Wearable.*Api")
        appendLine()
        append(
            withClient(context) { client ->
                buildString {
                    appendLine("GoogleApiClient：CONNECTED")
                    appendLine("hasConnectedApi(Wearable.API)：${client.hasConnectedApi(Wearable.API)}")

                    val localResult = Wearable.NodeApi.getLocalNode(client).await(10, TimeUnit.SECONDS)
                    appendLine("NodeApi.localNode：status=${localResult.status.statusCode} success=${localResult.status.isSuccess}")
                    if (localResult.status.isSuccess) {
                        appendLine("  ${localResult.node.displayName} / ${localResult.node.id}")
                    }

                    val connectedResult = Wearable.NodeApi.getConnectedNodes(client).await(10, TimeUnit.SECONDS)
                    appendLine("NodeApi.connectedNodes：status=${connectedResult.status.statusCode} success=${connectedResult.status.isSuccess} count=${connectedResult.nodes.size}")
                    connectedResult.nodes.forEach {
                        appendLine("  ${it.displayName} / ${it.id} nearby=${it.isNearby}")
                    }
                    appendLine("最近接收：${lastEvent(context)}")
                }
            }
        )
    }

    fun sendMessage(context: Context, role: String): String = withClient(context) { client ->
        val nodesResult = Wearable.NodeApi.getConnectedNodes(client).await(10, TimeUnit.SECONDS)
        if (!nodesResult.status.isSuccess) return@withClient "NodeApi FAIL status=${nodesResult.status.statusCode}"
        if (nodesResult.nodes.isEmpty()) return@withClient "失败：connectedNodes=0"
        val payload = "${role}_LEGACY_PROBE|${System.currentTimeMillis()}|${Build.MODEL}".toByteArray()
        nodesResult.nodes.joinToString("\n") { node ->
            val result = Wearable.MessageApi.sendMessage(client, node.id, MESSAGE_PATH, payload)
                .await(10, TimeUnit.SECONDS)
            "${node.displayName}: status=${result.status.statusCode} success=${result.status.isSuccess} requestId=${result.requestId}"
        }
    }

    fun sendAck(context: Context, targetNodeId: String, role: String): String = withClient(context) { client ->
        val result = Wearable.MessageApi.sendMessage(
            client,
            targetNodeId,
            ACK_PATH,
            "${role}_ACK|${System.currentTimeMillis()}".toByteArray()
        ).await(10, TimeUnit.SECONDS)
        "ACK status=${result.status.statusCode} success=${result.status.isSuccess} requestId=${result.requestId}"
    }

    fun sendDataItem(context: Context, role: String): String = withClient(context) { client ->
        val request = PutDataMapRequest.create(DATA_PATH).apply {
            dataMap.putLong("timestamp", System.currentTimeMillis())
            dataMap.putString("from", role)
            dataMap.putString("model", Build.MODEL)
            dataMap.putAsset("marker", Asset.createFromBytes("legacy-10.2.0".toByteArray()))
        }.asPutDataRequest().setUrgent()
        val result = Wearable.DataApi.putDataItem(client, request).await(10, TimeUnit.SECONDS)
        "DataApi.putDataItem status=${result.status.statusCode} success=${result.status.isSuccess} uri=${result.dataItem?.uri ?: "null"}"
    }

    private fun connectionName(result: ConnectionResult): String = when (result.errorCode) {
        ConnectionResult.SUCCESS -> "SUCCESS"
        ConnectionResult.SERVICE_MISSING -> "SERVICE_MISSING"
        ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "SERVICE_VERSION_UPDATE_REQUIRED"
        ConnectionResult.SERVICE_DISABLED -> "SERVICE_DISABLED"
        ConnectionResult.SERVICE_INVALID -> "SERVICE_INVALID"
        ConnectionResult.API_UNAVAILABLE -> "API_UNAVAILABLE"
        else -> "UNKNOWN"
    }

    private fun nowText(): String =
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
}
