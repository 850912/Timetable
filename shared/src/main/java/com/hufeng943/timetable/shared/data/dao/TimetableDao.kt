package com.hufeng943.timetable.shared.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.hufeng943.timetable.shared.data.entities.CourseEntity
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

    @Upsert
    suspend fun upsertTimetable(timetable: TimetableEntity)

    @Upsert
    suspend fun upsertCourse(course: CourseEntity)

    @Upsert
    suspend fun upsertTimeSlot(slot: TimeSlotEntity)


    @Query("UPDATE time_tables SET deletedAt = :deletedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDeleteTimetableForImport(id: Long, deletedAt: Long)

    @Query("UPDATE courses SET deletedAt = :deletedAt WHERE timetableId = :timetableId AND deletedAt IS NULL")
    suspend fun softDeleteCoursesForImport(timetableId: Long, deletedAt: Long)

    @Query("UPDATE time_slots SET deletedAt = :deletedAt WHERE courseId IN (SELECT id FROM courses WHERE timetableId = :timetableId) AND deletedAt IS NULL")
    suspend fun softDeleteTimeSlotsForImport(timetableId: Long, deletedAt: Long)

    @Query("SELECT * FROM courses WHERE timetableId = :timetableId AND deletedAt IS NULL")
    suspend fun getCourseEntitiesByTimetableId(timetableId: Long): List<CourseEntity>

    @Query("SELECT * FROM time_slots WHERE courseId = :courseId AND deletedAt IS NULL")
    suspend fun getTimeSlotEntitiesByCourseId(courseId: Long): List<TimeSlotEntity>

    @Query("UPDATE time_tables SET updatedAt = :updatedAt, revision = :revision, modifiedBy = :modifiedBy, deletedAt = :deletedAt WHERE id = :id")
    suspend fun markTimetableDeleted(id: Long, updatedAt: Long, revision: Long, modifiedBy: String, deletedAt: Long)

    @Query("UPDATE courses SET updatedAt = :updatedAt, revision = :revision, modifiedBy = :modifiedBy, deletedAt = :deletedAt WHERE id = :id")
    suspend fun markCourseDeleted(id: Long, updatedAt: Long, revision: Long, modifiedBy: String, deletedAt: Long)

    @Query("UPDATE time_slots SET updatedAt = :updatedAt, revision = :revision, modifiedBy = :modifiedBy, deletedAt = :deletedAt WHERE id = :id")
    suspend fun markTimeSlotDeleted(id: Long, updatedAt: Long, revision: Long, modifiedBy: String, deletedAt: Long)

    @Query("SELECT * FROM time_tables WHERE syncId = :syncId LIMIT 1")
    suspend fun getTimetableEntityBySyncId(syncId: String): TimetableEntity?

    @Query("SELECT * FROM courses WHERE syncId = :syncId LIMIT 1")
    suspend fun getCourseEntityBySyncId(syncId: String): CourseEntity?

    @Query("SELECT * FROM time_slots WHERE syncId = :syncId LIMIT 1")
    suspend fun getTimeSlotEntityBySyncId(syncId: String): TimeSlotEntity?

    @Query("SELECT * FROM time_tables WHERE syncId = :syncId LIMIT 1")
    suspend fun getTimetableBySyncId(syncId: String): TimetableEntity?

    @Query("SELECT * FROM time_tables WHERE id = :id LIMIT 1")
    suspend fun getTimetableEntityById(id: Long): TimetableEntity?

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getCourseEntityById(id: Long): CourseEntity?

    @Query("SELECT * FROM time_slots WHERE id = :id LIMIT 1")
    suspend fun getTimeSlotEntityById(id: Long): TimeSlotEntity?

    @Query("SELECT id FROM time_tables WHERE semesterName = :semesterName AND semesterStartEpochDay = :startEpochDay AND deletedAt IS NULL LIMIT 1")
    suspend fun findTimetableIdByIdentity(semesterName: String, startEpochDay: Long): Long?

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
