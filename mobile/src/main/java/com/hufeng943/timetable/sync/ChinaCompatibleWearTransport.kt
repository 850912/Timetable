package com.hufeng943.timetable.sync

import android.content.Context
import com.hufeng943.timetable.shared.sync.*

/**
 * Build11 China Wear transport.
 *
 * This layer deliberately does not identify Xiaomi/OPPO/etc.
 * It routes by capability and delegates to ChinaWearBridge.
 */
class ChinaCompatibleWearTransport(
    private val context: Context
) : SyncTransport {

    override val name: String = "China Wear Compatibility"

    private val bridge = ChinaWearBridge(context)

    override suspend fun isAvailable(): Boolean {
        return bridge.isReady()
    }

    override suspend fun sendRecords(
        records: List<SyncRecordPayload>
    ): SyncResult {
        return runCatching {
            bridge.enqueueSync(records)
            SyncResult.Success
        }.getOrElse {
            SyncResult.Failed("China Wear transport failed: ${it.message}")
        }
    }

    override suspend fun receive(): SyncResult {
        return runCatching {
            bridge.pollIncoming()
            SyncResult.Success
        }.getOrElse {
            SyncResult.Failed("China Wear receive failed: ${it.message}")
        }
    }
}
