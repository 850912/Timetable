package com.hufeng943.timetable.shared.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Persistent undo journal for schedule adjustment operations. Kept in Room so data changes and
 * their undo records commit atomically. */
@Entity(
    tableName = "schedule_adjustment_history",
    indices = [Index("timetableId"), Index(value = ["timetableId", "sequence"])],
)
data class ScheduleAdjustmentHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timetableId: Long,
    val sequence: Long,
    val courseId: Long,
    val slotId: Long,
    /** RESTORE restores snapshotJson; DELETE removes a slot created by the adjustment. */
    val action: String,
    val snapshotJson: String? = null,
)
