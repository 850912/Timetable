package com.hufeng943.timetable.shared.importexport

import androidx.room.withTransaction
import com.hufeng943.timetable.shared.data.dao.TimetableDao
import com.hufeng943.timetable.shared.data.database.AppDatabase
import com.hufeng943.timetable.shared.data.mappers.toAcademicEventEntity
import com.hufeng943.timetable.shared.data.mappers.toCourseEntity
import com.hufeng943.timetable.shared.data.mappers.toTimeSlotEntity
import com.hufeng943.timetable.shared.data.mappers.toTimetableEntity
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.data.entities.ProcessedSyncEntity
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
    suspend fun importReplacingMatchesAtomic(
        timetables: List<Timetable>,
        requestId: String? = null,
        sourceDeviceId: String? = null,
    ) {
        db.withTransaction {
            val normalizedRequestId = requestId?.takeIf { it.isNotBlank() }
            if (normalizedRequestId != null && db.processedSyncDao().exists(normalizedRequestId)) {
                return@withTransaction
            }
            timetables.forEach { timetable ->
                val identityMatches = dao.findTimetableIdsByIdentity(
                    timetable.semesterName,
                    timetable.semesterStart.toEpochDays()
                )
                val syncMatch = timetable.syncId?.takeIf { it.isNotBlank() }
                    ?.let { dao.getTimetableEntityBySyncId(it)?.id }
                val matches = (identityMatches + listOfNotNull(syncMatch)).distinct()
                if (matches.isNotEmpty()) {
                    // Clean every stale copy, not only the first one. Older builds could
                    // accumulate identical snapshots when manual full sync was repeated.
                    matches.forEach { timetableId ->
                        // Snapshot replacement is idempotent state replacement, not a domain delete.
                        // Hard-delete the old graph (children cascade) to avoid unbounded history rows
                        // and to allow the incoming stable syncIds to be preserved.
                        dao.hardDeleteTimetableForImport(timetableId)
                    }
                }
                insertTimetableGraph(timetable)
            }
            if (normalizedRequestId != null) {
                db.processedSyncDao().insert(
                    ProcessedSyncEntity(
                        requestId = normalizedRequestId,
                        deviceId = sourceDeviceId?.takeIf { it.isNotBlank() } ?: "FULL_IMPORT",
                        processedAt = System.currentTimeMillis(),
                    )
                )
                db.processedSyncDao().cleanup(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
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
        for (event in tt.events) {
            dao.insertAcademicEvent(event.toAcademicEventEntity(newTtId).copy(id = 0))
        }
    }
}
