package com.hufeng943.timetable.shared.sync

import androidx.room.withTransaction
import com.hufeng943.timetable.shared.data.database.AppDatabase
import com.hufeng943.timetable.shared.data.entities.CourseEntity
import com.hufeng943.timetable.shared.data.entities.SyncTombstoneEntity
import com.hufeng943.timetable.shared.data.entities.TimeSlotEntity
import com.hufeng943.timetable.shared.data.entities.TimetableEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/** Applies remote records without generating local SyncRecords. */
class SyncApplier(private val db: AppDatabase) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun apply(records: List<SyncRecordPayload>): List<Long> = db.withTransaction {
        val applied = mutableListOf<Long>()
        records.sortedWith(compareBy<SyncRecordPayload> { entityOrder(it.entityType) }.thenBy { it.updatedAt }.thenBy { it.sourceRecordId }).forEach { record ->
            if (applyOne(record)) applied += record.sourceRecordId
        }
        applied
    }

    private suspend fun applyOne(record: SyncRecordPayload): Boolean {
        val root = json.parseToJsonElement(record.payloadJson).jsonObject
        val syncId = root["syncId"]?.jsonPrimitive?.content ?: return false
        if (syncId.isBlank()) return false
        val tombstone = db.syncTombstoneDao().find(syncId, record.entityType)
        if (tombstone != null && compare(record.revision, record.updatedAt, record.deviceId, tombstone.revision, tombstone.updatedAt, tombstone.deviceId) <= 0) return true

        return when (record.entityType) {
            SyncEntityType.TIMETABLE -> applyTimetable(record, root, syncId)
            SyncEntityType.COURSE -> applyCourse(record, root, syncId)
            SyncEntityType.TIME_SLOT -> applySlot(record, root, syncId)
            else -> false
        }
    }

    private suspend fun applyTimetable(r: SyncRecordPayload, o: kotlinx.serialization.json.JsonObject, syncId: String): Boolean {
        val dao = db.timetableDao(); val existing = dao.getTimetableEntityBySyncId(syncId)
        if (existing != null && !wins(r, existing.revision, existing.updatedAt, existing.modifiedBy)) return true
        if (r.operation == SyncOperation.DELETE) {
            if (existing != null) dao.markTimetableDeleted(existing.id, r.updatedAt, r.revision, r.deviceId, r.updatedAt)
            else db.syncTombstoneDao().upsert(SyncTombstoneEntity(syncId, r.entityType, r.revision, r.updatedAt, r.deviceId))
            return true
        }
        val entity = TimetableEntity(0, syncId, o.str("semesterName"), o.long("createdAtMillis"), o.long("semesterStartEpochDay"), o.longOrNull("semesterEndEpochDay"), o.long("color"), r.updatedAt, r.revision, r.deviceId, null)
        if (existing == null) dao.insertTimetable(entity) else dao.upsertTimetable(entity.copy(id = existing.id))
        db.syncTombstoneDao().delete(syncId, r.entityType); return true
    }

    private suspend fun applyCourse(r: SyncRecordPayload, o: kotlinx.serialization.json.JsonObject, syncId: String): Boolean {
        val dao = db.timetableDao(); val existing = dao.getCourseEntityBySyncId(syncId)
        if (existing != null && !wins(r, existing.revision, existing.updatedAt, existing.modifiedBy)) return true
        if (r.operation == SyncOperation.DELETE) {
            if (existing != null) dao.markCourseDeleted(existing.id, r.updatedAt, r.revision, r.deviceId, r.updatedAt)
            else db.syncTombstoneDao().upsert(SyncTombstoneEntity(syncId, r.entityType, r.revision, r.updatedAt, r.deviceId))
            return true
        }
        val parentSync = o.str("timetableSyncId")
        val parent = dao.getTimetableEntityBySyncId(parentSync) ?: return false
        val entity = CourseEntity(0, syncId, parent.id, o.str("name"), o.strOrNull("location"), o.long("color"), o.strOrNull("teacher"), r.updatedAt, r.revision, r.deviceId, null)
        if (existing == null) dao.insertCourse(entity) else dao.upsertCourse(entity.copy(id = existing.id))
        db.syncTombstoneDao().delete(syncId, r.entityType); return true
    }

    private suspend fun applySlot(r: SyncRecordPayload, o: kotlinx.serialization.json.JsonObject, syncId: String): Boolean {
        val dao = db.timetableDao(); val existing = dao.getTimeSlotEntityBySyncId(syncId)
        if (existing != null && !wins(r, existing.revision, existing.updatedAt, existing.modifiedBy)) return true
        if (r.operation == SyncOperation.DELETE) {
            if (existing != null) dao.markTimeSlotDeleted(existing.id, r.updatedAt, r.revision, r.deviceId, r.updatedAt)
            else db.syncTombstoneDao().upsert(SyncTombstoneEntity(syncId, r.entityType, r.revision, r.updatedAt, r.deviceId))
            return true
        }
        val parent = dao.getCourseEntityBySyncId(o.str("courseSyncId")) ?: return false
        val entity = TimeSlotEntity(
            id = 0,
            syncId = syncId,
            courseId = parent.id,
            dayOfWeek = o.int("dayOfWeek"),
            startMinute = o.int("startMinute"),
            endMinute = o.int("endMinute"),
            recurrence = o.int("recurrence"),
            remark = o.strOrNull("remark"),
            overridesJson = o.strOrNull("overridesJson") ?: "[]",
            updatedAt = r.updatedAt,
            revision = r.revision,
            modifiedBy = r.deviceId,
            deletedAt = null,
        )
        if (existing == null) dao.insertTimeSlot(entity) else dao.upsertTimeSlot(entity.copy(id = existing.id))
        db.syncTombstoneDao().delete(syncId, r.entityType); return true
    }

    private fun entityOrder(type: String) = when (type) {
        SyncEntityType.TIMETABLE -> 0
        SyncEntityType.COURSE -> 1
        SyncEntityType.TIME_SLOT -> 2
        else -> 3
    }

    private fun wins(r: SyncRecordPayload, rev: Long, updated: Long, device: String) = compare(r.revision, r.updatedAt, r.deviceId, rev, updated, device) > 0
    private fun compare(aRev: Long, aTime: Long, aDev: String, bRev: Long, bTime: Long, bDev: String): Int = when {
        aRev != bRev -> aRev.compareTo(bRev)
        aTime != bTime -> aTime.compareTo(bTime)
        else -> aDev.compareTo(bDev)
    }

    private fun kotlinx.serialization.json.JsonObject.str(k: String) = this[k]?.jsonPrimitive?.content ?: error("missing $k")
    private fun kotlinx.serialization.json.JsonObject.strOrNull(k: String) = this[k]?.jsonPrimitive?.content
    private fun kotlinx.serialization.json.JsonObject.long(k: String) = this[k]?.jsonPrimitive?.long ?: error("missing $k")
    private fun kotlinx.serialization.json.JsonObject.longOrNull(k: String) = this[k]?.jsonPrimitive?.long
    private fun kotlinx.serialization.json.JsonObject.int(k: String) = this[k]?.jsonPrimitive?.content?.toInt() ?: error("missing $k")
}
