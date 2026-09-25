package com.hufeng943.timetable.shared.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncManagerTest {
    @Test
    fun retriesAfterFailureAndEventuallySucceedsWithSameRequestId() = kotlinx.coroutines.runBlocking {
        val transport = FakeTransport(
            results = ArrayDeque(listOf(SyncResult.Failed("temporary"), SyncResult.Success))
        )
        val result = SyncManager(listOf(transport)).syncRecords(listOf(record(1, 1)))
        assertEquals(SyncResult.Success, result)
        assertEquals(2, transport.calls)
        assertEquals(1, transport.requestIds.distinct().size)
    }

    @Test
    fun compactsOlderRevisionsOfSameEntityBeforeTransport() = kotlinx.coroutines.runBlocking {
        val transport = FakeTransport(ArrayDeque(listOf(SyncResult.Success)))
        val manager = SyncManager(listOf(transport))
        manager.syncRecords(
            listOf(
                record(1, 1, revision = 1),
                record(2, 1, revision = 3),
                record(3, 2, revision = 1),
            )
        )
        val sent = transport.lastRecords
        assertEquals(2, sent.size)
        assertTrue(sent.any { it.entityId == 1L && it.revision == 3L })
        assertTrue(sent.any { it.entityId == 2L })
    }

    private fun record(id: Long, entityId: Long, revision: Long = 1) = SyncRecordPayload(
        sourceRecordId = id,
        entityId = entityId,
        entityType = SyncEntityType.COURSE,
        operation = SyncOperation.UPSERT,
        revision = revision,
        updatedAt = id * 100,
        deviceId = "phone",
        payloadJson = "{}",
    )

    private class FakeTransport(
        private val results: ArrayDeque<SyncResult>,
    ) : SyncTransport {
        override val name: String = "fake"
        var calls: Int = 0
        var lastRecords: List<SyncRecordPayload> = emptyList()
        val requestIds = mutableListOf<String>()

        override suspend fun isAvailable(): Boolean = true

        override suspend fun sendRecords(
            records: List<SyncRecordPayload>,
            requestId: String,
        ): SyncResult {
            calls++
            requestIds += requestId
            lastRecords = records
            return if (results.isEmpty()) SyncResult.Failed("no result") else results.removeFirst()
        }

        override suspend fun receive(): SyncResult = SyncResult.Failed("unused")
    }
}
