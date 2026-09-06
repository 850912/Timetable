package com.hufeng943.timetable.shared.data.repository

import androidx.room.withTransaction
import com.hufeng943.timetable.shared.data.dao.TimetableDao
import com.hufeng943.timetable.shared.data.database.AppDatabase
import com.hufeng943.timetable.shared.data.entities.CourseEntity
import com.hufeng943.timetable.shared.data.entities.SyncRecordEntity
import com.hufeng943.timetable.shared.data.entities.TimeSlotEntity
import com.hufeng943.timetable.shared.data.entities.TimetableEntity
import com.hufeng943.timetable.shared.data.mappers.toCourse
import com.hufeng943.timetable.shared.data.mappers.toCourseEntity
import com.hufeng943.timetable.shared.data.mappers.toTimeSlot
import com.hufeng943.timetable.shared.data.mappers.toTimeSlotEntity
import com.hufeng943.timetable.shared.data.mappers.toTimetable
import com.hufeng943.timetable.shared.data.mappers.toTimetableEntity
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.sync.SyncEntityType
import com.hufeng943.timetable.shared.sync.SyncOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class TimetableRepositoryImpl(
    private val db: AppDatabase,
    private val deviceId: String,
) : TimetableRepository {
    private val dao: TimetableDao = db.timetableDao()

    override suspend fun upsertTimetable(timetable: Timetable): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val existing = timetable.timetableId.takeIf { it != 0L }?.let { dao.getTimetableEntityById(it) }
        val entity = timetable.toTimetableEntity().let { candidate ->
            if (existing == null) {
                candidate.copy(
                    id = 0,
                    syncId = UUID.randomUUID().toString(),
                    updatedAt = now,
                    revision = 1,
                    modifiedBy = deviceId,
                    deletedAt = null,
                )
            } else {
                candidate.copy(
                    id = existing.id,
                    syncId = existing.syncId.ifBlank { UUID.randomUUID().toString() },
                    updatedAt = now,
                    revision = existing.revision + 1,
                    modifiedBy = deviceId,
                    deletedAt = null,
                )
            }
        }

        val id = if (entity.id == 0L) {
            dao.insertTimetable(entity)
        } else {
            dao.upsertTimetable(entity)
            entity.id
        }
        val storedEntity = if (entity.id == 0L) entity.copy(id = id) else entity

        db.syncRecordDao().insert(
            SyncRecordEntity(
                entityId = id,
                entityType = SyncEntityType.TIMETABLE,
                operation = SyncOperation.UPSERT,
                revision = entity.revision,
                updatedAt = entity.updatedAt,
                deviceId = deviceId,
                payloadJson = storedEntity.toSyncPayloadJson(),
            )
        )
        id
    }

    override suspend fun upsertCourse(course: Course, timetableId: Long): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val existing = course.id.takeIf { it != 0L }?.let { dao.getCourseEntityById(it) }
        val entity = course.toCourseEntity(timetableId).let { candidate ->
            if (existing == null) {
                candidate.copy(
                    id = 0,
                    syncId = UUID.randomUUID().toString(),
                    updatedAt = now,
                    revision = 1,
                    modifiedBy = deviceId,
                    deletedAt = null,
                )
            } else {
                candidate.copy(
                    id = existing.id,
                    syncId = existing.syncId.ifBlank { UUID.randomUUID().toString() },
                    updatedAt = now,
                    revision = existing.revision + 1,
                    modifiedBy = deviceId,
                    deletedAt = null,
                )
            }
        }

        val id = if (entity.id == 0L) {
            dao.insertCourse(entity)
        } else {
            dao.upsertCourse(entity)
            entity.id
        }
        val storedEntity = if (entity.id == 0L) entity.copy(id = id) else entity

        db.syncRecordDao().insert(
            SyncRecordEntity(
                entityId = id,
                entityType = SyncEntityType.COURSE,
                operation = SyncOperation.UPSERT,
                revision = entity.revision,
                updatedAt = entity.updatedAt,
                deviceId = deviceId,
                payloadJson = storedEntity.toSyncPayloadJson(),
            )
        )
        id
    }

    override suspend fun upsertTimeSlot(timeSlot: TimeSlot, courseId: Long): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val existing = timeSlot.id.takeIf { it != 0L }?.let { dao.getTimeSlotEntityById(it) }
        val entity = timeSlot.toTimeSlotEntity(courseId).let { candidate ->
            if (existing == null) {
                candidate.copy(
                    id = 0,
                    syncId = UUID.randomUUID().toString(),
                    updatedAt = now,
                    revision = 1,
                    modifiedBy = deviceId,
                    deletedAt = null,
                )
            } else {
                candidate.copy(
                    id = existing.id,
                    syncId = existing.syncId.ifBlank { UUID.randomUUID().toString() },
                    updatedAt = now,
                    revision = existing.revision + 1,
                    modifiedBy = deviceId,
                    deletedAt = null,
                )
            }
        }

        val id = if (entity.id == 0L) {
            dao.insertTimeSlot(entity)
        } else {
            dao.upsertTimeSlot(entity)
            entity.id
        }
        val storedEntity = if (entity.id == 0L) entity.copy(id = id) else entity

        db.syncRecordDao().insert(
            SyncRecordEntity(
                entityId = id,
                entityType = SyncEntityType.TIME_SLOT,
                operation = SyncOperation.UPSERT,
                revision = entity.revision,
                updatedAt = entity.updatedAt,
                deviceId = deviceId,
                payloadJson = storedEntity.toSyncPayloadJson(),
            )
        )
        id
    }

    override suspend fun deleteTimetable(timetableId: Long) = db.withTransaction {
        val existing = dao.getTimetableEntityById(timetableId) ?: return@withTransaction
        val now = System.currentTimeMillis()
        val timetableRevision = existing.revision + 1
        dao.markTimetableDeleted(
            id = existing.id,
            updatedAt = now,
            revision = timetableRevision,
            modifiedBy = deviceId,
            deletedAt = now,
        )
        db.syncRecordDao().insert(
            existing.toDeleteRecord(SyncEntityType.TIMETABLE, now, timetableRevision)
        )

        // A timetable deletion also needs tombstones for every child entity.
        // Otherwise the remote device could keep orphaned courses/slots forever.
        for (course in dao.getCourseEntitiesByTimetableId(timetableId)) {
            val courseRevision = course.revision + 1
            dao.markCourseDeleted(course.id, now, courseRevision, deviceId, now)
            db.syncRecordDao().insert(
                course.toDeleteRecord(SyncEntityType.COURSE, now, courseRevision)
            )
            for (slot in dao.getTimeSlotEntitiesByCourseId(course.id)) {
                val slotRevision = slot.revision + 1
                dao.markTimeSlotDeleted(slot.id, now, slotRevision, deviceId, now)
                db.syncRecordDao().insert(
                    slot.toDeleteRecord(SyncEntityType.TIME_SLOT, now, slotRevision)
                )
            }
        }
    }

    override suspend fun deleteCourse(courseId: Long) = db.withTransaction {
        val existing = dao.getCourseEntityById(courseId) ?: return@withTransaction
        val now = System.currentTimeMillis()
        val courseRevision = existing.revision + 1
        dao.markCourseDeleted(courseId, now, courseRevision, deviceId, now)
        db.syncRecordDao().insert(
            existing.toDeleteRecord(SyncEntityType.COURSE, now, courseRevision)
        )
        // Keep slot tombstones so a remote copy cannot resurrect deleted slots.
        for (slot in dao.getTimeSlotEntitiesByCourseId(courseId)) {
            val slotRevision = slot.revision + 1
            dao.markTimeSlotDeleted(slot.id, now, slotRevision, deviceId, now)
            db.syncRecordDao().insert(
                slot.toDeleteRecord(SyncEntityType.TIME_SLOT, now, slotRevision)
            )
        }
    }

    override suspend fun deleteTimeSlot(timeSlotId: Long) = db.withTransaction {
        val existing = dao.getTimeSlotEntityById(timeSlotId) ?: return@withTransaction
        val now = System.currentTimeMillis()
        val revision = existing.revision + 1
        dao.markTimeSlotDeleted(timeSlotId, now, revision, deviceId, now)
        db.syncRecordDao().insert(
            existing.toDeleteRecord(SyncEntityType.TIME_SLOT, now, revision)
        )
    }

    override fun getAllTimetables(): Flow<List<Timetable>> =
        dao.getTimetables().map { entities -> entities.map { it.toTimetable() } }

    override fun getTimetableById(timetableId: Long): Flow<Timetable?> =
        dao.getTimetableById(timetableId).map { it?.toTimetable() }

    override fun getCourseById(courseId: Long): Flow<Course?> =
        dao.getCourseById(courseId).map { it?.toCourse() }

    override fun getTimeSlotById(timeSlotId: Long): Flow<TimeSlot?> =
        dao.getTimeSlotById(timeSlotId).map { it?.toTimeSlot() }

    override fun getCourseByTimeSlotId(timeSlotId: Long): Flow<Course?> =
        dao.getCourseByTimeSlotId(timeSlotId).map { it?.toCourse() }

    override fun getTimetableByCourseId(courseId: Long): Flow<Timetable?> =
        dao.getTimetableByCourseId(courseId).map { it?.toTimetable() }

    private suspend fun TimetableEntity.toSyncPayloadJson(): String = buildJsonObject {
        put("id", id)
        put("syncId", syncId)
        put("semesterName", semesterName)
        put("createdAtMillis", createdAtMillis)
        put("semesterStartEpochDay", semesterStartEpochDay)
        semesterEndEpochDay?.let { put("semesterEndEpochDay", it) }
        put("color", color)
        put("updatedAt", updatedAt)
        put("revision", revision)
        put("modifiedBy", modifiedBy)
        deletedAt?.let { put("deletedAt", it) }
    }.toString()

    private suspend fun CourseEntity.toSyncPayloadJson(): String = buildJsonObject {
        put("id", id)
        put("syncId", syncId)
        put("timetableId", timetableId)
        put("timetableSyncId", dao.getTimetableEntityById(timetableId)?.syncId ?: "")
        put("name", name)
        location?.let { put("location", it) }
        put("color", color)
        teacher?.let { put("teacher", it) }
        put("updatedAt", updatedAt)
        put("revision", revision)
        put("modifiedBy", modifiedBy)
        deletedAt?.let { put("deletedAt", it) }
    }.toString()

    private suspend fun TimeSlotEntity.toSyncPayloadJson(): String = buildJsonObject {
        put("id", id)
        put("syncId", syncId)
        put("courseId", courseId)
        put("courseSyncId", dao.getCourseEntityById(courseId)?.syncId ?: "")
        put("dayOfWeek", dayOfWeek)
        put("startMinute", startMinute)
        put("endMinute", endMinute)
        put("recurrence", recurrence)
        remark?.let { put("remark", it) }
        put("updatedAt", updatedAt)
        put("revision", revision)
        put("modifiedBy", modifiedBy)
        deletedAt?.let { put("deletedAt", it) }
    }.toString()

    private suspend fun TimetableEntity.toDeleteRecord(
        type: String,
        now: Long,
        deleteRevision: Long,
    ): SyncRecordEntity = SyncRecordEntity(
        entityId = id,
        entityType = type,
        operation = SyncOperation.DELETE,
        revision = deleteRevision,
        updatedAt = now,
        deviceId = deviceId,
        payloadJson = buildJsonObject {
            put("id", id)
            put("syncId", syncId)
            put("updatedAt", now)
            put("revision", deleteRevision)
            put("modifiedBy", deviceId)
            put("deletedAt", now)
        }.toString(),
    )

    private suspend fun CourseEntity.toDeleteRecord(
        type: String,
        now: Long,
        deleteRevision: Long,
    ): SyncRecordEntity = SyncRecordEntity(
        entityId = id,
        entityType = type,
        operation = SyncOperation.DELETE,
        revision = deleteRevision,
        updatedAt = now,
        deviceId = deviceId,
        payloadJson = buildJsonObject {
            put("id", id)
            put("syncId", syncId)
            put("timetableId", timetableId)
            put("timetableSyncId", dao.getTimetableEntityById(timetableId)?.syncId ?: "")
            put("updatedAt", now)
            put("revision", deleteRevision)
            put("modifiedBy", deviceId)
            put("deletedAt", now)
        }.toString(),
    )

    private suspend fun TimeSlotEntity.toDeleteRecord(
        type: String,
        now: Long,
        deleteRevision: Long,
    ): SyncRecordEntity = SyncRecordEntity(
        entityId = id,
        entityType = type,
        operation = SyncOperation.DELETE,
        revision = deleteRevision,
        updatedAt = now,
        deviceId = deviceId,
        payloadJson = buildJsonObject {
            put("id", id)
            put("syncId", syncId)
            put("courseId", courseId)
            put("courseSyncId", dao.getCourseEntityById(courseId)?.syncId ?: "")
            put("updatedAt", now)
            put("revision", deleteRevision)
            put("modifiedBy", deviceId)
            put("deletedAt", now)
        }.toString(),
    )

}
