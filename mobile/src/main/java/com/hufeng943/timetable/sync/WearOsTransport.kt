package com.hufeng943.timetable.sync

import android.content.Context
import com.google.android.gms.common.api.GoogleApiClient
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
        const val CONNECT_ATTEMPTS = 3
        const val CONNECT_TIMEOUT_SECONDS = 8L
        const val OPERATION_TIMEOUT_SECONDS = 10L
        const val RETRY_DELAY_MS = 2_000L
    }

    override suspend fun isAvailable(): Boolean = withClientOrNull { client ->
        Wearable.NodeApi.getConnectedNodes(client)
            .await(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .nodes
            .isNotEmpty()
    } ?: false

    override suspend fun sendRecords(records: List<SyncRecordPayload>): SyncResult {
        if (records.isEmpty()) return SyncResult.Success

        return try {
            withClient { client ->
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

    private fun <T> withClient(block: (GoogleApiClient) -> T): T {
        var lastError: Throwable? = null
        repeat(CONNECT_ATTEMPTS) { attempt ->
            val client = GoogleApiClient.Builder(context.applicationContext)
                .addApi(Wearable.API)
                .build()
            try {
                val result = client.blockingConnect(
                    CONNECT_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                )
                if (result.isSuccess && client.hasConnectedApi(Wearable.API)) {
                    return block(client)
                }
                lastError = IllegalStateException(
                    "Wear OS 连接失败(${result.errorCode})"
                )
            } catch (t: Throwable) {
                lastError = t
            } finally {
                if (client.isConnected || client.isConnecting) client.disconnect()
            }
            if (attempt + 1 < CONNECT_ATTEMPTS) Thread.sleep(RETRY_DELAY_MS)
        }
        throw lastError ?: IllegalStateException("Wear OS 连接失败")
    }

    private fun <T> withClientOrNull(block: (GoogleApiClient) -> T): T? =
        runCatching { withClient(block) }.getOrNull()
}
