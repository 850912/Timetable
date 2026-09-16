package com.hufeng943.timetable.shared.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To11() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(
            TEST_DB,
            11,
            true,
            AppDatabaseMigrations.MIGRATION_1_2,
            AppDatabaseMigrations.MIGRATION_2_3,
            AppDatabaseMigrations.MIGRATION_3_4,
            AppDatabaseMigrations.MIGRATION_4_5,
            AppDatabaseMigrations.MIGRATION_5_6,
            AppDatabaseMigrations.MIGRATION_6_7,
            AppDatabaseMigrations.MIGRATION_7_8,
            AppDatabaseMigrations.MIGRATION_8_9,
            AppDatabaseMigrations.MIGRATION_9_10,
            AppDatabaseMigrations.MIGRATION_10_11,
        ).close()
    }

    @Test
    fun migrate6To11PreservesSyncInfrastructure() {
        val db = helper.createDatabase(TEST_DB_6, 6)
        db.execSQL("INSERT INTO sync_tombstones(syncId, entityType, revision, updatedAt, deviceId) VALUES('x','COURSE',3,100,'phone')")
        db.close()
        val migrated = helper.runMigrationsAndValidate(
            TEST_DB_6,
            11,
            true,
            AppDatabaseMigrations.MIGRATION_6_7,
            AppDatabaseMigrations.MIGRATION_7_8,
            AppDatabaseMigrations.MIGRATION_8_9,
            AppDatabaseMigrations.MIGRATION_9_10,
            AppDatabaseMigrations.MIGRATION_10_11,
        )
        migrated.query("SELECT revision FROM sync_tombstones WHERE syncId='x'").use { cursor ->
            check(cursor.moveToFirst())
            check(cursor.getLong(0) == 3L)
        }
        migrated.close()
    }

    companion object {
        private const val TEST_DB = "migration-1-11"
        private const val TEST_DB_6 = "migration-6-11"
    }
}
