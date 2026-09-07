package com.hufeng943.timetable.shared.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hufeng943.timetable.shared.data.entities.ProcessedSyncEntity

@Dao
interface ProcessedSyncDao {
    @Query("SELECT EXISTS(SELECT 1 FROM processed_sync_requests WHERE requestId = :requestId)")
    suspend fun exists(requestId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ProcessedSyncEntity)

    @Query("DELETE FROM processed_sync_requests WHERE processedAt < :time")
    suspend fun cleanup(time: Long)
}
