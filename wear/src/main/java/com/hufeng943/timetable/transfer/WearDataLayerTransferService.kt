package com.hufeng943.timetable.transfer

import android.content.Intent
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.hufeng943.timetable.shared.export.BackupManager
import com.hufeng943.timetable.shared.export.CsvImporter
import com.hufeng943.timetable.shared.export.IcsImporter
import com.hufeng943.timetable.shared.importexport.ImportService
import com.hufeng943.timetable.shared.importexport.WearFileTransferProtocol
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream

@AndroidEntryPoint
class WearDataLayerTransferService : WearableListenerService() {
    @javax.inject.Inject lateinit var importService: ImportService

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED ||
                event.dataItem.uri.path != WearFileTransferProtocol.PATH
            ) return@forEach

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val targetNodeId = dataMap.getString(WearFileTransferProtocol.KEY_TARGET_NODE_ID)
            val localNodeId = runCatching {
                Tasks.await(Wearable.getNodeClient(this).localNode).id
            }.getOrNull()

            if (targetNodeId != null && targetNodeId != localNodeId) {
                return@forEach
            }

            val processed = if (dataMap.getString(WearFileTransferProtocol.KEY_KIND) ==
                WearFileTransferProtocol.KIND_PHONE_IMPORT_RESULT
            ) {
                runCatching {
                    val errorMessage = dataMap.getString(WearFileTransferProtocol.KEY_ERROR_MESSAGE)
                    if (!errorMessage.isNullOrBlank()) {
                        throw IllegalStateException(errorMessage)
                    }
                    importAsset(dataMap.getAsset(WearFileTransferProtocol.KEY_ASSET))
                }.onSuccess {
                    sendResultBroadcast(true, null)
                }.onFailure { error ->
                    sendResultBroadcast(false, error.message ?: "导入失败")
                }
                true
            } else {
                false
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

    private fun importAsset(asset: Asset?) {
        requireNotNull(asset) { "导入数据缺少文件内容" }
        val bytes = Tasks.await(Wearable.getDataClient(this).getFdForAsset(asset))
            ?.inputStream
            ?.use { it.readBytes() }
            ?: throw IllegalStateException("无法读取手机发送的文件")

        val content = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val trimmed = content.trimStart()
        val timetables = when {
            trimmed.startsWith("{") || trimmed.startsWith("[") -> parseJson(bytes, content)
            content.contains("BEGIN:VCALENDAR", ignoreCase = true) -> IcsImporter.parseIcs(content)
            content.contains("学期,课程名称") || content.contains("星期") -> CsvImporter.parseCsv(content)
            else -> throw IllegalArgumentException("无法识别的文件格式，仅支持 .json/.ics/.csv")
        }

        if (timetables.isEmpty()) throw IllegalStateException("未在文件中解析到有效课表")
        runBlocking(Dispatchers.IO) { importService.importAtomic(timetables) }
    }

    private fun parseJson(
        bytes: ByteArray,
        content: String
    ): List<com.hufeng943.timetable.shared.model.Timetable> {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        runCatching { BackupManager.restore(ByteArrayInputStream(bytes)) }
            .getOrNull()
            ?.let { return it }
        runCatching {
            json.decodeFromString<List<com.hufeng943.timetable.shared.model.Timetable>>(content)
        }.getOrNull()?.let { return it }
        runCatching {
            json.decodeFromString<com.hufeng943.timetable.shared.model.Timetable>(content)
        }.getOrNull()?.let { return listOf(it) }
        throw IllegalArgumentException("JSON 课表文件格式不受支持或文件已损坏")
    }

    private fun sendResultBroadcast(success: Boolean, message: String?) {
        sendBroadcast(Intent(ACTION_IMPORT_RESULT).apply {
            setPackage(packageName)
            putExtra(EXTRA_SUCCESS, success)
            if (message != null) putExtra(EXTRA_MESSAGE, message)
        })
    }

    companion object {
        const val ACTION_IMPORT_RESULT = "com.hufeng943.timetable.action.IMPORT_RESULT"
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_MESSAGE = "message"
    }
}
