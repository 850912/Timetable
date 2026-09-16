package com.hufeng943.timetable.transfer

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.WearableListenerService
import com.hufeng943.timetable.shared.importexport.WearBridgeProtocol
import com.hufeng943.timetable.shared.importexport.WearFileTransferProtocol
import com.hufeng943.timetable.shared.importexport.TimetableFileParser
import com.hufeng943.timetable.TimetableDatabaseProvider
import com.hufeng943.timetable.sync.LegacyWearIo
import com.hufeng943.timetable.sync.AutoSyncJobService
import com.hufeng943.timetable.sync.WearConnectionState
import com.hufeng943.timetable.sync.WearOsTransport
import com.hufeng943.timetable.sync.SyncAckTracker
import com.hufeng943.timetable.reminder.CourseReminderScheduler
import com.hufeng943.timetable.widget.TodayWidgetProvider
import com.hufeng943.timetable.shared.sync.SyncAck
import com.hufeng943.timetable.shared.sync.SyncApplier
import com.hufeng943.timetable.shared.sync.SyncRecordPayload
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PhoneWearDataLayerService : WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onPeerConnected(peer: Node) {
        WearConnectionState.update(true)
        AutoSyncJobService.scheduleNow(this)
    }

    override fun onPeerDisconnected(peer: Node) {
        WearConnectionState.update(false)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearBridgeProtocol.HELLO_PATH) return

        getSharedPreferences(WearBridgeProtocol.PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(WearBridgeProtocol.KEY_READY_NODE_ID, messageEvent.sourceNodeId)
            .putLong(WearBridgeProtocol.KEY_READY_AT, System.currentTimeMillis())
            .apply()

        serviceScope.launch {
            runCatching {
                LegacyWearIo.sendMessage(
                    this@PhoneWearDataLayerService,
                    messageEvent.sourceNodeId,
                    WearBridgeProtocol.READY_PATH,
                    "PHONE_READY|${WearBridgeProtocol.PROTOCOL_VERSION}|${System.currentTimeMillis()}".toByteArray()
                )
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            val dataPath = event.dataItem.uri.path
            if (event.type != DataEvent.TYPE_CHANGED ||
                !WearFileTransferProtocol.matchesPath(dataPath)
            ) return@forEach

            // Copy callback-owned values before returning from WearableListenerService.
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val dataUri = event.dataItem.uri
            serviceScope.launch {
                val target = dataMap.getString(WearFileTransferProtocol.KEY_TARGET_NODE_ID)
                if (target != null) {
                    val local = runCatching { LegacyWearIo.localNodeId(this@PhoneWearDataLayerService) }.getOrNull()
                    if (local != target) return@launch
                }
                val processed = when (dataMap.getString(WearFileTransferProtocol.KEY_KIND)) {
                    WearFileTransferProtocol.KIND_SYNC_ACK -> runCatching { applySyncAck(dataMap) }.getOrDefault(false)
                    WearFileTransferProtocol.KIND_WEAR_EXPORT -> runCatching { saveWearExport(dataMap) }.isSuccess
                    else -> false
                }
                if (processed) {
                    runCatching { LegacyWearIo.deleteDataItem(this@PhoneWearDataLayerService, dataUri) }
                }
            }
        }
    }

    private suspend fun applySyncAck(dataMap: com.google.android.gms.wearable.DataMap): Boolean {
        val asset = dataMap.getAsset(WearFileTransferProtocol.KEY_ASSET) ?: error("缺少同步 ACK")
        val bytes = readAsset(asset).use { it.readBytes() }
        val ack = json.decodeFromString<SyncAck>(bytes.toString(Charsets.UTF_8))
        SyncAckTracker.complete(ack)
        val db = TimetableDatabaseProvider.database(this)
        if (ack.appliedRecordIds.isNotEmpty()) {
            db.syncRecordDao().markSynced(ack.appliedRecordIds)
        }
        if (ack.records.isNotEmpty()) {
            val target = dataMap.getString(WearFileTransferProtocol.KEY_SOURCE_NODE_ID) ?: return false
            val applied = SyncApplier(db).applyOnce(ack.requestId, ack.sourceDeviceId, ack.records)
            if (applied.isNotEmpty()) refreshLocalSurfaces()
            sendSyncAck(target, ack.requestId, applied)
            return applied.size == ack.records.size
        }
        return true
    }

    private fun sendSyncAck(targetNodeId: String, requestId: String, appliedIds: List<Long>) {
        val localNodeId = runCatching { LegacyWearIo.localNodeId(this) }.getOrNull() ?: return
        val ack = SyncAck(requestId = requestId, sourceDeviceId = localNodeId, appliedRecordIds = appliedIds)
        val bytes = json.encodeToString(ack).toByteArray(Charsets.UTF_8)
        val request = com.google.android.gms.wearable.PutDataMapRequest.create(
            WearFileTransferProtocol.path(requestId)
        ).apply {
            dataMap.putString(WearFileTransferProtocol.KEY_KIND, WearFileTransferProtocol.KIND_SYNC_ACK)
            dataMap.putString(WearFileTransferProtocol.KEY_REQUEST_ID, requestId)
            dataMap.putString(WearFileTransferProtocol.KEY_TARGET_NODE_ID, targetNodeId)
            dataMap.putString(WearFileTransferProtocol.KEY_SOURCE_NODE_ID, localNodeId)
            dataMap.putAsset(WearFileTransferProtocol.KEY_ASSET, Asset.createFromBytes(bytes))
        }.asPutDataRequest().setUrgent()
        LegacyWearIo.withClient(this) { client ->
            val result = com.google.android.gms.wearable.Wearable.DataApi.putDataItem(client, request)
                .await(10, java.util.concurrent.TimeUnit.SECONDS)
            if (!result.status.isSuccess) error("发送同步 ACK 失败(${result.status.statusCode})")
        }
    }

    private suspend fun saveWearExport(dataMap: com.google.android.gms.wearable.DataMap) {
        val asset = dataMap.getAsset(WearFileTransferProtocol.KEY_ASSET)
            ?: throw IllegalArgumentException("缺少导出文件")
        val fileName = sanitizeFileName(
            dataMap.getString(WearFileTransferProtocol.KEY_FILE_NAME)
                ?: "Timetable_Export"
        )
        val mimeType = dataMap.getString(WearFileTransferProtocol.KEY_MIME_TYPE)
            ?: "application/octet-stream"

        // Keep the user's selected export bytes for Downloads, but prefer the
        // canonical JSON backup asset for importing into the companion app.
        // Falling back to the primary asset keeps compatibility with older Wear
        // builds that only send KEY_ASSET.
        val bytes = readAsset(asset).use { it.readBytes() }
        val appImportAsset = dataMap.getAsset(WearFileTransferProtocol.KEY_APP_IMPORT_ASSET)
        val appImportBytes = if (appImportAsset != null) {
            readAsset(appImportAsset).use { it.readBytes() }
        } else {
            bytes
        }
        val timetables = TimetableFileParser.parse(appImportBytes)
        TimetableDatabaseProvider.importService(this@PhoneWearDataLayerService)
            .importReplacingMatchesAtomic(
                timetables,
                dataMap.getString(WearFileTransferProtocol.KEY_REQUEST_ID),
                dataMap.getString(WearFileTransferProtocol.KEY_SOURCE_NODE_ID),
            )
        refreshLocalSurfaces()

        bytes.inputStream().use {
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

    private suspend fun refreshLocalSurfaces() {
        runCatching {
            val tables = TimetableDatabaseProvider.repository(this@PhoneWearDataLayerService).getAllTimetables().first()
            CourseReminderScheduler.schedule(this, tables)
            TodayWidgetProvider.requestRefresh(this)
        }
    }

    private fun readAsset(asset: Asset): InputStream {
        return LegacyWearIo.readAsset(this, asset)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "Timetable_Export" }
    }

}
