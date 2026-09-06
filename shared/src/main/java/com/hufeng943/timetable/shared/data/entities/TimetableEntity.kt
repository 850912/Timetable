package com.hufeng943.timetable.shared.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "time_tables", indices = [Index(value = ["syncId"], unique = true)])
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(defaultValue = "''")
    val syncId: String = "",
    val semesterName: String,
    val createdAtMillis: Long,
    val semesterStartEpochDay: Long,
    val semesterEndEpochDay: Long?,
    @ColumnInfo(defaultValue = "-1")
    val color: Long,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val revision: Long = 0,
    @ColumnInfo(defaultValue = "'UNKNOWN'")
    val modifiedBy: String = "UNKNOWN",
    val deletedAt: Long? = null
)