package com.hufeng943.timetable.shared.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hufeng943.timetable.shared.data.dao.SyncRecordDao
import com.hufeng943.timetable.shared.data.dao.SyncTombstoneDao
import com.hufeng943.timetable.shared.data.dao.TimetableDao
import com.hufeng943.timetable.shared.data.entities.CourseEntity
import com.hufeng943.timetable.shared.data.entities.SyncRecordEntity
import com.hufeng943.timetable.shared.data.entities.SyncTombstoneEntity
import com.hufeng943.timetable.shared.data.entities.TimeSlotEntity
import com.hufeng943.timetable.shared.data.entities.TimetableEntity

@Database(
    entities = [
        TimetableEntity::class,
        CourseEntity::class,
        TimeSlotEntity::class,
        SyncRecordEntity::class,
        SyncTombstoneEntity::class
    ],
    version = 6,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timetableDao(): TimetableDao
    abstract fun syncRecordDao(): SyncRecordDao
    abstract fun syncTombstoneDao(): SyncTombstoneDao
}
