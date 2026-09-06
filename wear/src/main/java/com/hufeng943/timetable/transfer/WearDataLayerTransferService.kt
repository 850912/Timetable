package com.hufeng943.timetable.transfer

import android.content.Intent
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.hufeng943.timetable.shared.importexport.ImportService
import com.hufeng943.timetable.shared.importexport.TimetableFileParser
import com.hufeng943.timetable.shared.importexport.WearFileTransferProtocol
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

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

            val kind = dataMap.getString(WearFileTransferProtocol.KEY_KIND)
            val processed = if (kind == WearFileTransferProtocol.KIND_PHONE_IMPORT_RESULT ||
                kind == WearFileTransferProtocol.KIND_PHONE_PUSH_TIMETABLES
            ) {
                runCatching {
                    val errorMessage = dataMap.getString(WearFileTransferProtocol.KEY_ERROR_MESSAGE)
                    if (!errorMessage.isNullOrBlank()) {
                        throw IllegalStateException(errorMessage)
                    }
                    importAsset(
                        dataMap.getAsset(WearFileTransferProtocol.KEY_ASSET),
                        replaceMatching = kind == WearFileTransferProtocol.KIND_PHONE_PUSH_TIMETABLES
                    )
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

    private fun importAsset(asset: Asset?, replaceMatching: Boolean) {
        requireNotNull(asset) { "导入数据缺少文件内容" }
        val bytes = Tasks.await(Wearable.getDataClient(this).getFdForAsset(asset))
            ?.inputStream
            ?.use { it.readBytes() }
            ?: throw IllegalStateException("无法读取手机发送的文件")

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
