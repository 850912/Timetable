package com.hufeng943.timetable.shared.sync

/**
 * Sync transport abstraction.
 *
 * Implementations can use Wear OS Data Layer, Samsung Accessory,
 * BLE or file based transfer without changing business code.
 */
interface SyncTransport {
    val name: String

    suspend fun isAvailable(): Boolean = true

    suspend fun sendRecords(records: List<SyncRecordPayload>): SyncResult = SyncResult.Failed("该同步通道不支持增量同步")

    suspend fun receive(): SyncResult
}
