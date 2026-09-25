package com.hufeng943.timetable.shared.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = TimetableEntity::class,
            parentColumns = ["id"],
            childColumns = ["timetableId"],
            onDelete = ForeignKey.CASCADE
        )// 与TimetableEntity绑定
    ],
    indices = [
        Index("timetableId"),
        Index(value = ["syncId"], unique = true),
    ]
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long= 0,
    @ColumnInfo(defaultValue = "''")
    val syncId: String = "",
    val timetableId: Long,
    val name: String,
    val location: String?,
    val color: Long,
    val teacher: String?,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val revision: Long = 0,
    @ColumnInfo(defaultValue = "'UNKNOWN'")
    val modifiedBy: String = "UNKNOWN",
    val deletedAt: Long? = null
)
