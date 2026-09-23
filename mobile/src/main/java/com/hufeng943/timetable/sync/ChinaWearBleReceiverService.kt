package com.hufeng943.timetable.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import com.hufeng943.timetable.TimetableDatabaseProvider
import com.hufeng943.timetable.shared.importexport.ChinaWearBleProtocol
import com.hufeng943.timetable.shared.importexport.ChinaWearExportPayload
import com.hufeng943.timetable.shared.importexport.ChinaWearPacketCodec
import com.hufeng943.timetable.shared.importexport.ChinaWearProtocol
import com.hufeng943.timetable.shared.importexport.WearProfileRequest
import com.hufeng943.timetable.shared.importexport.TimetableFileParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@AndroidEntryPoint
class ChinaWearBleReceiverService : Service() {
    @Inject lateinit var database: com.hufeng943.timetable.shared.data.database.AppDatabase
    @Inject lateinit var importService: com.hufeng943.timetable.shared.importexport.ImportService

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private val currentMtu = ConcurrentHashMap<BluetoothDevice, Int>()
    private val transferCounter = AtomicLong(System.nanoTime())
    private val assemblies = ConcurrentHashMap<String, FrameAssembly>()

    override fun onCreate() {
        super.onCreate()
        if (!hasPermissions()) { stopSelf(); return }
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
            SyncDiagnosticsReporter.recordError(this, "china_ble_phone_foreground_failed")
            stopSelf()
            return
        }
        startBle()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasPermissions(): Boolean = if (android.os.Build.VERSION.SDK_INT >= 31) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else true

    private fun startBle() {
        val manager = getSystemService(BluetoothManager::class.java) ?: return stopSelf()
        val adapter = manager.adapter ?: return stopSelf()
        if (!adapter.isEnabled || !packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) return stopSelf()

        val service = android.bluetooth.BluetoothGattService(ChinaWearBleProtocol.SERVICE_UUID, android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val write = BluetoothGattCharacteristic(
            ChinaWearBleProtocol.WRITE_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE,
        )
        notifyCharacteristic = BluetoothGattCharacteristic(
            ChinaWearBleProtocol.NOTIFY_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ,
        ).also {
            it.addDescriptor(BluetoothGattDescriptor(
                ChinaWearBleProtocol.CCCD_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
            ))
        }
        service.addCharacteristic(write)
        service.addCharacteristic(requireNotNull(notifyCharacteristic))
        gattServer = manager.openGattServer(this, callback)
        if (gattServer?.addService(service) != true) return stopSelf()

        advertiser = adapter.bluetoothLeAdvertiser ?: return stopSelf()
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(android.os.ParcelUuid(ChinaWearBleProtocol.SERVICE_UUID))
            .addServiceData(android.os.ParcelUuid(ChinaWearBleProtocol.SERVICE_UUID), byteArrayOf(ChinaWearBleProtocol.ROLE_PHONE))
            .build()
        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            SyncDiagnosticsReporter.recordError(this@ChinaWearBleReceiverService, "china_ble_phone_advertise_failed:$errorCode")
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
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            currentMtu[device] = mtu.coerceAtLeast(23)
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) {
            if (responseNeeded) gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) {
            if (characteristic.uuid != ChinaWearBleProtocol.WRITE_UUID) return
            if (responseNeeded) gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
            val frame = runCatching { ChinaWearBleProtocol.decode(value) }.getOrNull() ?: return
            if (frame.total !in 1..ChinaWearBleProtocol.MAX_FRAME_COUNT) return
            val key = "${device.address}:${frame.transferId}"
            val assembly = assemblies.getOrPut(key) { FrameAssembly(frame.total) }
            if (assembly.total != frame.total) { assemblies.remove(key); return }
            assembly.parts[frame.sequence] = frame.payload
            if (assembly.parts.values.sumOf { it.size } > MAX_ASSEMBLY_BYTES) {
                assemblies.remove(key)
                return
            }
            if (assembly.parts.size == assembly.total) {
                val bytes = ByteBuffer.allocate(assembly.parts.values.sumOf { it.size }).apply {
                    for (i in 0 until assembly.total) put(assembly.parts[i])
                }.array()
                assemblies.remove(key)
                val envelope = runCatching { ChinaWearPacketCodec.decode(bytes) }.getOrNull() ?: return
                scope.launch { handleEnvelope(device, envelope) }
            }
        }
    }

    private suspend fun handleEnvelope(device: BluetoothDevice, envelope: com.hufeng943.timetable.shared.importexport.ChinaWearEnvelope) {
        if (envelope.version != ChinaWearProtocol.VERSION) return
        when (envelope.type) {
            "EXPORT_REQUEST" -> handleExport(device, envelope)
            "PROFILE_OPEN_REQUEST" -> handleProfileOpen(device, envelope)
            "PING" -> sendEnvelope(device, envelope.requestId, "PONG", "ok")
        }
    }

