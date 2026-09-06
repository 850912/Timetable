package com.hufeng943.timetable.transfer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.tasks.Tasks
import com.hufeng943.timetable.shared.importexport.WearFileTransferProtocol
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Phone-side endpoint opened by the Wear app for importing a timetable file.
 * The Android Storage Access Framework handles the actual file picker UI.
 */
class PhoneFileTransferActivity : Activity() {
    companion object {
        private const val REQUEST_OPEN_FILE = 4101
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var requestId: String? = null
    private var targetNodeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestId = intent.data?.getQueryParameter(WearFileTransferProtocol.URI_REQUEST_ID)
        targetNodeId = intent.data?.getQueryParameter(WearFileTransferProtocol.URI_SOURCE_NODE_ID)

        if (requestId.isNullOrBlank() || targetNodeId.isNullOrBlank()) {
            finish()
            return
        }

        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            },
            REQUEST_OPEN_FILE
        )
    }

    @Deprecated("This small remote endpoint intentionally uses the platform callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_OPEN_FILE) return

        if (resultCode == RESULT_OK && data?.data != null) {
            val uri = data.data!!
            executor.execute {
                runCatching {
                    sendSelectedFile(uri)
                }.onFailure { error ->
                    runCatching { sendTransferError(error.message ?: "无法读取所选文件") }
                }
                runOnUiThread { finish() }
            }
        } else {
            finish()
        }
    }

    private fun sendSelectedFile(uri: Uri) {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("无法读取所选文件")

        val fileName = queryDisplayName(uri)
            ?: "Timetable_Import_${System.currentTimeMillis()}"
        val mimeType = contentResolver.getType(uri) ?: "*/*"

        val request = PutDataMapRequest.create(WearFileTransferProtocol.PATH).apply {
            dataMap.putString(
                WearFileTransferProtocol.KEY_KIND,
                WearFileTransferProtocol.KIND_PHONE_IMPORT_RESULT
            )
            dataMap.putString(WearFileTransferProtocol.KEY_REQUEST_ID, requestId)
            dataMap.putString(WearFileTransferProtocol.KEY_TARGET_NODE_ID, targetNodeId)
            dataMap.putString(WearFileTransferProtocol.KEY_FILE_NAME, fileName)
            dataMap.putString(WearFileTransferProtocol.KEY_MIME_TYPE, mimeType)
            dataMap.putAsset(WearFileTransferProtocol.KEY_ASSET, Asset.createFromBytes(bytes))
        }.asPutDataRequest().setUrgent()

        Tasks.await(Wearable.getDataClient(this).putDataItem(request))
    }


    private fun sendTransferError(message: String) {
        val request = PutDataMapRequest.create(WearFileTransferProtocol.PATH).apply {
            dataMap.putString(
                WearFileTransferProtocol.KEY_KIND,
                WearFileTransferProtocol.KIND_PHONE_IMPORT_RESULT
            )
            dataMap.putString(WearFileTransferProtocol.KEY_REQUEST_ID, requestId)
            dataMap.putString(WearFileTransferProtocol.KEY_TARGET_NODE_ID, targetNodeId)
            dataMap.putString(WearFileTransferProtocol.KEY_ERROR_MESSAGE, message)
        }.asPutDataRequest().setUrgent()

        Tasks.await(Wearable.getDataClient(this).putDataItem(request))
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
