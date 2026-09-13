package com.hufeng943.timetable.shared.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class DailyScheduleSummary(
    val courseCount: Int,
    val firstStart: LocalTime?,
    val lastEnd: LocalTime?,
    val freeMinutesBetweenCourses: Int,
)

fun Timetable.dailySummary(date: LocalDate): DailyScheduleSummary {
    val intervals = resolveDate(date)
        .map { it.startTime to it.endTime }
        .filter { (start, end) -> end > start }
        .sortedBy { (start, _) -> start }

    if (intervals.isEmpty()) {
        return DailyScheduleSummary(0, null, null, 0)
    }

    var free = 0
    var previousEnd = intervals.first().second
    for ((start, end) in intervals.drop(1)) {
        val gap = start.toMinuteOfDay() - previousEnd.toMinuteOfDay()
        if (gap > 0) free += gap
        if (end > previousEnd) previousEnd = end
    }

    return DailyScheduleSummary(
        courseCount = intervals.size,
        firstStart = intervals.first().first,
        lastEnd = intervals.maxBy { it.second }.second,
        freeMinutesBetweenCourses = free,
    )
}

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
