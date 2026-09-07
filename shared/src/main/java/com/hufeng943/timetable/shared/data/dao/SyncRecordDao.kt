package com.hufeng943.timetable.shared.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hufeng943.timetable.shared.data.entities.SyncRecordEntity

@Dao
interface SyncRecordDao {
    @Insert
    suspend fun insert(record: SyncRecordEntity): Long

    @Query("SELECT * FROM sync_records WHERE synced = 0 AND retryCount < 5 ORDER BY updatedAt ASC, id ASC")
    suspend fun pending(): List<SyncRecordEntity>

    @Query("UPDATE sync_records SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE sync_records SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT * FROM sync_records WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<SyncRecordEntity>

    @Query("SELECT COUNT(*) FROM sync_records WHERE synced = 0")
    suspend fun pendingCount(): Int

    @Query("UPDATE sync_records SET retryCount = retryCount + 1, lastAttemptAt = :time, lastError = :error WHERE id IN (:ids)")
    suspend fun markFailed(ids: List<Long>, time: Long, error: String?)
}
