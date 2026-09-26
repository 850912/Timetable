package com.hufeng943.timetable.shared

import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.NextCourseEngine
import com.hufeng943.timetable.shared.model.NextCoursePhase
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class NextCourseEngineTest {
    private val shanghai = TimeZone.of("Asia/Shanghai")

    @Test
    fun resolvesCurrentCourseAndFollowingCourseFromOneState() {
        val table = table(
            TimeSlot(1, LocalTime(8, 0), LocalTime(9, 40), DayOfWeek.MONDAY),
            TimeSlot(2, LocalTime(10, 0), LocalTime(11, 0), DayOfWeek.MONDAY),
        )
        val now = LocalDate(2026, 9, 14).atTime(8, 30).toInstant(shanghai)

        val state = NextCourseEngine.resolve(listOf(table), now, shanghai)

        assertEquals(NextCoursePhase.IN_CLASS, state.phase)
        assertEquals(1L, state.current?.timeSlotId)
        assertEquals(2L, state.next?.timeSlotId)
        assertEquals(70, state.minutesRemaining)
        assertEquals(90, state.minutesUntilNext)
        assertTrue((state.currentProgress ?: 0f) in 0.29f..0.31f)
    }

    @Test
    fun resolvesBetweenClassesWithoutSurfaceSpecificRules() {
        val table = table(
            TimeSlot(1, LocalTime(8, 0), LocalTime(9, 0), DayOfWeek.MONDAY),
            TimeSlot(2, LocalTime(10, 0), LocalTime(11, 0), DayOfWeek.MONDAY),
        )
        val now = LocalDate(2026, 9, 14).atTime(9, 15).toInstant(shanghai)

        val state = NextCourseEngine.resolve(listOf(table), now, shanghai)

        assertEquals(NextCoursePhase.BETWEEN_CLASSES, state.phase)
        assertNull(state.current)
        assertEquals(2L, state.next?.timeSlotId)
        assertEquals(45, state.minutesUntilNext)
    }

    @Test
    fun noClassTodayStillFindsNextCourseAcrossWeekBoundary() {
        val table = table(
            TimeSlot(1, LocalTime(8, 0), LocalTime(9, 0), DayOfWeek.MONDAY),
        )
        val now = LocalDate(2026, 9, 18).atTime(17, 0).toInstant(shanghai) // Friday

        val state = NextCourseEngine.resolve(listOf(table), now, shanghai)

        assertEquals(NextCoursePhase.NO_CLASS_TODAY, state.phase)
        assertEquals(LocalDate(2026, 9, 21), state.next?.date)
        assertEquals(LocalTime(8, 0), state.next?.startTime)
    }

    @Test
    fun oddWeekCourseSkipsEvenWeekAndFindsFollowingOccurrence() {
        val table = table(
            TimeSlot(
                id = 1,
                startTime = LocalTime(8, 0),
                endTime = LocalTime(9, 0),
                dayOfWeek = DayOfWeek.MONDAY,
                recurrence = WeekPattern.ODD_WEEK,
            ),
        )
        val now = LocalDate(2026, 9, 14).atTime(12, 0).toInstant(shanghai) // week 2

        val state = NextCourseEngine.resolve(listOf(table), now, shanghai)

        assertEquals(NextCoursePhase.NO_CLASS_TODAY, state.phase)
        assertEquals(LocalDate(2026, 9, 21), state.next?.date)
    }

    @Test
    fun timezoneControlsWhichLocalCourseIsCurrent() {
        val table = table(
            TimeSlot(1, LocalTime(8, 0), LocalTime(9, 0), DayOfWeek.MONDAY),
        )
        val localInstant = LocalDate(2026, 9, 14).atTime(8, 30).toInstant(shanghai)

        val shanghaiState = NextCourseEngine.resolve(listOf(table), localInstant, shanghai)
        val utcState = NextCourseEngine.resolve(listOf(table), localInstant, TimeZone.UTC)

        assertEquals(NextCoursePhase.IN_CLASS, shanghaiState.phase)
        assertTrue(utcState.phase != NextCoursePhase.IN_CLASS)
    }

    @Test
    fun emptyTimetableListIsExplicitNoScheduleState() {
        val now: Instant = LocalDate(2026, 9, 14).atTime(8, 30).toInstant(shanghai)
        val state = NextCourseEngine.resolve(emptyList(), now, shanghai)
        assertEquals(NextCoursePhase.NO_SCHEDULE, state.phase)
        assertNull(state.current)
        assertNull(state.next)
    }

    private fun table(vararg slots: TimeSlot): Timetable = Timetable(
        timetableId = 1,
        semesterName = "2026 秋季",
        createdAt = Instant.fromEpochMilliseconds(0),
        semesterStart = LocalDate(2026, 9, 7),
        semesterEnd = LocalDate(2027, 1, 31),
        allCourses = slots.mapIndexed { index, slot ->
            Course(
                id = (index + 1).toLong(),
                name = "课程${index + 1}",
                location = "A${101 + index}",
                timeSlots = listOf(slot),
            )
        },
    )
}
