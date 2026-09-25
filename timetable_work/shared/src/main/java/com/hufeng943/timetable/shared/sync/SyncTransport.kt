package com.hufeng943.timetable.shared.sync

import com.hufeng943.timetable.shared.model.Timetable
import java.util.UUID

/**
 * Sync transport abstraction.
 *
 * Implementations can use Wear OS Data Layer, a China-compatible BLE bridge,
 * Samsung Accessory, BLE or file based transfer without changing business code.
 */
interface SyncTransport {
    val name: String

    suspend fun isAvailable(): Boolean = true

    /**
     * Sends a batch with a caller-owned request id.
     * Keeping the request id stable across retries is required for receiver-side
     * idempotency and ACK recovery.
     */
    suspend fun sendRecords(
        records: List<SyncRecordPayload>,
        requestId: String,
    ): SyncResult

    /** Compatibility entry point for older transports. */
    suspend fun sendRecords(records: List<SyncRecordPayload>): SyncResult =
        sendRecords(records, UUID.randomUUID().toString())

    /** Sends a complete timetable snapshot when supported by the transport. */
    suspend fun sendTimetableSnapshot(timetables: List<Timetable>): SyncResult =
        SyncResult.Failed("该同步通道不支持完整课表同步")

    suspend fun receive(): SyncResult
}
