package com.hufeng943.timetable.shared

import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.ScheduleBatchOperations
import com.hufeng943.timetable.shared.model.ScheduleOverrideType
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import com.hufeng943.timetable.shared.model.resolveDate
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class ScheduleBatchOperationsTest {
    private val start = LocalDate(2026, 9, 7)

    private fun table(slot: TimeSlot) = Timetable(
        timetableId = 1,
        semesterName = "2026 秋",
        createdAt = Instant.fromEpochMilliseconds(0),
        semesterStart = start,
        semesterEnd = LocalDate(2026, 12, 31),
        allCourses = listOf(Course(id = 2, name = "数学", timeSlots = listOf(slot))),
    )

    @Test fun dateOnlySlotOnlyResolvesSelectedDates() {
        val dates = listOf(LocalDate(2026, 9, 9), LocalDate(2026, 9, 11))
        val slot = ScheduleBatchOperations.dateOnlySlot(dates, LocalTime(10, 0), LocalTime(11, 0))
        val timetable = table(slot.copy(id = 3))

        assertEquals(WeekPattern.DATE_ONLY, slot.recurrence)
        assertEquals(1, timetable.resolveDate(dates[0]).size)
        assertEquals(1, timetable.resolveDate(dates[1]).size)
        assertTrue(timetable.resolveDate(LocalDate(2026, 9, 16)).isEmpty())
    }

    @Test fun shiftCreatesDateSpecificModification() {
        val slot = TimeSlot(
            id = 3,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime(8, 0),
            endTime = LocalTime(9, 0),
        )
        val date = LocalDate(2026, 9, 14)
        val shifted = ScheduleBatchOperations.shiftDates(slot, listOf(date), 15)
        val occurrence = table(shifted).resolveDate(date).single()

        assertEquals(LocalTime(8, 15), occurrence.startTime)
        assertEquals(LocalTime(9, 15), occurrence.endTime)
        assertEquals(ScheduleOverrideType.MODIFIED, shifted.overrides.single().type)
    }

    @Test fun dateOnlyCanBeCancelledAndRestored() {
        val date = LocalDate(2026, 9, 15)
        val original = ScheduleBatchOperations.dateOnlySlot(
            listOf(date), LocalTime(14, 0), LocalTime(15, 30), location = "A201"
        ).copy(id = 3)
        val cancelled = ScheduleBatchOperations.cancelDates(original, listOf(date))
        assertTrue(table(cancelled).resolveDate(date).isEmpty())

        val restoreDates = ScheduleBatchOperations.overrideDates(
            cancelled, date, date,
        )
        val restored = ScheduleBatchOperations.clearDates(cancelled, restoreDates)
        val occurrence = table(restored).resolveDate(date).single()
        assertEquals(LocalTime(14, 0), occurrence.startTime)
        assertEquals("A201", occurrence.location)
        assertEquals(ScheduleOverrideType.EXTRA, restored.overrides.single().type)
    }

    @Test fun matchingDatesCanTargetOnlyClassesOverlappingTimeWindow() {
        val morning = TimeSlot(
            id = 11,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime(8, 0),
            endTime = LocalTime(9, 0),
        )
        val afternoon = TimeSlot(
            id = 12,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime(14, 0),
            endTime = LocalTime(15, 0),
        )
        val timetable = Timetable(
            timetableId = 1,
            semesterName = "2026 秋",
            createdAt = Instant.fromEpochMilliseconds(0),
            semesterStart = start,
            semesterEnd = LocalDate(2026, 12, 31),
            allCourses = listOf(Course(id = 2, name = "课", timeSlots = listOf(morning, afternoon))),
        )
        val matches = ScheduleBatchOperations.matchingDates(
            timetable,
            LocalDate(2026, 9, 14),
            LocalDate(2026, 9, 14),
            LocalTime(13, 0),
            LocalTime(16, 0),
        )

        assertTrue(matches[11].isNullOrEmpty())
        assertEquals(listOf(LocalDate(2026, 9, 14)), matches[12])
    }
}
