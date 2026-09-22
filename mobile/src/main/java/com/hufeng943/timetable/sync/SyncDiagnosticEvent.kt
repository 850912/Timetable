package com.hufeng943.timetable.sync

enum class SyncDiagnosticType {
    SYNC,
    EXPORT,
    PROFILE_OPEN,
    ERROR
}

data class SyncDiagnosticEvent(
    val type: SyncDiagnosticType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
