package com.hufeng943.timetable.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.hufeng943.timetable.shared.importexport.WearFileTransferProtocol
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.export.BackupManager
import com.hufeng943.timetable.shared.export.CsvExporter
import com.hufeng943.timetable.shared.export.IcsExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.guava.await
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID

object WearFileTransfer {
    suspend fun exportToPhone(
        context: Context,
        format: ExportFormatForPhone,
        timetables: List<Timetable>
    ) = withContext(Dispatchers.IO) {
        val bytes = ByteArrayOutputStream().use { output ->
            when (format) {
                ExportFormatForPhone.ICS -> IcsExporter.streamIcs(output, timetables)
                ExportFormatForPhone.CSV -> CsvExporter.streamCsv(output, timetables)
                ExportFormatForPhone.JSON_BACKUP -> BackupManager.backup(output, timetables)
            }
            output.toByteArray()
        }

        val timestamp = java.text.SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(java.util.Date())
        val extension = format.extension
        val fileName = "Timetable_Export_${timestamp}.${extension}"

        val request = PutDataMapRequest.create(WearFileTransferProtocol.PATH).apply {
            dataMap.putString(
                WearFileTransferProtocol.KEY_KIND,
                WearFileTransferProtocol.KIND_WEAR_EXPORT
            )
            dataMap.putString(
                WearFileTransferProtocol.KEY_REQUEST_ID,
                UUID.randomUUID().toString()
            )
            dataMap.putString(WearFileTransferProtocol.KEY_FILE_NAME, fileName)
            dataMap.putString(WearFileTransferProtocol.KEY_MIME_TYPE, format.mimeType)
            dataMap.putAsset(WearFileTransferProtocol.KEY_ASSET, Asset.createFromBytes(bytes))
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request).await()
    }

    suspend fun requestImportFromPhone(context: Context): Boolean = withContext(Dispatchers.Main) {
        try {
            val requestId = UUID.randomUUID().toString()
            val sourceNodeId = Wearable.getNodeClient(context).localNode.await().id
            val uri = Uri.Builder()
                .scheme(WearFileTransferProtocol.URI_SCHEME)
                .authority(WearFileTransferProtocol.URI_HOST)
                .appendQueryParameter(WearFileTransferProtocol.URI_MODE, WearFileTransferProtocol.MODE_IMPORT)
                .appendQueryParameter(WearFileTransferProtocol.URI_REQUEST_ID, requestId)
                .appendQueryParameter(WearFileTransferProtocol.URI_SOURCE_NODE_ID, sourceNodeId)
                .build()

            RemoteActivityHelper(context, context.mainExecutor)
                .startRemoteActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setData(uri)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                )
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }
}

enum class ExportFormatForPhone(
    val extension: String,
    val mimeType: String
) {
    ICS("ics", "text/calendar"),
    CSV("csv", "text/csv"),
    JSON_BACKUP("json", "application/json")
}