    private suspend fun handleProfileOpen(device: BluetoothDevice, envelope: com.hufeng943.timetable.shared.importexport.ChinaWearEnvelope) {
        runCatching {
            val payload = json.decodeFromString<com.hufeng943.timetable.shared.importexport.ChinaWearProfilePayload>(envelope.payload)
            WearProfileIntentRouter.open(this, WearProfileRequest(payload.target, payload.appUri, payload.fallbackUrl))
            sendEnvelope(device, envelope.requestId, "PROFILE_OPEN_ACK", "queued")
            SyncDiagnosticsReporter.recordProfile(this, "profile_open_queued:${payload.target}:china_ble")
        }.onFailure {
            SyncDiagnosticsReporter.recordError(this, "profile_open_failed:${it.message}")
            sendEnvelope(device, envelope.requestId, "SYNC_ERROR", it.message ?: "主页打开失败")
        }
    }

    private suspend fun handleExport(device: BluetoothDevice, envelope: com.hufeng943.timetable.shared.importexport.ChinaWearEnvelope) {
        runCatching {
            val payload = json.decodeFromString<ChinaWearExportPayload>(envelope.payload)
            val exportBytes = android.util.Base64.decode(payload.exportBytesBase64, android.util.Base64.DEFAULT)
            val backupBytes = android.util.Base64.decode(payload.backupBytesBase64, android.util.Base64.DEFAULT)
            require(exportBytes.size <= 512 * 1024) { "导出文件过大" }
            require(backupBytes.size <= 512 * 1024) { "课表备份过大" }
            val timetables = TimetableFileParser.parse(backupBytes)
            importService.importReplacingMatchesAtomic(timetables, envelope.requestId, "wear-ble")
            saveDownload(payload.fileName, payload.mimeType, exportBytes)
            sendEnvelope(device, envelope.requestId, "EXPORT_ACK", "ok")
            SyncDiagnosticsReporter.recordExport(this, "china_ble_export_success:${envelope.requestId}:${payload.fileName}")
        }.onFailure {
            SyncDiagnosticsReporter.recordError(this, "china_ble_export_failed:${it.message}")
            sendEnvelope(device, envelope.requestId, "SYNC_ERROR", it.message ?: "导出失败")
        }
    }

    private fun saveDownload(fileName: String, mimeType: String, bytes: ByteArray) {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, sanitizeFileName(fileName))
            put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType)
            put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/Timetable")
            put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建手机端导出文件")
        try {
            contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("无法写入手机端导出文件")
            values.clear(); values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null); throw e
        }
    }

    private fun sanitizeFileName(name: String): String = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "Timetable_Export" }

    private suspend fun sendEnvelope(device: BluetoothDevice, requestId: String, type: String, payload: String) {
        val bytes = ChinaWearPacketCodec.encode(
            com.hufeng943.timetable.shared.importexport.ChinaWearEnvelope(
                requestId, type, System.currentTimeMillis(), ChinaWearProtocol.VERSION, payload
            )
        )
        val mtu = currentMtu[device] ?: 23
        val chunk = (mtu - 3 - ChinaWearBleProtocol.HEADER_SIZE).coerceAtLeast(1)
        val total = (bytes.size + chunk - 1) / chunk
        val transferId = transferCounter.incrementAndGet()
        require(total in 1..ChinaWearBleProtocol.MAX_FRAME_COUNT)
        repeat(total) { seq ->
            val start = seq * chunk
            val end = minOf(bytes.size, start + chunk)
            val characteristic = notifyCharacteristic ?: error("BLE 通知特征不存在")
            val chunkBytes = ChinaWearBleProtocol.encode(transferId, seq, total, bytes.copyOfRange(start, end))
            val notified = if (android.os.Build.VERSION.SDK_INT >= 33) {
                gattServer?.notifyCharacteristicChanged(device, characteristic, false, chunkBytes) == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = chunkBytes
                @Suppress("DEPRECATION")
                gattServer?.notifyCharacteristicChanged(device, characteristic, false) == true
            }
            check(notified) { "BLE 通知发送失败" }
            kotlinx.coroutines.delay(8)
        }
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(CHANNEL_ID, "Timetable 国行同步", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Timetable")
            .setContentText("国行手机同步通道已待命")
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        advertiser?.stopAdvertising(advertiseCallback)
        advertiser = null
        gattServer?.close(); gattServer = null
        notifyCharacteristic = null
        currentMtu.clear(); assemblies.clear(); scope.cancel()
        super.onDestroy()
    }

    private data class FrameAssembly(val total: Int, val parts: MutableMap<Int, ByteArray> = HashMap())

    companion object {
        private const val CHANNEL_ID = "china_phone_ble"
        private const val NOTIFICATION_ID = 305321
        private const val MAX_ASSEMBLY_BYTES = 2 * 1024 * 1024
        fun start(context: android.content.Context) {
            val intent = Intent(context, ChinaWearBleReceiverService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
