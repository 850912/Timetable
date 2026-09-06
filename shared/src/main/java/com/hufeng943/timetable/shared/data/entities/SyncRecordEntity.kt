package com.hufeng943.timetable.shared.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_records",
    indices = [
        Index(value = ["synced", "updatedAt"]),
        Index(value = ["entityType", "entityId", "revision"])
    ]
)
data class SyncRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityId: Long,
    val entityType: String,
    val operation: String,
    @ColumnInfo(defaultValue = "0")
    val revision: Long,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long,
    val deviceId: String,
    @ColumnInfo(defaultValue = "0")
    val synced: Boolean = false,
    @ColumnInfo(defaultValue = "'{}'")
    val payloadJson: String = "{}"
)
