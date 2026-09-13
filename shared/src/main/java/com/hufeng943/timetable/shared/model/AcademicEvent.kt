package com.hufeng943.timetable.shared.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
enum class AcademicEventType {
    ASSIGNMENT,
    EXAM,
    LAB,
    OTHER,
}

/** A one-off academic task or event attached to a timetable. */
@Serializable
data class AcademicEvent(
    val id: Long = 0,
    val title: String = "",
    val type: AcademicEventType = AcademicEventType.OTHER,
    val date: LocalDate,
    val time: LocalTime? = null,
    val courseName: String? = null,
    val location: String? = null,
    val note: String? = null,
    /** null disables the reminder; otherwise minutes before [date]/[time]. */
    val reminderMinutesBefore: Int? = null,
    val completed: Boolean = false,
)
