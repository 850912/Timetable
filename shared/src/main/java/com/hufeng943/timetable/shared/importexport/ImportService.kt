package com.hufeng943.timetable.shared.importexport

import androidx.room.withTransaction
import com.hufeng943.timetable.shared.data.dao.TimetableDao
import com.hufeng943.timetable.shared.data.database.AppDatabase
import com.hufeng943.timetable.shared.data.mappers.toCourseEntity
import com.hufeng943.timetable.shared.data.mappers.toTimeSlotEntity
import com.hufeng943.timetable.shared.data.mappers.toTimetableEntity
import com.hufeng943.timetable.shared.model.Timetable
import javax.inject.Inject

class ImportService @Inject constructor(
    private val db: AppDatabase,
    private val dao: TimetableDao
) {
    suspend fun importAtomic(timetables: List<Timetable>) {
        db.withTransaction {
            timetables.forEach { insertTimetableGraph(it) }
        }
    }

    /**
     * Phone initiated sync should be repeatable without creating duplicates on Wear.
     * A timetable is treated as the same logical timetable when its name and start date match.
     */
    suspend fun importReplacingMatchesAtomic(timetables: List<Timetable>) {
        db.withTransaction {
            timetables.forEach { timetable ->
                dao.findTimetableIdByIdentity(
                    timetable.semesterName,
                    timetable.semesterStart.toEpochDays().toLong()
                )?.let { dao.deleteTimetableById(it) }
                insertTimetableGraph(timetable)
            }
        }
    }

    private suspend fun insertTimetableGraph(tt: Timetable) {
        val newTtId = dao.insertTimetable(tt.toTimetableEntity().copy(id = 0))
        for (course in tt.allCourses) {
            val newCourseId = dao.insertCourse(course.toCourseEntity(newTtId).copy(id = 0))
            for (slot in course.timeSlots) {
                dao.insertTimeSlot(slot.toTimeSlotEntity(newCourseId).copy(id = 0))
            }
        }
    }
}
