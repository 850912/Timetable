package com.hufeng943.timetable.shared

import com.hufeng943.timetable.shared.export.BackupManager
import com.hufeng943.timetable.shared.export.CsvExporter
import com.hufeng943.timetable.shared.export.CsvImporter
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.ScheduleOverride
import com.hufeng943.timetable.shared.model.ScheduleOverrideType
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.time.Instant

class ExportRoundTripTest {
    private val source = Timetable(
        timetableId = 7,
        semesterName = "2026 秋季",
        createdAt = Instant.fromEpochMilliseconds(1),
        semesterStart = LocalDate(2026, 9, 7),
        semesterEnd = null,
        allCourses = listOf(
            Course(
                id = 9,
                name = "高等数学",
                location = "A101",
                timeSlots = listOf(
                    TimeSlot(
                        id = 11,
                        dayOfWeek = DayOfWeek.MONDAY,
                        startTime = LocalTime(8, 0),
                        endTime = LocalTime(9, 40),
                        overrides = listOf(
                            ScheduleOverride(
                                date = LocalDate(2026, 9, 14),
                                type = ScheduleOverrideType.MODIFIED,
                                startTime = LocalTime(10, 0),
                                endTime = LocalTime(11, 40),
                                location = "B202",
                            )
                        ),
                    )
                ),
            )
        ),
    )

    @Test fun backupPreservesOverrides() {
        val output = ByteArrayOutputStream()
        BackupManager.backup(output, listOf(source))
        val restored = BackupManager.restore(ByteArrayInputStream(output.toByteArray())).single()
        assertEquals(source.semesterStart, restored.semesterStart)
        assertNull(restored.semesterEnd)
        assertEquals(source.allCourses.single().timeSlots.single().overrides, restored.allCourses.single().timeSlots.single().overrides)
    }

    @Test fun csvPreservesSemesterAndOverrides() {
        val output = ByteArrayOutputStream()
        CsvExporter.streamCsv(output, listOf(source))
        val restored = CsvImporter.parseCsv(output.toString(Charsets.UTF_8.name())).single()
        assertEquals(source.semesterStart, restored.semesterStart)
        assertNull(restored.semesterEnd)
        assertEquals(source.allCourses.single().timeSlots.single().overrides, restored.allCourses.single().timeSlots.single().overrides)
    }
}
