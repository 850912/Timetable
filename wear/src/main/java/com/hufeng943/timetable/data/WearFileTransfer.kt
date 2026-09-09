package com.hufeng943.timetable.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.hufeng943.timetable.shared.importexport.WearFileTransferProtocol
import com.hufeng943.timetable.sync.LegacyWearIo
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.export.BackupManager
import com.hufeng943.timetable.shared.export.CsvExporter
import com.hufeng943.timetable.shared.export.IcsExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        // The selected export format is preserved as the user's downloadable
        // file, while a canonical JSON backup is sent alongside it for exact
        // import into the companion phone app.  This avoids reconstructing an
        // app timetable from lossy ICS/CSV exports.
        val appImportBytes = ByteArrayOutputStream().use { output ->
            BackupManager.backup(output, timetables)
            output.toByteArray()
        }

        val timestamp = java.text.SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(java.util.Date())
        val extension = format.extension
        val fileName = "Timetable_Export_${timestamp}.${extension}"

        val requestId = UUID.randomUUID().toString()
        val request = PutDataMapRequest.create(WearFileTransferProtocol.path(requestId)).apply {
            dataMap.putString(
                WearFileTransferProtocol.KEY_KIND,
                WearFileTransferProtocol.KIND_WEAR_EXPORT
            )
            dataMap.putString(
                WearFileTransferProtocol.KEY_REQUEST_ID,
                requestId
            )
            dataMap.putString(WearFileTransferProtocol.KEY_FILE_NAME, fileName)
            dataMap.putString(WearFileTransferProtocol.KEY_MIME_TYPE, format.mimeType)
            dataMap.putAsset(WearFileTransferProtocol.KEY_ASSET, Asset.createFromBytes(bytes))
            dataMap.putAsset(
                WearFileTransferProtocol.KEY_APP_IMPORT_ASSET,
                Asset.createFromBytes(appImportBytes)
            )
        }.asPutDataRequest().setUrgent()

        LegacyWearIo.putDataItem(context, request)
    }

    suspend fun requestImportFromPhone(context: Context): Boolean {
        return try {
            val requestId = UUID.randomUUID().toString()
            val sourceNodeId = withContext(Dispatchers.IO) { LegacyWearIo.localNodeId(context) }
            val uri = Uri.Builder()
                .scheme(WearFileTransferProtocol.URI_SCHEME)
                .authority(WearFileTransferProtocol.URI_HOST)
                .appendQueryParameter(WearFileTransferProtocol.URI_MODE, WearFileTransferProtocol.MODE_IMPORT)
                .appendQueryParameter(WearFileTransferProtocol.URI_REQUEST_ID, requestId)
                .appendQueryParameter(WearFileTransferProtocol.URI_SOURCE_NODE_ID, sourceNodeId)
                .build()

            withContext(Dispatchers.Main) {
                RemoteActivityHelper(context, context.mainExecutor)
                    .startRemoteActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(uri)
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                    )
                    .await()
            }
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
