package com.hufeng943.timetable.sync

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.hufeng943.timetable.shared.importexport.WearFileTransferProtocol
import com.hufeng943.timetable.shared.sync.SyncEnvelope
import com.hufeng943.timetable.shared.sync.SyncRecordPayload
import com.hufeng943.timetable.shared.sync.SyncResult
import com.hufeng943.timetable.shared.sync.SyncTransport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Wear Data Layer transport for Wear OS for China.
 *
 * Android's China-specific Wear OS guidance explicitly requires the 10.2.0
 * wearable client and recommends the legacy GoogleApiClient/Wearable API path.
 * Keep this transport deliberately conservative: connect, discover a node,
 * write an urgent DataItem, then disconnect.
 */
class WearOsTransport(
    private val context: Context,
) : SyncTransport {
    override val name: String = "Wear OS"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun isAvailable(): Boolean = withLegacyClient { client ->
        val result = Wearable.NodeApi.getConnectedNodes(client).await(10, TimeUnit.SECONDS)
        result.status.isSuccess && result.nodes.isNotEmpty()
    }

    override suspend fun sendRecords(records: List<SyncRecordPayload>): SyncResult =
        withLegacyClientResult { client ->
            val nodesResult = Wearable.NodeApi.getConnectedNodes(client).await(10, TimeUnit.SECONDS)
            if (!nodesResult.status.isSuccess) {
                return@withLegacyClientResult SyncResult.Failed(
                    "Wear OS 节点发现失败(${nodesResult.status.statusCode})"
                )
            }

            val node = nodesResult.nodes
                .sortedByDescending { it.isNearby }
                .firstOrNull()
                ?: return@withLegacyClientResult SyncResult.Failed("未检测到已连接的 Wear OS 设备")

            val sourceNode = Wearable.NodeApi.getLocalNode(client)
                .await(10, TimeUnit.SECONDS)
                .node.id
            val requestId = UUID.randomUUID().toString()
            val envelope = SyncEnvelope(
                requestId = requestId,
                sourceDeviceId = sourceNode,
                records = records
            )
            val bytes = json.encodeToString(envelope).toByteArray(Charsets.UTF_8)
            val request = PutDataMapRequest.create(WearFileTransferProtocol.path(requestId)).apply {
                dataMap.putString(WearFileTransferProtocol.KEY_KIND, WearFileTransferProtocol.KIND_SYNC_BATCH)
                dataMap.putString(WearFileTransferProtocol.KEY_REQUEST_ID, requestId)
                dataMap.putString(WearFileTransferProtocol.KEY_TARGET_NODE_ID, node.id)
                dataMap.putString(WearFileTransferProtocol.KEY_SOURCE_NODE_ID, sourceNode)
                dataMap.putString(WearFileTransferProtocol.KEY_MIME_TYPE, "application/json")
                dataMap.putAsset(WearFileTransferProtocol.KEY_ASSET, Asset.createFromBytes(bytes))
            }.asPutDataRequest().setUrgent()

            val putResult = Wearable.DataApi.putDataItem(client, request)
                .await(15, TimeUnit.SECONDS)
            if (putResult.status.isSuccess) {
                SyncResult.Success
            } else {
                SyncResult.Failed(
                    "Wear OS 数据发送失败(${putResult.status.statusCode})"
                )
            }
        }

    override suspend fun receive(): SyncResult =
        SyncResult.Failed("Wear OS 手机端接收由 Data Layer ListenerService 处理")

    private suspend fun withLegacyClient(block: (GoogleApiClient) -> Boolean): Boolean {
        return withLegacyClientResult { client ->
            if (block(client)) SyncResult.Success else SyncResult.Failed("Wear OS 不可用")
        } == SyncResult.Success
    }

    private suspend fun withLegacyClientResult(
        block: (GoogleApiClient) -> SyncResult
    ): SyncResult {
        var lastFailure = "Wear OS GoogleApiClient 连接失败"
        repeat(3) { attempt ->
            val client = GoogleApiClient.Builder(context.applicationContext)
                .addApi(Wearable.API)
                .build()
            try {
                val result = client.blockingConnect(12, TimeUnit.SECONDS)
                if (result.isSuccess && client.hasConnectedApi(Wearable.API)) {
                    return block(client)
                }
                lastFailure = "Wear OS 连接失败 code=${result.errorCode} (${connectionName(result)})"
                if (attempt < 2) Thread.sleep(2000)
            } catch (t: Throwable) {
                lastFailure = "Wear OS 连接异常: ${t.message ?: t.javaClass.simpleName}"
                if (attempt < 2) Thread.sleep(2000)
            } finally {
                if (client.isConnected || client.isConnecting) client.disconnect()
            }
        }
        return SyncResult.Failed(lastFailure)
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
}
