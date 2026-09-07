package com.hufeng943.timetable.shared.sync

class SyncManager(
    private val transports: List<SyncTransport>
) {
    suspend fun syncRecords(records: List<SyncRecordPayload>): SyncResult {
        var lastError: SyncResult.Failed? = null
        for (transport in transports) {
            if (!transport.isAvailable()) continue
            when (val result = transport.sendRecords(records)) {
                SyncResult.Success -> return result
                is SyncResult.Failed -> lastError = result
            }
        }
        return lastError ?: SyncResult.Failed("没有可用的增量同步通道")
    }
}
