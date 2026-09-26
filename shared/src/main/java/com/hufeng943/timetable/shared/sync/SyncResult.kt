package com.hufeng943.timetable.shared.sync

sealed interface SyncResult {
    data object Success : SyncResult
    data class Failed(val message: String, val cause: Throwable? = null) : SyncResult
}
