package com.hufeng943.timetable.shared.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.hufeng943.timetable.shared.data.entities.SyncRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncRecordDao {
    @Insert
    suspend fun insert(record: SyncRecordEntity): Long

    /**
     * Keep at most one unsynced record per entity. A newer local edit fully supersedes
     * an older unsynced UPSERT/DELETE for the same entity because every payload is a
     * complete entity snapshot/tombstone and revisions are monotonic.
     */
    @Transaction
    suspend fun replacePending(record: SyncRecordEntity): Long {
        deletePendingForEntity(record.entityType, record.entityId)
        return insert(record)
    }

    @Query("DELETE FROM sync_records WHERE synced = 0 AND entityType = :entityType AND entityId = :entityId")
    suspend fun deletePendingForEntity(entityType: String, entityId: Long)

    @Query("SELECT * FROM sync_records WHERE synced = 0 AND retryCount < 5 ORDER BY updatedAt ASC, id ASC")
    suspend fun pending(): List<SyncRecordEntity>

    @Query("UPDATE sync_records SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE sync_records SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT * FROM sync_records WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<SyncRecordEntity>

    @Query("SELECT COUNT(*) FROM sync_records WHERE synced = 0 AND retryCount < 5")
    suspend fun pendingCount(): Int

    @Query("SELECT COUNT(*) FROM sync_records WHERE synced = 0 AND retryCount < 5")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_records WHERE synced = 0 AND retryCount >= 5")
    suspend fun permanentlyFailedCount(): Int

    @Query("SELECT COUNT(*) FROM sync_records WHERE synced = 0 AND retryCount >= 5")
    fun observePermanentlyFailedCount(): Flow<Int>

    @Query("UPDATE sync_records SET retryCount = 0, lastAttemptAt = 0, lastError = NULL WHERE synced = 0 AND retryCount >= 5")
    suspend fun retryFailed(): Int

    @Query("UPDATE sync_records SET retryCount = retryCount + 1, lastAttemptAt = :time, lastError = :error WHERE id IN (:ids)")
    suspend fun markFailed(ids: List<Long>, time: Long, error: String?)

    @Query("DELETE FROM sync_records WHERE synced = 1 AND updatedAt < :beforeMillis")
    suspend fun deleteSyncedBefore(beforeMillis: Long): Int

    @Query("DELETE FROM sync_records WHERE synced = 1 AND id NOT IN (SELECT id FROM sync_records WHERE synced = 1 ORDER BY updatedAt DESC, id DESC LIMIT :keepLatest)")
    suspend fun trimSyncedHistory(keepLatest: Int): Int
}
