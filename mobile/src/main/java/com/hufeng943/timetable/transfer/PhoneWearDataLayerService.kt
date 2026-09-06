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
import java.io.File
import java.io.IOException

class PhoneWearDataLayerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED ||
                event.dataItem.uri.path != WearFileTransferProtocol.PATH
            ) return@forEach

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val processed = when (dataMap.getString(WearFileTransferProtocol.KEY_KIND)) {
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
