package com.hufeng943.timetable.shared.sync

import com.hufeng943.timetable.shared.data.dao.SyncRecordDao
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Coordinates pending sync records and transport delivery. */
class SyncCoordinator(
    private val dao: SyncRecordDao,
    private val manager: SyncManager
) {
    private val mutex = Mutex()

    suspend fun syncPending(): SyncResult = mutex.withLock {
        val records = dao.pending().map {
            SyncRecordPayload(
                it.id,
                it.entityId,
                it.entityType,
                it.operation,
                it.revision,
                it.updatedAt,
                it.deviceId,
                it.payloadJson
            )
        }
        if (records.isEmpty()) return SyncResult.Failed("没有待同步数据")

        return try {
            when (val result = manager.syncRecords(records)) {
                SyncResult.Success -> {
                    // Transport success only means delivery accepted.
                    // Actual synced state is confirmed by ACK with appliedRecordIds.
                    result
                }
                is SyncResult.Failed -> {
                    dao.markFailed(
                        records.map { it.id },
                        System.currentTimeMillis(),
                        result.message
                    )
                    result
                }
            }
        } catch (e: Exception) {
            dao.markFailed(
                records.map { it.id },
                System.currentTimeMillis(),
                e.message ?: "同步异常"
            )
            SyncResult.Failed(e.message ?: "同步异常", e)
        }
    }
}
