package com.hufeng943.timetable.sync

import android.content.Context
import java.util.UUID

class ChinaWearMessageHandler(
    private val context: Context
) {
    fun handle(type: String, payload: ByteArray): String {
        ChinaWearDiagnosticsLogger.record(
            context,
            "received:$type:${payload.size}"
        )

        return when (type) {
            "SYNC_REQUEST" -> {
                "SYNC_ACK:${UUID.randomUUID()}"
            }
            "EXPORT_REQUEST" -> "EXPORT_ACK"
            "PING" -> "PONG"
            else -> "UNKNOWN"
        }
    }
}
