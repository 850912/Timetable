package com.hufeng943.timetable.shared.sync

object SyncRetryPolicy {
    const val MAX_RETRY = 5

    fun canRetry(retryCount: Int): Boolean = retryCount < MAX_RETRY

    fun nextDelayMillis(retryCount: Int): Long {
        return (retryCount + 1).toLong() * 5000L
    }
}
