package com.hufeng943.timetable.sync

import android.content.Context
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.hufeng943.timetable.shared.importexport.WearBridgeProtocol
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
 * Wear OS transport using the legacy GoogleApiClient Wearable API.
 *
 * This is intentionally limited to the production sync transport. The rest of
 * the app keeps the existing dependency/API surface. China Wear OS guidance
 * explicitly recommends the GoogleApiClient-related Wearable APIs.
 */
class WearOsTransport(
    private val context: Context,
) : SyncTransport {
    override val name: String = "Wear OS"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private companion object {
        const val OPERATION_TIMEOUT_SECONDS = 10L
    }

    override suspend fun isAvailable(): Boolean = runCatching { LegacyWearIo.withClient(context) { client ->
        Wearable.NodeApi.getConnectedNodes(client)
            .await(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .nodes
            .isNotEmpty()
    } }.getOrDefault(false)

    override suspend fun sendRecords(records: List<SyncRecordPayload>): SyncResult {
        if (records.isEmpty()) return SyncResult.Success

        return try {
            requireWatchBootstrap()
            LegacyWearIo.withClient(context) { client ->
                val nodesResult = Wearable.NodeApi.getConnectedNodes(client)
                    .await(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (!nodesResult.status.isSuccess) {
                    throw IllegalStateException("Wear OS 节点发现失败(${nodesResult.status.statusCode})")
                }

                val node = nodesResult.nodes
                    .sortedByDescending { it.isNearby }
                    .firstOrNull()
                    ?: throw IllegalStateException("未检测到已连接的 Wear OS 设备")

                val sourceNode = Wearable.NodeApi.getLocalNode(client)
                    .await(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (!sourceNode.status.isSuccess) {
                    throw IllegalStateException("无法获取本机 Wear OS 节点(${sourceNode.status.statusCode})")
                }

                val requestId = UUID.randomUUID().toString()
                val envelope = SyncEnvelope(
                    requestId = requestId,
                    sourceDeviceId = sourceNode.node.id,
                    records = records,
                )
                val bytes = json.encodeToString(envelope).toByteArray(Charsets.UTF_8)
                val request = PutDataMapRequest.create(
                    WearFileTransferProtocol.path(requestId)
                ).apply {
                    dataMap.putString(WearFileTransferProtocol.KEY_KIND, WearFileTransferProtocol.KIND_SYNC_BATCH)
                    dataMap.putString(WearFileTransferProtocol.KEY_REQUEST_ID, requestId)
                    dataMap.putString(WearFileTransferProtocol.KEY_TARGET_NODE_ID, node.id)
                    dataMap.putString(WearFileTransferProtocol.KEY_SOURCE_NODE_ID, sourceNode.node.id)
                    dataMap.putString(WearFileTransferProtocol.KEY_MIME_TYPE, "application/json")
                    dataMap.putAsset(WearFileTransferProtocol.KEY_ASSET, Asset.createFromBytes(bytes))
                }.asPutDataRequest().setUrgent()

                val result = Wearable.DataApi.putDataItem(client, request)
                    .await(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                if (!result.status.isSuccess) {
                    throw IllegalStateException("Wear OS 数据发送失败(${result.status.statusCode})")
                }
            }
            SyncResult.Success
        } catch (e: Exception) {
            SyncResult.Failed(e.message ?: "Wear OS 增量同步失败", e)
        }
    }

    override suspend fun receive(): SyncResult =
        SyncResult.Failed("Wear OS 手机端接收由 Data Layer ListenerService 处理")

    private fun requireWatchBootstrap() {
        val prefs = context.getSharedPreferences(WearBridgeProtocol.PREFS, Context.MODE_PRIVATE)
        val nodeId = prefs.getString(WearBridgeProtocol.KEY_READY_NODE_ID, null)
        val readyAt = prefs.getLong(WearBridgeProtocol.KEY_READY_AT, 0L)
        val fresh = nodeId != null && System.currentTimeMillis() - readyAt <= WearBridgeProtocol.READY_TTL_MS
        if (!fresh) {
            throw IllegalStateException("手表通信桥尚未就绪，请先打开手表端 Timetable；手表会自动完成连接握手")
        }
    }
}
