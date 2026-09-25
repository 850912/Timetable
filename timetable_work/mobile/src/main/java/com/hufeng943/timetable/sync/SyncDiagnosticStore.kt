package com.hufeng943.timetable.sync

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Build14 lightweight local diagnostic store.
 * No account data is stored.
 */
object SyncDiagnosticStore {

    private const val PREF = "sync_diagnostic_store"
    private const val KEY = "events"

    private val json = Json { encodeDefaults = true }

    fun append(context: Context, event: SyncDiagnosticEvent) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val old = prefs.getString(KEY, "[]") ?: "[]"
        val list = runCatching {
            json.decodeFromString<List<SyncDiagnosticEvent>>(old)
        }.getOrDefault(emptyList())

        prefs.edit()
            .putString(KEY, json.encodeToString((list + event).takeLast(20)))
            .apply()
    }

    fun latest(context: Context): List<SyncDiagnosticEvent> {
        val value = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        return runCatching {
            json.decodeFromString<List<SyncDiagnosticEvent>>(value)
        }.getOrDefault(emptyList())
    }
}
