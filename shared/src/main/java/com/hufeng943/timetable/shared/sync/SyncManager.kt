package com.hufeng943.timetable.shared.sync

import com.hufeng943.timetable.shared.model.Timetable

class SyncManager(
    private val transports: List<SyncTransport>
) {
    suspend fun sync(timetables: List<Timetable>): SyncResult {
        if (timetables.isEmpty()) {
            return SyncResult.Failed("没有可同步的课表")
        }

        var lastError: SyncResult.Failed? = null

        for (transport in transports) {
            if (!transport.isAvailable()) continue

            when (val result = transport.send(timetables)) {
                SyncResult.Success -> return result
                is SyncResult.Failed -> lastError = result
            }
        }

        return lastError ?: SyncResult.Failed("没有可用的同步通道")
    }

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
