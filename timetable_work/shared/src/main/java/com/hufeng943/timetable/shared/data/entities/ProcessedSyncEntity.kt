package com.hufeng943.timetable.shared.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_sync_requests")
data class ProcessedSyncEntity(
    @PrimaryKey
    val requestId: String,
    val deviceId: String,
    val processedAt: Long
)
