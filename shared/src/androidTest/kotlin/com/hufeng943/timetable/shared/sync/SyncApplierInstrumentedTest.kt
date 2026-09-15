package com.hufeng943.timetable.shared.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hufeng943.timetable.shared.data.database.AppDatabase
import com.hufeng943.timetable.shared.data.entities.TimetableEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncApplierInstrumentedTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun semanticMatchDoesNotLetOlderRemoteRevisionOverwriteNewerLocal() = runBlocking {
        val id = db.timetableDao().insertTimetable(
            TimetableEntity(
                syncId = "local-sync",
                semesterName = "2026秋季",
                createdAtMillis = 1,
                semesterStartEpochDay = 20_000,
                semesterEndEpochDay = 20_140,
                color = -1,
                updatedAt = 1_000,
                revision = 10,
                modifiedBy = "phone",
            )
        )
        val remote = timetableRecord(
            sourceRecordId = 7,
            syncId = "different-remote-sync",
            revision = 5,
            updatedAt = 900,
            deviceId = "watch",
            color = 123,
        )

        SyncApplier(db).apply(listOf(remote))

        val actual = db.timetableDao().getTimetableEntityById(id)!!
        assertEquals(10L, actual.revision)
        assertEquals("local-sync", actual.syncId)
        assertEquals(-1L, actual.color)
    }


    @Test
    fun deleteForUnknownEntityCreatesTombstoneAndBlocksOlderResurrection() = runBlocking {
        val applier = SyncApplier(db)
        val delete = timetableRecord(9, "deleted-stable", 5, 500, "watch", 0)
            .copy(operation = SyncOperation.DELETE)
        applier.apply(listOf(delete))

        val olderUpsert = timetableRecord(10, "deleted-stable", 4, 400, "phone", 99)
        applier.apply(listOf(olderUpsert))

        assertEquals(null, db.timetableDao().getTimetableEntityBySyncId("deleted-stable"))
        val tombstone = db.syncTombstoneDao().find("deleted-stable", SyncEntityType.TIMETABLE)
        assertEquals(5L, tombstone?.revision)
    }

    @Test
    fun duplicateRequestIdIsAppliedExactlyOnce() = runBlocking {
        val applier = SyncApplier(db)
        val first = timetableRecord(1, "stable", 1, 100, "phone", 11)
        val duplicateButDifferentPayload = timetableRecord(1, "stable", 2, 200, "phone", 22)

        applier.applyOnce("request-123", "phone", listOf(first))
        applier.applyOnce("request-123", "phone", listOf(duplicateButDifferentPayload))

        val actual = db.timetableDao().getTimetableEntityBySyncId("stable")!!
        assertEquals(1L, actual.revision)
        assertEquals(11L, actual.color)
        assertTrue(db.processedSyncDao().exists("request-123"))
    }

    private fun timetableRecord(
        sourceRecordId: Long,
        syncId: String,
        revision: Long,
        updatedAt: Long,
        deviceId: String,
        color: Long,
    ) = SyncRecordPayload(
        sourceRecordId = sourceRecordId,
        entityId = 0,
        entityType = SyncEntityType.TIMETABLE,
        operation = SyncOperation.UPSERT,
        revision = revision,
        updatedAt = updatedAt,
        deviceId = deviceId,
        payloadJson = """{
            "syncId":"$syncId",
            "semesterName":"2026秋季",
            "createdAtMillis":1,
            "semesterStartEpochDay":20000,
            "semesterEndEpochDay":20140,
            "color":$color
        }""".trimIndent(),
    )
}
