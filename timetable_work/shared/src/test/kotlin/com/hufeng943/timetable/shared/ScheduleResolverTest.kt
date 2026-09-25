package com.hufeng943.timetable.shared

import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.ScheduleOverride
import com.hufeng943.timetable.shared.model.ScheduleOverrideType
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import com.hufeng943.timetable.shared.model.resolveDate
import com.hufeng943.timetable.shared.model.weekNumberFor
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class ScheduleResolverTest {
    private fun table(slot: TimeSlot) = Timetable(
        timetableId = 1,
        semesterName = "Test",
        createdAt = Instant.fromEpochMilliseconds(0),
        semesterStart = LocalDate(2026, 9, 7), // Monday
        semesterEnd = LocalDate(2026, 10, 31),
        allCourses = listOf(Course(id = 10, name = "Math", location = "A101", timeSlots = listOf(slot))),
    )

    @Test fun weekNumbersAreSemesterRelative() {
        val t = table(TimeSlot())
        assertEquals(1, t.weekNumberFor(LocalDate(2026, 9, 7)))
        assertEquals(2, t.weekNumberFor(LocalDate(2026, 9, 14)))
    }

    @Test fun cancelledOverrideRemovesOccurrence() {
        val slot = TimeSlot(
            id = 1,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime(8, 0),
            endTime = LocalTime(9, 0),
            overrides = listOf(ScheduleOverride(LocalDate(2026, 9, 14), ScheduleOverrideType.CANCELLED)),
        )
        assertTrue(table(slot).resolveDate(LocalDate(2026, 9, 14)).isEmpty())
    }

    @Test fun extraOverrideCreatesOneOffClass() {
        val slot = TimeSlot(
            id = 1,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime(8, 0),
            endTime = LocalTime(9, 0),
            recurrence = WeekPattern.ODD_WEEK,
            overrides = listOf(
                ScheduleOverride(
                    date = LocalDate(2026, 9, 16),
                    type = ScheduleOverrideType.EXTRA,
                    startTime = LocalTime(14, 0),
                    endTime = LocalTime(15, 0),
                    location = "B202",
                )
            ),
        )
        val resolved = table(slot).resolveDate(LocalDate(2026, 9, 16))
        assertEquals(1, resolved.size)
        assertEquals(LocalTime(14, 0), resolved.single().startTime)
        assertEquals("B202", resolved.single().location)
    }

    @Test fun modifiedOverrideReplacesTimeAndLocation() {
        val slot = TimeSlot(
            id = 1,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime(8, 0),
            endTime = LocalTime(9, 0),
            overrides = listOf(
                ScheduleOverride(
                    date = LocalDate(2026, 9, 14),
                    type = ScheduleOverrideType.MODIFIED,
                    startTime = LocalTime(10, 0),
                    endTime = LocalTime(11, 0),
                    location = "C303",
                )
            ),
        )
        val resolved = table(slot).resolveDate(LocalDate(2026, 9, 14)).single()
        assertEquals(LocalTime(10, 0), resolved.startTime)
        assertEquals(LocalTime(11, 0), resolved.endTime)
        assertEquals("C303", resolved.location)
    }

    @Test fun invalidLegacyOverrideIsIgnored() {
        val slot = TimeSlot(
            id = 1,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime(8, 0),
            endTime = LocalTime(9, 0),
            overrides = listOf(
                ScheduleOverride(
                    date = LocalDate(2026, 9, 14),
                    type = ScheduleOverrideType.MODIFIED,
                    startTime = LocalTime(12, 0),
                    endTime = LocalTime(11, 0),
                )
            ),
        )
        assertTrue(table(slot).resolveDate(LocalDate(2026, 9, 14)).isEmpty())
    }

}
