package com.hufeng943.timetable.reminder

import android.content.Context

object ReminderSettings {
    private const val PREFS = "course_reminders"
    private const val ENABLED = "enabled"
    private const val OFFSET = "offset_minutes"
    private const val SCHEDULED_CODES = "scheduled_request_codes"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(context: Context): Boolean = prefs(context).getBoolean(ENABLED, false)
    fun offsetMinutes(context: Context): Int = prefs(context).getInt(OFFSET, 10).coerceIn(0, 120)

    fun save(context: Context, enabled: Boolean, offsetMinutes: Int) {
        prefs(context).edit()
            .putBoolean(ENABLED, enabled)
            .putInt(OFFSET, offsetMinutes.coerceIn(0, 120))
            .apply()
    }

    fun scheduledRequestCodes(context: Context): Set<Int> =
        prefs(context).getStringSet(SCHEDULED_CODES, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    fun saveScheduledRequestCodes(context: Context, codes: Set<Int>) {
        prefs(context).edit().putStringSet(SCHEDULED_CODES, codes.map(Int::toString).toSet()).apply()
    }
}
