package com.hufeng943.timetable.shared.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.hufeng943.timetable.shared.data.entities.CourseEntity
import com.hufeng943.timetable.shared.data.entities.AcademicEventEntity
import com.hufeng943.timetable.shared.data.entities.TimeSlotEntity
import com.hufeng943.timetable.shared.data.entities.TimetableEntity
import com.hufeng943.timetable.shared.data.relations.CourseWithSlots
import com.hufeng943.timetable.shared.data.relations.TimetableWithCourses
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Insert
    suspend fun insertTimetable(timetable: TimetableEntity): Long

    @Insert
    suspend fun insertCourse(course: CourseEntity): Long

    @Insert
    suspend fun insertTimeSlot(slot: TimeSlotEntity): Long

    @Insert
    suspend fun insertAcademicEvent(event: AcademicEventEntity): Long

    @Upsert
    suspend fun upsertTimetable(timetable: TimetableEntity)

    @Upsert
    suspend fun upsertCourse(course: CourseEntity)

    @Upsert
    suspend fun upsertTimeSlot(slot: TimeSlotEntity)

    @Upsert
    suspend fun upsertAcademicEvent(event: AcademicEventEntity)


    @Query("UPDATE time_tables SET deletedAt = :deletedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDeleteTimetableForImport(id: Long, deletedAt: Long)

    @Query("UPDATE courses SET deletedAt = :deletedAt WHERE timetableId = :timetableId AND deletedAt IS NULL")
    suspend fun softDeleteCoursesForImport(timetableId: Long, deletedAt: Long)

    @Query("UPDATE time_slots SET deletedAt = :deletedAt WHERE courseId IN (SELECT id FROM courses WHERE timetableId = :timetableId) AND deletedAt IS NULL")
    suspend fun softDeleteTimeSlotsForImport(timetableId: Long, deletedAt: Long)

    @Query("UPDATE academic_events SET deletedAt = :deletedAt WHERE timetableId = :timetableId AND deletedAt IS NULL")
    suspend fun softDeleteAcademicEventsForImport(timetableId: Long, deletedAt: Long)

    @Query("SELECT * FROM courses WHERE timetableId = :timetableId AND deletedAt IS NULL")
    suspend fun getCourseEntitiesByTimetableId(timetableId: Long): List<CourseEntity>

    @Query("SELECT * FROM time_slots WHERE courseId = :courseId AND deletedAt IS NULL")
    suspend fun getTimeSlotEntitiesByCourseId(courseId: Long): List<TimeSlotEntity>

    @Query("SELECT * FROM academic_events WHERE timetableId = :timetableId AND deletedAt IS NULL")
    suspend fun getAcademicEventEntitiesByTimetableId(timetableId: Long): List<AcademicEventEntity>

    @Query("UPDATE time_tables SET updatedAt = :updatedAt, revision = :revision, modifiedBy = :modifiedBy, deletedAt = :deletedAt WHERE id = :id")
    suspend fun markTimetableDeleted(id: Long, updatedAt: Long, revision: Long, modifiedBy: String, deletedAt: Long)

    @Query("UPDATE courses SET updatedAt = :updatedAt, revision = :revision, modifiedBy = :modifiedBy, deletedAt = :deletedAt WHERE id = :id")
    suspend fun markCourseDeleted(id: Long, updatedAt: Long, revision: Long, modifiedBy: String, deletedAt: Long)

    @Query("UPDATE time_slots SET updatedAt = :updatedAt, revision = :revision, modifiedBy = :modifiedBy, deletedAt = :deletedAt WHERE id = :id")
    suspend fun markTimeSlotDeleted(id: Long, updatedAt: Long, revision: Long, modifiedBy: String, deletedAt: Long)

    @Query("UPDATE academic_events SET updatedAt = :updatedAt, revision = :revision, modifiedBy = :modifiedBy, deletedAt = :deletedAt WHERE id = :id")
    suspend fun markAcademicEventDeleted(id: Long, updatedAt: Long, revision: Long, modifiedBy: String, deletedAt: Long)

    @Query("SELECT * FROM time_tables WHERE syncId = :syncId LIMIT 1")
    suspend fun getTimetableEntityBySyncId(syncId: String): TimetableEntity?

    @Query("SELECT * FROM courses WHERE syncId = :syncId LIMIT 1")
    suspend fun getCourseEntityBySyncId(syncId: String): CourseEntity?

    @Query("SELECT * FROM time_slots WHERE syncId = :syncId LIMIT 1")
    suspend fun getTimeSlotEntityBySyncId(syncId: String): TimeSlotEntity?

    @Query("SELECT * FROM academic_events WHERE syncId = :syncId LIMIT 1")
    suspend fun getAcademicEventEntityBySyncId(syncId: String): AcademicEventEntity?

    @Query("SELECT * FROM time_tables WHERE syncId = :syncId LIMIT 1")
    suspend fun getTimetableBySyncId(syncId: String): TimetableEntity?

    @Query("SELECT * FROM time_tables WHERE id = :id LIMIT 1")
    suspend fun getTimetableEntityById(id: Long): TimetableEntity?

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getCourseEntityById(id: Long): CourseEntity?

    @Query("SELECT * FROM time_slots WHERE id = :id LIMIT 1")
    suspend fun getTimeSlotEntityById(id: Long): TimeSlotEntity?

    @Query("SELECT * FROM academic_events WHERE id = :id LIMIT 1")
    suspend fun getAcademicEventEntityById(id: Long): AcademicEventEntity?

    @Query("SELECT id FROM time_tables WHERE semesterName = :semesterName AND semesterStartEpochDay = :startEpochDay AND deletedAt IS NULL ORDER BY id")
    suspend fun findTimetableIdsByIdentity(semesterName: String, startEpochDay: Long): List<Long>

    @Query("SELECT * FROM time_tables WHERE semesterName = :semesterName AND semesterStartEpochDay = :startEpochDay AND deletedAt IS NULL ORDER BY id")
    suspend fun findTimetablesByIdentity(semesterName: String, startEpochDay: Long): List<TimetableEntity>

    @Query("SELECT * FROM courses WHERE timetableId = :timetableId AND name = :name AND IFNULL(location, '') = IFNULL(:location, '') AND IFNULL(teacher, '') = IFNULL(:teacher, '') AND deletedAt IS NULL ORDER BY id")
    suspend fun findCoursesByIdentity(timetableId: Long, name: String, location: String?, teacher: String?): List<CourseEntity>

    @Query("SELECT * FROM time_slots WHERE courseId = :courseId AND dayOfWeek = :dayOfWeek AND startMinute = :startMinute AND endMinute = :endMinute AND recurrence = :recurrence AND deletedAt IS NULL ORDER BY id")
    suspend fun findTimeSlotsByIdentity(courseId: Long, dayOfWeek: Int, startMinute: Int, endMinute: Int, recurrence: Int): List<TimeSlotEntity>

    @Query("""
        SELECT * FROM academic_events
        WHERE timetableId = :timetableId
          AND title = :title
          AND type = :type
          AND dateEpochDay = :dateEpochDay
          AND IFNULL(timeMinute, -1) = IFNULL(:timeMinute, -1)
          AND IFNULL(courseName, '') = IFNULL(:courseName, '')
          AND IFNULL(location, '') = IFNULL(:location, '')
          AND IFNULL(note, '') = IFNULL(:note, '')
          AND IFNULL(reminderMinutesBefore, -1) = IFNULL(:reminderMinutesBefore, -1)
          AND completed = :completed
          AND deletedAt IS NULL
        ORDER BY id
    """)
    suspend fun findAcademicEventsByIdentity(
        timetableId: Long,
        title: String,
        type: Int,
        dateEpochDay: Long,
        timeMinute: Int?,
        courseName: String?,
        location: String?,
        note: String?,
        reminderMinutesBefore: Int?,
        completed: Boolean,
    ): List<AcademicEventEntity>

    @Transaction
    @Query("SELECT * FROM time_tables WHERE deletedAt IS NULL")
    fun getTimetables(): Flow<List<TimetableWithCourses>>

    @Transaction
    @Query("SELECT * FROM time_tables WHERE id = :id AND deletedAt IS NULL")
    fun getTimetableById(id: Long): Flow<TimetableWithCourses?>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :courseId AND deletedAt IS NULL")
    fun getCourseById(courseId: Long): Flow<CourseWithSlots?>

    @Transaction
    @Query("SELECT * FROM time_slots WHERE id = :slotId AND deletedAt IS NULL")
    fun getTimeSlotById(slotId: Long): Flow<TimeSlotEntity?>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = (SELECT courseId FROM time_slots WHERE id = :slotId AND deletedAt IS NULL) AND deletedAt IS NULL")
    fun getCourseByTimeSlotId(slotId: Long): Flow<CourseWithSlots?>

    @Transaction
    @Query("SELECT * FROM time_tables WHERE id = (SELECT timetableId FROM courses WHERE id = :courseId AND deletedAt IS NULL) AND deletedAt IS NULL")
    fun getTimetableByCourseId(courseId: Long): Flow<TimetableWithCourses?>
}
