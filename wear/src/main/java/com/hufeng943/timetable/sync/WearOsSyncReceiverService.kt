package com.hufeng943.timetable.sync

import android.content.Intent
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.WearableListenerService
import com.hufeng943.timetable.shared.importexport.ImportService
import com.hufeng943.timetable.shared.importexport.TimetableFileParser
import com.hufeng943.timetable.shared.importexport.WearBridgeProtocol
import com.hufeng943.timetable.shared.importexport.WearFileTransferProtocol
import com.hufeng943.timetable.shared.data.database.AppDatabase
import com.hufeng943.timetable.shared.sync.SyncAck
import com.hufeng943.timetable.shared.sync.SyncApplier
import com.hufeng943.timetable.shared.sync.SyncEnvelope
import com.hufeng943.timetable.surface.WearSurfaceRefresher
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Wear-side endpoint for phone initiated timetable sync.
 *
 * This service is deliberately transport-facing: it validates routing,
 * decodes the Data Layer Asset, and hands the timetable graph to ImportService.
 */
@AndroidEntryPoint
class WearOsSyncReceiverService : WearableListenerService() {

    @Inject
    lateinit var importService: ImportService

    @Inject
    lateinit var database: AppDatabase

    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearBridgeProtocol.READY_PATH) return
        getSharedPreferences(WearBridgeProtocol.PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(WearBridgeProtocol.KEY_READY_NODE_ID, messageEvent.sourceNodeId)
            .putLong(WearBridgeProtocol.KEY_READY_AT, System.currentTimeMillis())
            .apply()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED ||
                !WearFileTransferProtocol.matchesPath(event.dataItem.uri.path)
            ) return@forEach

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            if (!isForThisNode(dataMap)) return@forEach

            val kind = dataMap.getString(WearFileTransferProtocol.KEY_KIND)
            when (kind) {
                WearFileTransferProtocol.KIND_SYNC_ACK -> {
                    val success = processSyncAck(dataMap)
                    if (success) deleteDataItem(event)
                }
                WearFileTransferProtocol.KIND_SYNC_BATCH -> {
                    val success = processSyncBatch(dataMap)
                    if (success) deleteDataItem(event)
                }
                WearFileTransferProtocol.KIND_PHONE_PUSH_TIMETABLES,
                WearFileTransferProtocol.KIND_PHONE_IMPORT_RESULT -> {
                    val success = processIncomingTransfer(dataMap)
                    if (success) {
                        runCatching {
                            LegacyWearIo.deleteDataItem(this, event.dataItem.uri)
                        }
                    }
                }
                else -> return@forEach
            }
        }
    }

    private fun processSyncAck(dataMap: DataMap): Boolean {
        return runCatching {
            val asset = dataMap.getAsset(WearFileTransferProtocol.KEY_ASSET) ?: error("缺少 ACK")
            val bytes = LegacyWearIo.readAsset(this, asset).use { it.readBytes() }
            val ack = json.decodeFromString<SyncAck>(bytes.toString(Charsets.UTF_8))
            if (ack.appliedRecordIds.isNotEmpty()) kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                val dao = database.syncRecordDao()
                dao.markSynced(ack.appliedRecordIds)
                dao.deleteSyncedBefore(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
                dao.trimSyncedHistory(500)
            }
            true
        }.isSuccess
    }

    private fun processSyncBatch(dataMap: DataMap): Boolean {
        return runCatching {
            val sourceNodeId = dataMap.getString(WearFileTransferProtocol.KEY_SOURCE_NODE_ID)
                ?: throw IllegalStateException("同步请求缺少源节点")
            val asset = dataMap.getAsset(WearFileTransferProtocol.KEY_ASSET)
                ?: throw IllegalStateException("同步请求缺少数据")
            val bytes = LegacyWearIo.readAsset(this, asset).use { it.readBytes() }
            val envelope = json.decodeFromString<SyncEnvelope>(bytes.toString(Charsets.UTF_8))
            val applied = kotlinx.coroutines.runBlocking(Dispatchers.IO) { SyncApplier(database).apply(envelope.records) }
            val localRecords = kotlinx.coroutines.runBlocking(Dispatchers.IO) { database.syncRecordDao().pending().map { com.hufeng943.timetable.shared.sync.SyncRecordPayload(it.id, it.entityId, it.entityType, it.operation, it.revision, it.updatedAt, it.deviceId, it.payloadJson) } }
            sendSyncAck(sourceNodeId, envelope.requestId, applied, localRecords)
            val complete = applied.size == envelope.records.size
            if (complete) WearSurfaceRefresher.refresh(this)
            sendResultBroadcast(complete, if (complete) null else "部分同步等待重试")
            complete
        }.onFailure { sendResultBroadcast(false, it.message ?: "增量同步失败") }.getOrDefault(false)
    }

    private fun sendSyncAck(targetNodeId: String, requestId: String, appliedIds: List<Long>, records: List<com.hufeng943.timetable.shared.sync.SyncRecordPayload> = emptyList()) {
        val localNodeId = runCatching { LegacyWearIo.localNodeId(this) }.getOrNull() ?: return
        val ack = SyncAck(requestId = requestId, sourceDeviceId = localNodeId, appliedRecordIds = appliedIds, records = records)
        val bytes = json.encodeToString(ack).toByteArray(Charsets.UTF_8)
        val request = PutDataMapRequest.create(WearFileTransferProtocol.path(requestId)).apply {
            dataMap.putString(WearFileTransferProtocol.KEY_KIND, WearFileTransferProtocol.KIND_SYNC_ACK)
            dataMap.putString(WearFileTransferProtocol.KEY_REQUEST_ID, requestId)
            dataMap.putString(WearFileTransferProtocol.KEY_TARGET_NODE_ID, targetNodeId)
            dataMap.putString(WearFileTransferProtocol.KEY_SOURCE_NODE_ID, localNodeId)
            dataMap.putAsset(WearFileTransferProtocol.KEY_ASSET, Asset.createFromBytes(bytes))
        }.asPutDataRequest().setUrgent()
        LegacyWearIo.putDataItem(this, request)
    }

    private fun deleteDataItem(event: DataEvent) {
        runCatching { LegacyWearIo.deleteDataItem(this, event.dataItem.uri) }
    }

    private fun isForThisNode(dataMap: DataMap): Boolean {
        val targetNodeId = dataMap.getString(WearFileTransferProtocol.KEY_TARGET_NODE_ID)
            ?: return true
        val localNodeId = runCatching { LegacyWearIo.localNodeId(this) }.getOrNull()
        return targetNodeId == localNodeId
    }

    private fun processIncomingTransfer(dataMap: DataMap): Boolean {
        return runCatching {
            val errorMessage = dataMap.getString(WearFileTransferProtocol.KEY_ERROR_MESSAGE)
            if (!errorMessage.isNullOrBlank()) throw IllegalStateException(errorMessage)

            val kind = dataMap.getString(WearFileTransferProtocol.KEY_KIND)
            importAsset(
                dataMap.getAsset(WearFileTransferProtocol.KEY_ASSET),
                replaceMatching = kind == WearFileTransferProtocol.KIND_PHONE_PUSH_TIMETABLES
            )
        }.onSuccess {
            WearSurfaceRefresher.refresh(this)
            sendResultBroadcast(true, null)
        }.onFailure { error ->
            sendResultBroadcast(false, error.message ?: "同步失败")
        }.isSuccess
    }

    private fun importAsset(asset: Asset?, replaceMatching: Boolean) {
        requireNotNull(asset) { "同步数据缺少文件内容" }
        val bytes = LegacyWearIo.readAsset(this, asset).use { it.readBytes() }

        val timetables = TimetableFileParser.parse(bytes)
        runBlocking(Dispatchers.IO) {
            if (replaceMatching) {
                importService.importReplacingMatchesAtomic(timetables)
            } else {
                importService.importAtomic(timetables)
            }
        }
    }

    private fun sendResultBroadcast(success: Boolean, message: String?) {
        sendBroadcast(Intent(ACTION_SYNC_RESULT).apply {
            setPackage(packageName)
            putExtra(EXTRA_SUCCESS, success)
            if (message != null) putExtra(EXTRA_MESSAGE, message)
        })
    }

    companion object {
        const val ACTION_SYNC_RESULT = "com.hufeng943.timetable.action.SYNC_RESULT"

        // Compatibility names kept for the existing import screen.
        const val ACTION_IMPORT_RESULT = ACTION_SYNC_RESULT
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_MESSAGE = "message"
    }
}
