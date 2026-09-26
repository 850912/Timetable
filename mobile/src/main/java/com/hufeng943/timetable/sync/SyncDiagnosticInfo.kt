package com.hufeng943.timetable.sync

/**
 * Runtime sync diagnostics.
 *
 * Used for troubleshooting China Wear OS compatibility environments.
 */
data class SyncDiagnosticInfo(
    val environment: String,
    val transport: String,
    val googleWearAvailable: Boolean,
    val lastError: String? = null
)
