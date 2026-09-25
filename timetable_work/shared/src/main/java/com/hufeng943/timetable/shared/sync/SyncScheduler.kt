package com.hufeng943.timetable.shared.sync

/**
 * Lightweight scheduler abstraction.
 * Keeps sync triggering independent from UI and allows WorkManager integration later.
 */
class SyncScheduler(
    private val coordinator: SyncCoordinator
) {
    suspend fun runOnce(): SyncResult {
        return coordinator.syncPending()
    }
}
