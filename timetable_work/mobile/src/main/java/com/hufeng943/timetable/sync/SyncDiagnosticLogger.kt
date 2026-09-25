package com.hufeng943.timetable.sync

import android.util.Log

object SyncDiagnosticLogger {
    private const val TAG = "TimetableSync"

    fun logTransport(transport: String, environment: String) {
        Log.i(TAG, "environment=$environment transport=$transport")
    }

    fun logError(error: Throwable) {
        Log.e(TAG, "sync error", error)
    }
}
