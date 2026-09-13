package com.hufeng943.timetable.shared

import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.dailySummary
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class DailyScheduleSummaryTest {
    private val monday = LocalDate(2026, 9, 7)

    @Test
    fun summaryCountsClassesAndOnlyPositiveGaps() {
        val table = Timetable(
            timetableId = 1,
            semesterName = "Test",
            createdAt = Instant.fromEpochMilliseconds(0),
            semesterStart = monday,
            semesterEnd = LocalDate(2026, 12, 31),
            allCourses = listOf(
                Course(
                    id = 1,
                    name = "A",
                    timeSlots = listOf(TimeSlot(id = 1, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime(8, 0), endTime = LocalTime(9, 0))),
                ),
                Course(
                    id = 2,
                    name = "B",
                    timeSlots = listOf(TimeSlot(id = 2, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime(9, 30), endTime = LocalTime(10, 30))),
                ),
                Course(
                    id = 3,
                    name = "C",
                    timeSlots = listOf(TimeSlot(id = 3, dayOfWeek = DayOfWeek.MONDAY, startTime = LocalTime(10, 15), endTime = LocalTime(11, 0))),
                ),
            ),
        )

        val summary = table.dailySummary(monday)
        assertEquals(3, summary.courseCount)
        assertEquals(LocalTime(8, 0), summary.firstStart)
        assertEquals(LocalTime(11, 0), summary.lastEnd)
        assertEquals(30, summary.freeMinutesBetweenCourses)
    }
}
