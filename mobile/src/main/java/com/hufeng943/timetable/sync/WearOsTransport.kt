package com.hufeng943.timetable.sync

import android.content.Context
import com.google.android.gms.common.api.ApiException
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

class WearOsTransport(
    private val context: Context,
) : SyncTransport {
    override val name: String = "Wear OS"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun isAvailable(): Boolean = try {
        Tasks.await(Wearable.getNodeClient(context).connectedNodes).isNotEmpty()
    } catch (_: Exception) { false }

    override suspend fun sendRecords(records: List<SyncRecordPayload>): SyncResult {
        return try {
            val node = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                .sortedByDescending { it.isNearby }.firstOrNull()
                ?: return SyncResult.Failed("未检测到已连接的 Wear OS 设备")
            val sourceNode = Tasks.await(Wearable.getNodeClient(context).localNode).id
            val requestId = UUID.randomUUID().toString()
            val envelope = SyncEnvelope(requestId = requestId, sourceDeviceId = sourceNode, records = records)
            val bytes = json.encodeToString(envelope).toByteArray(Charsets.UTF_8)
            val request = PutDataMapRequest.create(WearFileTransferProtocol.path(requestId)).apply {
                dataMap.putString(WearFileTransferProtocol.KEY_KIND, WearFileTransferProtocol.KIND_SYNC_BATCH)
                dataMap.putString(WearFileTransferProtocol.KEY_REQUEST_ID, requestId)
                dataMap.putString(WearFileTransferProtocol.KEY_TARGET_NODE_ID, node.id)
                dataMap.putString(WearFileTransferProtocol.KEY_SOURCE_NODE_ID, sourceNode)
                dataMap.putString(WearFileTransferProtocol.KEY_MIME_TYPE, "application/json")
                dataMap.putAsset(WearFileTransferProtocol.KEY_ASSET, Asset.createFromBytes(bytes))
            }.asPutDataRequest().setUrgent()
            Tasks.await(Wearable.getDataClient(context).putDataItem(request))
            SyncResult.Success
        } catch (e: ApiException) {
            SyncResult.Failed("Wear OS 增量同步失败(${e.statusCode})", e)
        } catch (e: Exception) {
            SyncResult.Failed(e.message ?: "Wear OS 增量同步失败", e)
        }
    }

    override suspend fun receive(): SyncResult = SyncResult.Failed("Wear OS 手机端接收由 Data Layer ListenerService 处理")
}
