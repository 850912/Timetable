package com.hufeng943.timetable.sync

import android.content.Context

object ChinaWearDiagnosticsLogger {

    private const val PREF = "china_wear_diagnostics"

    fun record(context: Context, message: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString("last_event", message)
            .putLong("time", System.currentTimeMillis())
            .apply()

        SyncDiagnosticsReporter.recordSync(context, message)
    }

    fun last(context: Context): String {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return "${p.getString("last_event", "NONE")} @ ${p.getLong("time",0)}"
    }
}
