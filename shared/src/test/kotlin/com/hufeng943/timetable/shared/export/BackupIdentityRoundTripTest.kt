package com.hufeng943.timetable.shared.export

import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.time.Instant

class BackupIdentityRoundTripTest {
    @Test
    fun backupPreservesStableSyncIds() {
        val source = Timetable(
            semesterName = "秋季",
            createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
            semesterStart = LocalDate(2026, 9, 14),
            syncId = "tt-sync",
            allCourses = listOf(
                Course(
                    name = "高数",
                    syncId = "course-sync",
                    timeSlots = listOf(
                        TimeSlot(
                            startTime = LocalTime(8, 0),
                            endTime = LocalTime(9, 40),
                            dayOfWeek = DayOfWeek.MONDAY,
                            recurrence = WeekPattern.EVERY_WEEK,
                            syncId = "slot-sync",
                        )
                    )
                )
            )
        )
        val out = ByteArrayOutputStream()
        BackupManager.backup(out, listOf(source))
        val restored = BackupManager.restore(ByteArrayInputStream(out.toByteArray())).single()
        assertEquals("tt-sync", restored.syncId)
        assertEquals("course-sync", restored.allCourses.single().syncId)
        assertEquals("slot-sync", restored.allCourses.single().timeSlots.single().syncId)
    }
}
