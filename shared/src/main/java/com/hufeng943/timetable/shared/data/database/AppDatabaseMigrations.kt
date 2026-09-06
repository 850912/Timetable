package com.hufeng943.timetable.shared.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Keeps all pre-sync database versions upgradeable without destructive migration.
 * Room schema history in this project is 1 -> 2 -> 3 -> 4 -> 5.
 */
object AppDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE time_tables ADD COLUMN color INTEGER NOT NULL DEFAULT -1"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 3 keeps the same physical schema as version 2.
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE time_tables ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE time_tables ADD COLUMN revision INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE time_tables ADD COLUMN modifiedBy TEXT NOT NULL DEFAULT 'UNKNOWN'"
            )
            db.execSQL(
                "ALTER TABLE time_tables ADD COLUMN deletedAt INTEGER"
            )

            db.execSQL(
                "ALTER TABLE courses ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE courses ADD COLUMN revision INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE courses ADD COLUMN modifiedBy TEXT NOT NULL DEFAULT 'UNKNOWN'"
            )
            db.execSQL(
                "ALTER TABLE courses ADD COLUMN deletedAt INTEGER"
            )

            db.execSQL(
                "ALTER TABLE time_slots ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE time_slots ADD COLUMN revision INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE time_slots ADD COLUMN modifiedBy TEXT NOT NULL DEFAULT 'UNKNOWN'"
            )
            db.execSQL(
                "ALTER TABLE time_slots ADD COLUMN deletedAt INTEGER"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS sync_records (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "entityId INTEGER NOT NULL, " +
                    "entityType TEXT NOT NULL, " +
                    "operation TEXT NOT NULL, " +
                    "revision INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL, " +
                    "deviceId TEXT NOT NULL, " +
                    "synced INTEGER NOT NULL DEFAULT 0)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_records_synced_updatedAt ON sync_records (synced, updatedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_records_entityType_entityId_revision ON sync_records (entityType, entityId, revision)")

            // Existing records predate revisions; use their creation/update timestamp as baseline.
            db.execSQL("UPDATE time_tables SET updatedAt = createdAtMillis WHERE updatedAt = 0")
            db.execSQL("UPDATE courses SET updatedAt = strftime('%s','now') * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE time_slots SET updatedAt = strftime('%s','now') * 1000 WHERE updatedAt = 0")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE sync_records ADD COLUMN payloadJson TEXT NOT NULL DEFAULT '{}'"
            )
        }
    }
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE time_tables ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE courses ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE time_slots ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE time_tables SET syncId = lower(hex(randomblob(16))) WHERE syncId = ''")
            db.execSQL("UPDATE courses SET syncId = lower(hex(randomblob(16))) WHERE syncId = ''")
            db.execSQL("UPDATE time_slots SET syncId = lower(hex(randomblob(16))) WHERE syncId = ''")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_time_tables_syncId ON time_tables(syncId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_courses_syncId ON courses(syncId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_time_slots_syncId ON time_slots(syncId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS sync_tombstones (syncId TEXT NOT NULL, entityType TEXT NOT NULL, revision INTEGER NOT NULL, updatedAt INTEGER NOT NULL, deviceId TEXT NOT NULL, PRIMARY KEY(syncId, entityType))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_tombstones_updatedAt ON sync_tombstones(updatedAt)")
        }
    }

}
