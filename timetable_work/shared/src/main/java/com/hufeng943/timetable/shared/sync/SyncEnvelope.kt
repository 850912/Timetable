package com.hufeng943.timetable.shared.sync

import kotlinx.serialization.Serializable

@Serializable
data class SyncRecordPayload(
    val sourceRecordId: Long,
    val entityId: Long,
    val entityType: String,
    val operation: String,
    val revision: Long,
    val updatedAt: Long,
    val deviceId: String,
    val payloadJson: String,
)

@Serializable
data class SyncEnvelope(
    val protocolVersion: Int = 1,
    val requestId: String,
    val sourceDeviceId: String,
    val records: List<SyncRecordPayload>,
)

@Serializable
data class SyncAck(
    val protocolVersion: Int = 1,
    val requestId: String,
    val sourceDeviceId: String,
    val appliedRecordIds: List<Long>,
    val records: List<SyncRecordPayload> = emptyList(),
)
