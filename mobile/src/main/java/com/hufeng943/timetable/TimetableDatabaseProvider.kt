package com.hufeng943.timetable

import android.content.Context
import androidx.room.Room
import com.hufeng943.timetable.shared.data.database.AppDatabase
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.data.repository.TimetableRepositoryImpl
import com.hufeng943.timetable.shared.importexport.ImportService

object TimetableDatabaseProvider {
    @Volatile
    private var database: AppDatabase? = null

    fun database(context: Context): AppDatabase = database ?: synchronized(this) {
        database ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "timetable.db"
        ).build().also { database = it }
    }

    fun repository(context: Context): TimetableRepository =
        TimetableRepositoryImpl(database(context).timetableDao())

    fun importService(context: Context): ImportService {
        val db = database(context)
        return ImportService(db, db.timetableDao())
    }
}
