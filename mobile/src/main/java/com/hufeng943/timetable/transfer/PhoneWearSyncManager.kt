package com.hufeng943.timetable.transfer

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.hufeng943.timetable.shared.export.BackupManager
import com.hufeng943.timetable.shared.importexport.WearFileTransferProtocol
import com.hufeng943.timetable.shared.model.Timetable
import java.io.ByteArrayOutputStream

object PhoneWearSyncManager {
    fun send(context: Context, timetables: List<Timetable>): String {
        require(timetables.isNotEmpty()) { "没有可同步的课表" }

        val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
        val node = nodes.sortedByDescending { it.isNearby }.firstOrNull()
            ?: throw IllegalStateException("未检测到已连接的 Wear OS 设备")

        val bytes = ByteArrayOutputStream().use { output ->
            BackupManager.backup(output, timetables)
            output.toByteArray()
        }
        val requestId = System.currentTimeMillis().toString()
        val request = PutDataMapRequest.create(WearFileTransferProtocol.PATH).apply {
            dataMap.putString(
                WearFileTransferProtocol.KEY_KIND,
                WearFileTransferProtocol.KIND_PHONE_PUSH_TIMETABLES
            )
            dataMap.putString(WearFileTransferProtocol.KEY_REQUEST_ID, requestId)
            dataMap.putString(WearFileTransferProtocol.KEY_TARGET_NODE_ID, node.id)
            dataMap.putString(WearFileTransferProtocol.KEY_FILE_NAME, "Timetable_Phone_$requestId.json")
            dataMap.putString(WearFileTransferProtocol.KEY_MIME_TYPE, "application/json")
            dataMap.putAsset(WearFileTransferProtocol.KEY_ASSET, Asset.createFromBytes(bytes))
        }.asPutDataRequest().setUrgent()

        Tasks.await(Wearable.getDataClient(context).putDataItem(request))
        return node.displayName
    }
}
