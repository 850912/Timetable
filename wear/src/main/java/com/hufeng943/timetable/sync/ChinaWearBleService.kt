package com.hufeng943.timetable.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hufeng943.timetable.R
import com.hufeng943.timetable.shared.data.database.AppDatabase
import com.hufeng943.timetable.shared.importexport.ChinaWearBleProtocol
import com.hufeng943.timetable.shared.importexport.ImportService
import com.hufeng943.timetable.shared.importexport.ChinaWearProtocol
import com.hufeng943.timetable.shared.importexport.ChinaWearEnvelope
import com.hufeng943.timetable.shared.importexport.ChinaWearPacketCodec
import com.hufeng943.timetable.shared.sync.SyncAck
import com.hufeng943.timetable.shared.sync.SyncApplier
import com.hufeng943.timetable.shared.sync.SyncEnvelope
import com.hufeng943.timetable.shared.sync.SyncRecordPayload
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@AndroidEntryPoint
class ChinaWearBleService : Service() {
    @Inject lateinit var database: AppDatabase
    @Inject lateinit var importService: ImportService

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var currentMtu = ConcurrentHashMap<BluetoothDevice, Int>()
    private val transferCounter = AtomicLong(System.nanoTime())
    private val assemblies = ConcurrentHashMap<String, FrameAssembly>()

