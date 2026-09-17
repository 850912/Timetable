package com.hufeng943.timetable.shared.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "academic_events",
    foreignKeys = [
        ForeignKey(
            entity = TimetableEntity::class,
            parentColumns = ["id"],
            childColumns = ["timetableId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("timetableId"),
        Index(value = ["syncId"], unique = true),
        Index(value = ["dateEpochDay", "completed"]),
    ],
)
data class AcademicEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(defaultValue = "''")
    val syncId: String = "",
    val timetableId: Long,
    val title: String,
    val type: Int,
    val dateEpochDay: Long,
    val timeMinute: Int?,
    val courseName: String?,
    val location: String?,
    val note: String?,
    val reminderMinutesBefore: Int?,
    @ColumnInfo(defaultValue = "0")
    val completed: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val revision: Long = 0,
    @ColumnInfo(defaultValue = "'UNKNOWN'")
    val modifiedBy: String = "UNKNOWN",
    val deletedAt: Long? = null,
)
