package com.hufeng943.timetable.shared.sync

import kotlinx.coroutines.delay

class SyncManager(
    private val transports: List<SyncTransport>
) {
    /**
     * Sends a compact incremental batch with bounded automatic retry.
     * Multiple local edits of the same entity are collapsed to the newest record,
     * while different entity types remain ordered by timestamp for deterministic apply.
     */
    suspend fun syncRecords(records: List<SyncRecordPayload>): SyncResult {
        if (records.isEmpty()) return SyncResult.Success
        val compact = compact(records)
        var retry = 0
        var lastError: SyncResult.Failed? = null

        while (SyncRetryPolicy.canRetry(retry)) {
            var anyAvailable = false
            for (transport in transports) {
                if (!transport.isAvailable()) continue
                anyAvailable = true
                when (val result = transport.sendRecords(compact)) {
                    SyncResult.Success -> return result
                    is SyncResult.Failed -> lastError = result
                }
            }

            if (!anyAvailable) {
                lastError = SyncResult.Failed("没有可用的增量同步通道")
            }
            retry++
            if (SyncRetryPolicy.canRetry(retry)) delay(SyncRetryPolicy.nextDelayMillis(retry - 1))
        }
        return lastError ?: SyncResult.Failed("同步失败")
    }

    private fun compact(records: List<SyncRecordPayload>): List<SyncRecordPayload> = records
        .groupBy { it.entityType to it.entityId }
        .values
        .mapNotNull { group ->
            group.maxWithOrNull(
                compareBy<SyncRecordPayload> { it.revision }
                    .thenBy { it.updatedAt }
                    .thenBy { it.sourceRecordId }
            ) ?: return@mapNotNull null
        }
        .sortedWith(compareBy<SyncRecordPayload> { it.updatedAt }.thenBy { it.sourceRecordId })
}
