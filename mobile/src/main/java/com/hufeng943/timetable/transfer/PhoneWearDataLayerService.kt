package com.hufeng943.timetable.transfer

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.hufeng943.timetable.shared.importexport.WearFileTransferProtocol
import com.hufeng943.timetable.TimetableDatabaseProvider
import com.hufeng943.timetable.shared.sync.SyncAck
import com.hufeng943.timetable.shared.sync.SyncApplier
import com.hufeng943.timetable.shared.sync.SyncRecordPayload
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

class PhoneWearDataLayerService : WearableListenerService() {
    private val json = Json { ignoreUnknownKeys = true }
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED ||
                event.dataItem.uri.path != WearFileTransferProtocol.PATH
            ) return@forEach

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val target = dataMap.getString(WearFileTransferProtocol.KEY_TARGET_NODE_ID)
            if (target != null) {
                val local = runCatching { Tasks.await(Wearable.getNodeClient(this).localNode).id }.getOrNull()
                if (local != target) return@forEach
            }
            val processed = when (dataMap.getString(WearFileTransferProtocol.KEY_KIND)) {
                WearFileTransferProtocol.KIND_SYNC_ACK -> runCatching { applySyncAck(dataMap) }.getOrDefault(false)
                WearFileTransferProtocol.KIND_WEAR_EXPORT ->
                    runCatching { saveWearExport(dataMap) }.isSuccess
                else -> false
            }

            if (processed) {
                runCatching {
                    Tasks.await(
                        Wearable.getDataClient(this).deleteDataItems(event.dataItem.uri)
                    )
                }
            }
        }
    }

    private fun applySyncAck(dataMap: com.google.android.gms.wearable.DataMap): Boolean {
        val asset = dataMap.getAsset(WearFileTransferProtocol.KEY_ASSET) ?: error("缺少同步 ACK")
        val bytes = readAsset(asset).use { it.readBytes() }
        val ack = json.decodeFromString<SyncAck>(bytes.toString(Charsets.UTF_8))
        val db = TimetableDatabaseProvider.database(this)
        if (ack.appliedRecordIds.isNotEmpty()) {
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) { db.syncRecordDao().markSynced(ack.appliedRecordIds) }
        }
        if (ack.records.isNotEmpty()) {
            val target = dataMap.getString(WearFileTransferProtocol.KEY_SOURCE_NODE_ID) ?: return false
            val applied = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) { SyncApplier(db).apply(ack.records) }
            sendSyncAck(target, ack.requestId, applied)
            return applied.size == ack.records.size
        }
        return true
    }

    private fun sendSyncAck(targetNodeId: String, requestId: String, appliedIds: List<Long>) {
        val localNodeId = runCatching { Tasks.await(Wearable.getNodeClient(this).localNode).id }.getOrNull() ?: return
        val ack = SyncAck(requestId = requestId, sourceDeviceId = localNodeId, appliedRecordIds = appliedIds)
        val bytes = json.encodeToString(ack).toByteArray(Charsets.UTF_8)
        val request = com.google.android.gms.wearable.PutDataMapRequest.create(WearFileTransferProtocol.PATH).apply {
            dataMap.putString(WearFileTransferProtocol.KEY_KIND, WearFileTransferProtocol.KIND_SYNC_ACK)
            dataMap.putString(WearFileTransferProtocol.KEY_REQUEST_ID, requestId)
            dataMap.putString(WearFileTransferProtocol.KEY_TARGET_NODE_ID, targetNodeId)
            dataMap.putString(WearFileTransferProtocol.KEY_SOURCE_NODE_ID, localNodeId)
            dataMap.putAsset(WearFileTransferProtocol.KEY_ASSET, Asset.createFromBytes(bytes))
        }.asPutDataRequest().setUrgent()
        Tasks.await(Wearable.getDataClient(this).putDataItem(request))
    }

    private fun saveWearExport(dataMap: com.google.android.gms.wearable.DataMap) {
        val asset = dataMap.getAsset(WearFileTransferProtocol.KEY_ASSET)
            ?: throw IllegalArgumentException("缺少导出文件")
        val fileName = sanitizeFileName(
            dataMap.getString(WearFileTransferProtocol.KEY_FILE_NAME)
                ?: "Timetable_Export"
        )
        val mimeType = dataMap.getString(WearFileTransferProtocol.KEY_MIME_TYPE)
            ?: "application/octet-stream"

        val inputStream = readAsset(asset)
        inputStream.use {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + File.separator + "Timetable"
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("无法创建手机端下载文件")

            try {
                resolver.openOutputStream(uri)?.use { output ->
                    it.copyTo(output)
                } ?: throw IOException("无法写入手机端下载文件")

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (error: Exception) {
                resolver.delete(uri, null, null)
                throw error
            }
        }
    }

    private fun readAsset(asset: Asset) =
        Tasks.await(Wearable.getDataClient(this).getFdForAsset(asset))?.inputStream
            ?: throw IOException("无法读取 Wear 导出资源")

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "Timetable_Export" }
    }
}
