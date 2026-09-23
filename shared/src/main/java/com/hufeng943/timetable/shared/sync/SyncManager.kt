package com.hufeng943.timetable.shared.sync

import kotlinx.coroutines.delay
import java.util.UUID

class SyncManager(
    private val transports: List<SyncTransport>
) {
    /**
     * Sends a compact incremental batch with bounded automatic retry.
     * The same request id is reused for every retry and across transport fallback,
     * allowing the receiver's processed-request table to make delivery idempotent.
     */
    suspend fun syncRecords(records: List<SyncRecordPayload>): SyncResult {
        if (records.isEmpty()) return SyncResult.Success
        val compact = compact(records)
        val requestId = UUID.randomUUID().toString()
        var retry = 0
        var lastError: SyncResult.Failed? = null

        while (SyncRetryPolicy.canRetry(retry)) {
            var anyAvailable = false
            for (transport in transports) {
                if (!transport.isAvailable()) continue
                anyAvailable = true
                when (val result = transport.sendRecords(compact, requestId)) {
                    SyncResult.Success -> return result
                    is SyncResult.Failed -> lastError = result
                }
            }

            if (!anyAvailable) {
                lastError = SyncResult.Failed("没有可用的增量同步通道")
            }
            retry++
            if (SyncRetryPolicy.canRetry(retry)) {
                delay(SyncRetryPolicy.nextDelayMillis(retry - 1))
            }
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
            )
        }
        .sortedWith(compareBy<SyncRecordPayload> { it.updatedAt }.thenBy { it.sourceRecordId })
}
