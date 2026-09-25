package com.hufeng943.timetable.sync

import android.content.Context
import com.hufeng943.timetable.shared.importexport.ChinaWearProtocol
import com.hufeng943.timetable.shared.importexport.ChinaWearEnvelope
import com.hufeng943.timetable.shared.importexport.ChinaWearPacketCodec
import com.hufeng943.timetable.shared.sync.SyncRecordPayload
import java.util.UUID

class ChinaWearBridge(
    private val context: Context
) {
    suspend fun isReady(): Boolean = true

    suspend fun enqueueSync(records: List<SyncRecordPayload>) {
        val packet = ChinaWearEnvelope(
            requestId = UUID.randomUUID().toString(),
            type = "SYNC_REQUEST",
            timestamp = System.currentTimeMillis(),
            version = ChinaWearProtocol.VERSION,
            payload = "records=${records.size}"
        )

        val bytes = ChinaWearPacketCodec.encode(packet)

        ChinaWearDiagnosticsLogger.record(
            context,
            "enqueue:${packet.requestId}:${bytes.size}"
        )

        // Build11 final boundary:
        // adapter implementations attach here.
    }

    suspend fun pollIncoming() {
        ChinaWearDiagnosticsLogger.record(context, "poll")
    }
}
