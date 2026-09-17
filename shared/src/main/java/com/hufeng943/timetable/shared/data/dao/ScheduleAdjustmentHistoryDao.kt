package com.hufeng943.timetable.shared.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hufeng943.timetable.shared.data.entities.ScheduleAdjustmentHistoryEntity

@Dao
interface ScheduleAdjustmentHistoryDao {
    @Insert suspend fun insert(entry: ScheduleAdjustmentHistoryEntity): Long
    @Query("SELECT COALESCE(MAX(sequence), 0) FROM schedule_adjustment_history WHERE timetableId = :timetableId")
    suspend fun maxSequence(timetableId: Long): Long
    @Query("SELECT * FROM schedule_adjustment_history WHERE timetableId = :timetableId ORDER BY sequence DESC, id DESC")
    suspend fun getForTimetable(timetableId: Long): List<ScheduleAdjustmentHistoryEntity>
    @Query("DELETE FROM schedule_adjustment_history WHERE timetableId = :timetableId")
    suspend fun clearForTimetable(timetableId: Long)
}
