package com.hufeng943.timetable.sync

import android.content.Context

object SyncDiagnosticsReporter {

    fun recordSync(context: Context, message: String) {
        SyncDiagnosticStore.append(
            context,
            SyncDiagnosticEvent(
                SyncDiagnosticType.SYNC,
                message
            )
        )
    }

    fun recordExport(context: Context, message: String) {
        SyncDiagnosticStore.append(
            context,
            SyncDiagnosticEvent(
                SyncDiagnosticType.EXPORT,
                message
            )
        )
    }

    fun recordError(context: Context, message: String) {
        SyncDiagnosticStore.append(
            context,
            SyncDiagnosticEvent(
                SyncDiagnosticType.ERROR,
                message
            )
        )
    }
}
