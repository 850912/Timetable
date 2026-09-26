package com.hufeng943.timetable.sync

import android.util.Base64
import com.hufeng943.timetable.TimetableDatabaseProvider
import com.hufeng943.timetable.shared.export.BackupManager
import com.hufeng943.timetable.shared.importexport.ChinaWearProtocol
import com.hufeng943.timetable.shared.importexport.ChinaWearEnvelope
import com.hufeng943.timetable.shared.importexport.ChinaWearPacketCodec
import com.hufeng943.timetable.shared.sync.SyncAck
import com.hufeng943.timetable.shared.sync.SyncApplier
import com.hufeng943.timetable.shared.sync.SyncEnvelope
import com.hufeng943.timetable.shared.sync.SyncRecordPayload
import com.hufeng943.timetable.shared.sync.SyncResult
import com.hufeng943.timetable.shared.sync.SyncTransport
import com.hufeng943.timetable.shared.model.Timetable
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * Build17/18 China-compatible transport.
 *
 * This is a real BLE GATT channel rather than a boolean placeholder. The watch
 * advertises the Timetable service when the Google Data Layer is unavailable;
 * the phone scans for that service and exchanges the same request/ACK protocol.
 */
class ChinaCompatibleWearTransport(
    private val context: Context,
) : SyncTransport {
    override val name: String = "China Wear BLE"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun isAvailable(): Boolean = ChinaWearBleCapabilities.isSupported(context)

    override suspend fun sendRecords(
        records: List<SyncRecordPayload>,
        requestId: String,
    ): SyncResult {
        if (records.isEmpty()) return SyncResult.Success
        return runCatching {
            val database = TimetableDatabaseProvider.database(context)
            val sourceDeviceId = SyncDeviceId.get(context)
            val request = SyncEnvelope(
                requestId = requestId,
                sourceDeviceId = sourceDeviceId,
                records = records,
            )
            val responseBytes = ChinaWearBleClient(context).request(
                ChinaWearPacketCodec.encode(
                    ChinaWearEnvelope(
                        requestId = requestId,
                        type = "SYNC_REQUEST",
                        timestamp = System.currentTimeMillis(),
                        version = ChinaWearProtocol.VERSION,
                        payload = json.encodeToString(request),
                    )
                )
            )
            val response = ChinaWearPacketCodec.decode(responseBytes)
            check(response.requestId == requestId) { "BLE ACK requestId 不匹配" }
            check(response.version == ChinaWearProtocol.VERSION) { "BLE 协议版本不匹配" }
            check(response.type == "SYNC_ACK") { "BLE 返回类型错误：${response.type}" }

            val ack = json.decodeFromString<SyncAck>(response.payload)
            check(ack.appliedRecordIds.toSet().containsAll(records.map { it.sourceRecordId })) {
                "BLE ACK 未确认全部记录写入"
            }

            if (ack.records.isNotEmpty()) {
                val applied = SyncApplier(database).applyOnce(
                    requestId = "${requestId}:remote",
                    sourceDeviceId = ack.sourceDeviceId,
                    records = ack.records,
                )
                val appliedAck = SyncAck(
                    requestId = requestId,
                    sourceDeviceId = sourceDeviceId,
                    appliedRecordIds = applied,
                )
                val confirmEnvelope = ChinaWearPacketCodec.encode(
                    ChinaWearEnvelope(
                        requestId = requestId,
                        type = "SYNC_APPLIED",
                        timestamp = System.currentTimeMillis(),
                        version = ChinaWearProtocol.VERSION,
                        payload = json.encodeToString(appliedAck),
                    )
                )
                val confirmedBytes = ChinaWearBleClient(context).request(confirmEnvelope)
                val confirmed = ChinaWearPacketCodec.decode(confirmedBytes)
                check(confirmed.type == "SYNC_APPLIED") { "BLE 双向 ACK 确认失败" }
            }

            database.syncRecordDao().markSynced(records.map { it.sourceRecordId })
            SyncDiagnosticsReporter.recordSync(context, "china_ble_success:$requestId:${records.size}")
            SyncResult.Success
        }.getOrElse { error ->
            SyncDiagnosticsReporter.recordError(context, "china_ble_failed:${error.message}")
            SyncResult.Failed(error.message ?: "China Wear BLE 同步失败", error)
        }
    }

    override suspend fun sendTimetableSnapshot(timetables: List<Timetable>): SyncResult =
        withContext(Dispatchers.IO) {
            if (timetables.isEmpty()) return@withContext SyncResult.Success

            val requestId = UUID.randomUUID().toString()
            val bytes = ByteArrayOutputStream().use { output ->
                BackupManager.backup(output, timetables)
                output.toByteArray()
            }
            if (bytes.size > 512 * 1024) {
                return@withContext SyncResult.Failed("国行 BLE 完整同步数据超过 512 KiB")
            }
            var lastFailure: SyncResult.Failed? = null

            repeat(com.hufeng943.timetable.shared.sync.SyncRetryPolicy.MAX_RETRY) { attempt ->
                val result = runCatching {
                    val envelope = ChinaWearEnvelope(
                        requestId = requestId,
                        type = "EXPORT_REQUEST",
                        timestamp = System.currentTimeMillis(),
                        version = ChinaWearProtocol.VERSION,
                        payload = Base64.encodeToString(bytes, Base64.NO_WRAP),
                    )
                    val responseBytes = ChinaWearBleClient(context).request(ChinaWearPacketCodec.encode(envelope))
                    val response = ChinaWearPacketCodec.decode(responseBytes)
                    check(response.requestId == requestId) { "完整课表 BLE ACK requestId 不匹配" }
                    check(response.type == "SYNC_APPLIED") { "完整课表 BLE ACK 错误：${response.type}" }
                    SyncDiagnosticsReporter.recordSync(context, "china_ble_snapshot_success:$requestId:${bytes.size}")
                    SyncResult.Success
                }.getOrElse { error ->
                    SyncDiagnosticsReporter.recordError(context, "china_ble_snapshot_failed:$requestId:${error.message}")
                    SyncResult.Failed(error.message ?: "China Wear BLE 完整同步失败", error)
                }
                if (result == SyncResult.Success) return@withContext result
                if (result is SyncResult.Failed) lastFailure = result
                if (attempt + 1 < com.hufeng943.timetable.shared.sync.SyncRetryPolicy.MAX_RETRY) {
                    kotlinx.coroutines.delay(com.hufeng943.timetable.shared.sync.SyncRetryPolicy.nextDelayMillis(attempt))
                }
            }
            lastFailure ?: SyncResult.Failed("China Wear BLE 完整同步失败")
        }

    override suspend fun receive(): SyncResult =
        SyncResult.Failed("China Wear BLE 手机端接收由扫描连接流程处理")
}

private object SyncDeviceId {
    private const val PREFS = "sync_identity"
    private const val KEY = "device_id"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY, null)
        if (existing != null) return existing
        val created = "phone-${UUID.randomUUID()}"
        prefs.edit().putString(KEY, created).apply()
        return created
    }
}
