package com.hufeng943.timetable.shared.export

import com.hufeng943.timetable.shared.model.WeekPattern
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class ImporterEdgeCaseTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun csvRejectsInvalidWeekdayAndTimeInsteadOfDefaulting() {
        val csv = """
            学期,课程,教师,地点,星期,开始,结束,重复
            秋季,高数,,A101,不存在,xx:yy,09:40,每周
        """.trimIndent()

        try {
            CsvImporter.parseCsv(csv)
            fail("invalid CSV must be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("第2行"))
        }
    }

    @Test
    fun icsUnfoldsFoldedLinesAndKeepsSingleOccurrenceDateOnly() {
        val ics = """
            BEGIN:VCALENDAR
            X-WR-CALNAME:测试学期
            BEGIN:VEVENT
            SUMMARY:高等
             数学
            DTSTART:20260914T080000
            DTEND:20260914T094000
            DESCRIPTION:备注:第一节
            END:VEVENT
            END:VCALENDAR
        """.trimIndent().replace("\n", "\r\n")

        val table = IcsImporter.parseIcs(ics).single()
        val slot = table.allCourses.single().timeSlots.single()
        assertEquals("高等数学", table.allCourses.single().name)
        assertEquals(WeekPattern.DATE_ONLY, slot.recurrence)
        assertEquals(1, slot.overrides.size)
        assertEquals("2026-09-14", table.semesterStart.toString())
    }

    @Test
    fun icsConvertsUtcAndTzidToDeviceZone() {
        val utc = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:UTC课
            DTSTART:20260914T160000Z
            DTEND:20260914T170000Z
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        val utcSlot = IcsImporter.parseIcs(utc).single().allCourses.single().timeSlots.single()
        assertEquals("16:00", utcSlot.startTime.toString())

        val tzid = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:纽约课
            DTSTART;TZID=America/New_York:20260914T120000
            DTEND;TZID=America/New_York:20260914T130000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        val tzSlot = IcsImporter.parseIcs(tzid).single().allCourses.single().timeSlots.single()
        assertEquals("16:00", tzSlot.startTime.toString())
    }


    @Test
    fun icsRecognizesContiguousWeeklyOccurrencesAsRecurring() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:周课
            DTSTART:20260914T080000
            DTEND:20260914T090000
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:周课
            DTSTART:20260921T080000
            DTEND:20260921T090000
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:周课
            DTSTART:20260928T080000
            DTEND:20260928T090000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val slot = IcsImporter.parseIcs(ics).single().allCourses.single().timeSlots.single()
        assertEquals(WeekPattern.EVERY_WEEK, slot.recurrence)
    }

    @Test
    fun icsDoesNotExpandIrregularOccurrencesToEveryWeek() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:专题
            DTSTART:20260914T080000
            DTEND:20260914T090000
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:专题
            DTSTART:20261005T080000
            DTEND:20261005T090000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val slot = IcsImporter.parseIcs(ics).single().allCourses.single().timeSlots.single()
        assertEquals(WeekPattern.DATE_ONLY, slot.recurrence)
        assertEquals(2, slot.overrides.size)
    }
}
