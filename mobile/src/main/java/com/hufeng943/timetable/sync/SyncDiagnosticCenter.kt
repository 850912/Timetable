package com.hufeng943.timetable.sync

import android.content.Context

/**
 * Build15 diagnostic center facade.
 */
object SyncDiagnosticCenter {

    fun snapshot(context: Context): String {
        return SyncDiagnosticStore.latest(context)
            .joinToString("\n") {
                "${it.timestamp} | ${it.type} | ${it.message}"
            }
            .ifBlank { "No diagnostic events" }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(
            "sync_diagnostic_store",
            Context.MODE_PRIVATE
        ).edit().clear().apply()
    }
}
