package com.hufeng943.timetable.shared.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * A date-specific exception for a recurring time slot.
 *
 * CANCELLED removes the occurrence on [date]. MODIFIED replaces the regular
 * occurrence on that date. EXTRA creates a one-off occurrence even when the
 * weekly recurrence would not normally match that date.
 */
@Serializable
data class ScheduleOverride(
    val date: LocalDate,
    val type: ScheduleOverrideType,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val location: String? = null,
    val remark: String? = null,
)

@Serializable
enum class ScheduleOverrideType {
    CANCELLED,
    MODIFIED,
    EXTRA,
}
