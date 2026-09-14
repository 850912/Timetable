package com.hufeng943.timetable.shared.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

/** A concrete occurrence after weekly recurrence and date overrides are applied. */
data class ResolvedSchedule(
    val timetable: Timetable,
    val course: Course,
    val timeSlot: TimeSlot,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val location: String?,
    val override: ScheduleOverride? = null,
)

fun Timetable.weekNumberFor(date: LocalDate): Int? {
    if (date < semesterStart || (semesterEnd != null && date > semesterEnd)) return null
    val semesterOffset = (semesterStart.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber).mod(7)
    val semesterMonday = semesterStart.minus(semesterOffset.toLong(), DateTimeUnit.DAY)
    val dateOffset = (date.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber).mod(7)
    val dateMonday = date.minus(dateOffset.toLong(), DateTimeUnit.DAY)
    val daysBetween = dateMonday.toEpochDays() - semesterMonday.toEpochDays()
    if (daysBetween < 0) return null
    return (daysBetween / 7 + 1).toInt()
}

fun TimeSlot.matchesWeek(week: Int): Boolean = when (recurrence) {
    WeekPattern.EVERY_WEEK -> true
    WeekPattern.ODD_WEEK -> week % 2 == 1
    WeekPattern.EVEN_WEEK -> week % 2 == 0
    WeekPattern.DATE_ONLY -> false
}

fun Timetable.resolveDate(date: LocalDate): List<ResolvedSchedule> {
    val week = weekNumberFor(date) ?: return emptyList()
    return buildList {
        for (course in allCourses) {
            for (slot in course.timeSlots) {
                val override = slot.overrides.lastOrNull { override ->
                    date >= override.date && (override.endDate?.let { date <= it } ?: (date == override.date))
                }
                if (override?.type == ScheduleOverrideType.CANCELLED) continue

                val regular = slot.dayOfWeek == date.dayOfWeek && slot.matchesWeek(week)
                val forced = override?.type == ScheduleOverrideType.EXTRA
                // MODIFIED changes a regular occurrence; it must not create lessons on every day
                // when used as a date-range override. EXTRA is the only forcing override.
                if (!regular && !forced) continue

                val start = override?.startTime ?: slot.startTime ?: continue
                val end = override?.endTime ?: slot.endTime ?: continue
                if (end <= start) continue
                add(
                    ResolvedSchedule(
                        timetable = this@resolveDate,
                        course = course,
                        timeSlot = slot,
                        date = date,
                        startTime = start,
                        endTime = end,
                        location = override?.location ?: course.location,
                        override = override,
                    )
                )
            }
        }
    }.sortedBy { it.startTime }
}
