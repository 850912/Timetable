package com.hufeng943.timetable.transfer

import android.content.Context
import com.hufeng943.timetable.TimetableDatabaseProvider
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.sync.SyncManager
import com.hufeng943.timetable.shared.sync.SyncRecordPayload
import com.hufeng943.timetable.shared.sync.SyncResult
import com.hufeng943.timetable.sync.WearOsTransport
import kotlinx.coroutines.runBlocking

@Deprecated("Use SyncManager with WearOsTransport")
object PhoneWearSyncManager {
    fun send(context: Context, timetables: List<Timetable>): String = runBlocking {
        val db = TimetableDatabaseProvider.database(context)
        val records = db.syncRecordDao().pending().map {
            SyncRecordPayload(it.id, it.entityId, it.entityType, it.operation, it.revision, it.updatedAt, it.deviceId, it.payloadJson)
        }
        when (val result = SyncManager(listOf(WearOsTransport(context))).syncRecords(records)) {
            SyncResult.Success -> "Wear OS"
            is SyncResult.Failed -> throw IllegalStateException(result.message, result.cause)
        }
    }
}
