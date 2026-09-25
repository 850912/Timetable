package com.hufeng943.timetable.shared.importexport

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hufeng943.timetable.shared.data.database.AppDatabase
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class ImportServiceInstrumentedTest {
    private lateinit var db: AppDatabase
    private lateinit var service: ImportService

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        service = ImportService(db, db.timetableDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun repeatedFullImportRequestIsIdempotentAndKeepsStableIds() = runBlocking {
        val snapshot = Timetable(
            semesterName = "2026秋季",
            createdAt = Instant.fromEpochMilliseconds(1_000),
            semesterStart = LocalDate(2026, 9, 14),
            semesterEnd = LocalDate(2027, 1, 31),
            syncId = "tt-stable",
            allCourses = listOf(
                Course(
                    name = "高数",
                    syncId = "course-stable",
                    timeSlots = listOf(
                        TimeSlot(
                            dayOfWeek = DayOfWeek.MONDAY,
                            startTime = LocalTime(8, 0),
                            endTime = LocalTime(9, 40),
                            syncId = "slot-stable",
                        )
                    )
                )
            )
        )

        service.importReplacingMatchesAtomic(listOf(snapshot), "full-1", "phone")
        service.importReplacingMatchesAtomic(listOf(snapshot.copy(color = 123)), "full-1", "phone")

        val ids = db.timetableDao().findTimetableIdsByIdentity("2026秋季", LocalDate(2026, 9, 14).toEpochDays())
        assertEquals(1, ids.size)
        val table = db.timetableDao().getTimetableEntityBySyncId("tt-stable")!!
        val courses = db.timetableDao().getCourseEntitiesByTimetableId(table.id)
        assertEquals(1, courses.size)
        assertEquals("course-stable", courses.single().syncId)
        val slots = db.timetableDao().getTimeSlotEntitiesByCourseId(courses.single().id)
        assertEquals("slot-stable", slots.single().syncId)
        assertEquals(-1L, table.color)
        assertTrue(db.processedSyncDao().exists("full-1"))
    }
}
