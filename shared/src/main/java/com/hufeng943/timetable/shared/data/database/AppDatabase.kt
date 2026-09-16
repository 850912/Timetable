package com.hufeng943.timetable.shared.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hufeng943.timetable.shared.data.dao.SyncRecordDao
import com.hufeng943.timetable.shared.data.dao.SyncTombstoneDao
import com.hufeng943.timetable.shared.data.dao.TimetableDao
import com.hufeng943.timetable.shared.data.dao.ScheduleAdjustmentHistoryDao
import com.hufeng943.timetable.shared.data.entities.CourseEntity
import com.hufeng943.timetable.shared.data.entities.AcademicEventEntity
import com.hufeng943.timetable.shared.data.entities.SyncRecordEntity
import com.hufeng943.timetable.shared.data.entities.SyncTombstoneEntity
import com.hufeng943.timetable.shared.data.entities.ProcessedSyncEntity
import com.hufeng943.timetable.shared.data.entities.TimeSlotEntity
import com.hufeng943.timetable.shared.data.entities.TimetableEntity
import com.hufeng943.timetable.shared.data.entities.ScheduleAdjustmentHistoryEntity

@Database(
    entities = [
        TimetableEntity::class,
        CourseEntity::class,
        TimeSlotEntity::class,
        AcademicEventEntity::class,
        SyncRecordEntity::class,
        SyncTombstoneEntity::class,
        ProcessedSyncEntity::class,
        ScheduleAdjustmentHistoryEntity::class
    ],
    version = 11,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timetableDao(): TimetableDao
    abstract fun syncRecordDao(): SyncRecordDao
    abstract fun syncTombstoneDao(): SyncTombstoneDao
    abstract fun scheduleAdjustmentHistoryDao(): ScheduleAdjustmentHistoryDao
    abstract fun processedSyncDao(): com.hufeng943.timetable.shared.data.dao.ProcessedSyncDao
}
