package com.hufeng943.timetable.shared.data.repository

import com.hufeng943.timetable.shared.model.AcademicEvent
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import kotlinx.coroutines.flow.Flow

data class TimeSlotMutation(val slot: TimeSlot, val courseId: Long)

interface TimetableRepository {
    // 更新
    suspend fun upsertTimetable(timetable: Timetable): Long
    suspend fun upsertCourse(course: Course, timetableId: Long): Long
    suspend fun upsertTimeSlot(timeSlot: TimeSlot, courseId: Long): Long
    /** Atomically applies ordinary time-slot mutations without adding entries to the adjustment undo journal. */
    suspend fun applyTimeSlotMutations(mutations: List<TimeSlotMutation>)
    /** Atomically applies schedule-adjustment slot changes and records their undo journal. */
    suspend fun applyAdjustmentMutations(timetableId: Long, mutations: List<TimeSlotMutation>)
    /** Atomically restores all adjustment mutations for one timetable and clears their journal. */
    suspend fun restoreAdjustmentMutations(timetableId: Long)
    suspend fun upsertAcademicEvent(event: AcademicEvent, timetableId: Long): Long

    // 删除课表
    suspend fun deleteTimetable(timetableId: Long)
    suspend fun deleteCourse(courseId: Long)
    suspend fun deleteTimeSlot(timeSlotId: Long)
    suspend fun deleteAcademicEvent(eventId: Long)

    // 获取所有课表
    fun getAllTimetables(): Flow<List<Timetable>>
    fun getTimetableById(timetableId: Long): Flow<Timetable?>
    fun getCourseById(courseId: Long): Flow<Course?>
    fun getTimeSlotById(timeSlotId: Long): Flow<TimeSlot?>
    fun getCourseByTimeSlotId(timeSlotId: Long): Flow<Course?>
    fun getTimetableByCourseId(courseId: Long): Flow<Timetable?>
}