    override fun onCreate() {
        super.onCreate()
        if (!hasBlePermissions()) {
            SyncDiagnosticLogger.record(this, "china_ble_permission_missing")
            stopSelf()
            return
        }
        val foregroundStarted = runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        }.isSuccess
        if (!foregroundStarted) {
            SyncDiagnosticLogger.record(this, "china_ble_foreground_start_failed")
            stopSelf()
            return
        }
        startBle()
    }

    private fun hasBlePermissions(): Boolean = if (android.os.Build.VERSION.SDK_INT >= 31) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startBle() {
        val manager = getSystemService(BluetoothManager::class.java) ?: run { stopSelf(); return }
        val adapter = manager.adapter ?: run { stopSelf(); return }
        if (!adapter.isEnabled || !packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            SyncDiagnosticLogger.record(this, "china_ble_unavailable")
            stopSelf()
            return
        }
        val service = android.bluetooth.BluetoothGattService(
            ChinaWearBleProtocol.SERVICE_UUID,
            android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY,
        )
        val write = BluetoothGattCharacteristic(
            ChinaWearBleProtocol.WRITE_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE,
        )
        notifyCharacteristic = BluetoothGattCharacteristic(
            ChinaWearBleProtocol.NOTIFY_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ,
        ).also { characteristic ->
            characteristic.addDescriptor(
                BluetoothGattDescriptor(
                    ChinaWearBleProtocol.CCCD_UUID,
                    BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
                )
            )
        }
        service.addCharacteristic(write)
        service.addCharacteristic(requireNotNull(notifyCharacteristic))

        gattServer = manager.openGattServer(this, callback)
        if (gattServer?.addService(service) != true) {
            SyncDiagnosticLogger.record(this, "china_ble_add_service_failed")
            stopSelf()
            return
        }

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            SyncDiagnosticLogger.record(this, "china_ble_advertiser_unavailable")
            stopSelf()
            return
        }
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(android.os.ParcelUuid(ChinaWearBleProtocol.SERVICE_UUID))
            .addServiceData(android.os.ParcelUuid(ChinaWearBleProtocol.SERVICE_UUID), byteArrayOf(ChinaWearBleProtocol.ROLE_WATCH))
            .build()
        advertiser?.startAdvertising(settings, data, advertiseCallback)
        SyncDiagnosticLogger.record(this, "china_ble_advertising_started")
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            SyncDiagnosticLogger.record(this@ChinaWearBleService, "china_ble_advertising_ready")
        }

        override fun onStartFailure(errorCode: Int) {
            SyncDiagnosticLogger.record(this@ChinaWearBleService, "china_ble_advertising_failed:$errorCode")
            stopSelf()
        }
    }

    private val callback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                assemblies.keys.removeIf { it.startsWith(device.address) }
                currentMtu.remove(device)
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                currentMtu[device] = 23
                SyncDiagnosticLogger.record(this@ChinaWearBleService, "china_ble_connected:${device.address}")
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            currentMtu[device] = mtu.coerceAtLeast(23)
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (characteristic.uuid != ChinaWearBleProtocol.WRITE_UUID) return
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
            }
            val frame = runCatching { ChinaWearBleProtocol.decode(value) }.getOrNull() ?: return
            if (frame.total !in 1..ChinaWearBleProtocol.MAX_FRAME_COUNT) return
            val key = "${device.address}:${frame.transferId}"
            val assembly = assemblies.getOrPut(key) { FrameAssembly(frame.total) }
            if (assembly.total != frame.total) {
                assemblies.remove(key)
                return
            }
            assembly.parts[frame.sequence] = frame.payload
            if (assembly.parts.values.sumOf { it.size } > MAX_ASSEMBLY_BYTES) {
                assemblies.remove(key)
                return
            }
            if (assembly.parts.size == assembly.total) {
                val bytes = ByteBuffer.allocate(assembly.parts.values.sumOf { it.size }).apply {
                    for (index in 0 until assembly.total) put(assembly.parts[index])
                }.array()
                assemblies.remove(key)
                val envelope = runCatching {
                    ChinaWearPacketCodec.decode(bytes)
                }.getOrNull() ?: return
                scope.launch { handleEnvelope(device, envelope) }
            }
        }
    }

    private suspend fun handleEnvelope(device: BluetoothDevice, envelope: ChinaWearEnvelope) {
        if (envelope.version != ChinaWearProtocol.VERSION) return
        when (envelope.type) {
            "SYNC_REQUEST" -> handleSyncRequest(device, envelope)
            "SYNC_APPLIED" -> handleSyncApplied(device, envelope)
            "EXPORT_REQUEST" -> handleSnapshot(device, envelope)
            "PING" -> sendEnvelope(device, envelope.requestId, "PONG", "ok")
        }
    }

    private suspend fun handleSyncRequest(device: BluetoothDevice, envelope: ChinaWearEnvelope) {
        runCatching {
            val request = json.decodeFromString<SyncEnvelope>(envelope.payload)
            val localNodeId = localDeviceId()
            val applied = SyncApplier(database).applyOnce(
                requestId = request.requestId,
                sourceDeviceId = request.sourceDeviceId,
                records = request.records,
            )
            val localRecords = database.syncRecordDao().pending().map {
                SyncRecordPayload(it.id, it.entityId, it.entityType, it.operation, it.revision, it.updatedAt, it.deviceId, it.payloadJson)
            }
            val ack = SyncAck(
                requestId = request.requestId,
                sourceDeviceId = localNodeId,
                appliedRecordIds = applied,
                records = localRecords,
            )
            sendEnvelope(device, envelope.requestId, "SYNC_ACK", json.encodeToString(ack))
            SyncDiagnosticLogger.record(this, "china_ble_sync_received:${request.requestId}:${request.records.size}")
        }.onFailure {
            sendEnvelope(device, envelope.requestId, "SYNC_ERROR", it.message ?: "同步失败")
            SyncDiagnosticLogger.record(this, "china_ble_sync_error:${it.message}")
        }
    }

    private suspend fun handleSyncApplied(device: BluetoothDevice, envelope: ChinaWearEnvelope) {
        runCatching {
            val ack = json.decodeFromString<SyncAck>(envelope.payload)
            if (ack.appliedRecordIds.isNotEmpty()) {
                database.syncRecordDao().markSynced(ack.appliedRecordIds)
            }
            sendEnvelope(device, envelope.requestId, "SYNC_APPLIED", ack.requestId)
            SyncDiagnosticLogger.record(this, "china_ble_sync_applied:${ack.requestId}:${ack.appliedRecordIds.size}")
        }.onFailure {
            sendEnvelope(device, envelope.requestId, "SYNC_ERROR", it.message ?: "确认失败")
        }
    }

    private suspend fun handleSnapshot(device: BluetoothDevice, envelope: ChinaWearEnvelope) {
        runCatching {
            val bytes = android.util.Base64.decode(envelope.payload, android.util.Base64.DEFAULT)
            require(bytes.size <= 512 * 1024) { "完整同步包过大" }
            val timetables = com.hufeng943.timetable.shared.importexport.TimetableFileParser.parse(bytes)
            importService.importReplacingMatchesAtomic(
                timetables,
                envelope.requestId,
                localDeviceId(),
            )
            sendEnvelope(device, envelope.requestId, "SYNC_APPLIED", "snapshot")
            SyncDiagnosticLogger.record(this, "china_ble_snapshot_applied:${envelope.requestId}")
        }.onFailure {
            sendEnvelope(device, envelope.requestId, "SYNC_ERROR", it.message ?: "完整同步失败")
        }
    }

    private suspend fun sendEnvelope(device: BluetoothDevice, requestId: String, type: String, payload: String) {
        val bytes = ChinaWearPacketCodec.encode(
            ChinaWearEnvelope(
                requestId = requestId,
                type = type,
                timestamp = System.currentTimeMillis(),
                version = ChinaWearProtocol.VERSION,
                payload = payload,
            )
        )
        val mtu = currentMtu[device] ?: 23
        val chunkSize = (mtu - 3 - ChinaWearBleProtocol.WIRE_OVERHEAD).coerceAtLeast(1)
        val total = (bytes.size + chunkSize - 1) / chunkSize
        val transferId = transferCounter.incrementAndGet()
        repeat(total) { sequence ->
            val start = sequence * chunkSize
            val end = minOf(bytes.size, start + chunkSize)
            val frame = ChinaWearBleProtocol.encode(
                transferId,
                sequence,
                total,
                bytes.copyOfRange(start, end),
            )
            val characteristic = notifyCharacteristic ?: error("BLE 通知特征不存在")
            val notified = if (android.os.Build.VERSION.SDK_INT >= 33) {
                gattServer?.notifyCharacteristicChanged(device, characteristic, false, frame) == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = frame
                @Suppress("DEPRECATION")
                gattServer?.notifyCharacteristicChanged(device, characteristic, false) == true
            }
            check(notified) { "BLE 通知发送失败" }
            // BLE notification has no application ACK; a short inter-frame gap avoids overrunning
            // watches with small controller buffers while keeping the channel responsive.
            kotlinx.coroutines.delay(8)
        }
    }

    private fun localDeviceId(): String {
        val prefs = getSharedPreferences("sync_identity", MODE_PRIVATE)
        return prefs.getString("device_id", null) ?: "watch-${java.util.UUID.randomUUID()}".also {
            prefs.edit().putString("device_id", it).apply()
        }
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(CHANNEL_ID, "Timetable 国行同步", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Timetable")
            .setContentText("国行手表同步通道已待命")
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        advertiser?.stopAdvertising(advertiseCallback)
        advertiser = null
        gattServer?.close()
        gattServer = null
        notifyCharacteristic = null
        currentMtu.clear()
        assemblies.clear()
        scope.cancel()
        super.onDestroy()
    }

    private data class FrameAssembly(
        val total: Int,
        val parts: MutableMap<Int, ByteArray> = HashMap(),
    )

    companion object {
        private const val CHANNEL_ID = "china_wear_ble"
        private const val NOTIFICATION_ID = 305320
        private const val MAX_ASSEMBLY_BYTES = 2 * 1024 * 1024

        fun start(context: android.content.Context) {
            val intent = Intent(context, ChinaWearBleService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
