package com.hufeng943.timetable.sync

import kotlinx.serialization.Serializable

@Serializable
enum class SyncDiagnosticType {
    SYNC,
    EXPORT,
    PROFILE_OPEN,
    ERROR
}

@Serializable
data class SyncDiagnosticEvent(
    val type: SyncDiagnosticType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
