package com.hufeng943.timetable.shared.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hufeng943.timetable.shared.data.entities.SyncTombstoneEntity

@Dao
interface SyncTombstoneDao {
    @Query("SELECT * FROM sync_tombstones WHERE syncId = :syncId AND entityType = :entityType LIMIT 1")
    suspend fun find(syncId: String, entityType: String): SyncTombstoneEntity?

    @Upsert
    suspend fun upsert(tombstone: SyncTombstoneEntity)

    @Query("DELETE FROM sync_tombstones WHERE syncId = :syncId AND entityType = :entityType")
    suspend fun delete(syncId: String, entityType: String)
}
