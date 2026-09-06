package com.hufeng943.timetable.shared.data.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "sync_tombstones",
    primaryKeys = ["syncId", "entityType"],
    indices = [Index("updatedAt")]
)
data class SyncTombstoneEntity(
    val syncId: String,
    val entityType: String,
    val revision: Long,
    val updatedAt: Long,
    val deviceId: String,
)
