package com.hufeng943.timetable.shared.export

import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.time.Clock

class ExportRoundTripTest {
    private fun timetable(name: String, courseName: String): Timetable = Timetable(
        timetableId = 0,
        semesterName = name,
        createdAt = Clock.System.now(),
        semesterStart = LocalDate(2026, 9, 1),
        semesterEnd = LocalDate(2026, 12, 31),
        allCourses = listOf(
            Course(
                name = courseName,
                timeSlots = listOf(
                    TimeSlot(
                        dayOfWeek = DayOfWeek.MONDAY,
                        startTime = LocalTime(8, 0),
                        endTime = LocalTime(9, 0)
                    )
                )
            )
        )
    )

    @Test
    fun jsonBackupPreservesMultipleTimetables() {
        val source = listOf(
            timetable("大三上", "高等数学"),
            timetable("大三下", "大学物理")
        )
        val output = ByteArrayOutputStream()

        BackupManager.backup(output, source)
        val restored = BackupManager.restore(ByteArrayInputStream(output.toByteArray()))

        assertEquals(2, restored.size)
        assertEquals(source.map { it.semesterName }, restored.map { it.semesterName })
        assertEquals(
            source.map { it.allCourses.first().name },
            restored.map { it.allCourses.first().name }
        )
    }

    @Test
    fun csvAndIcsContainAllTimetables() {
        val source = listOf(
            timetable("A 学期", "课程 A"),
            timetable("B 学期", "课程 B")
        )

        val csv = ByteArrayOutputStream()
        CsvExporter.streamCsv(csv, source)
        val csvText = csv.toString(StandardCharsets.UTF_8.name())
        assertTrue(csvText.contains("A 学期"))
        assertTrue(csvText.contains("B 学期"))

        val ics = ByteArrayOutputStream()
        IcsExporter.streamIcs(ics, source)
        val icsText = ics.toString(StandardCharsets.UTF_8.name())
        assertTrue(icsText.contains("学期: A 学期"))
        assertTrue(icsText.contains("学期: B 学期"))
    }
}
