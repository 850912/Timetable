package com.hufeng943.timetable.sync

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.hufeng943.timetable.shared.importexport.ChinaWearBleProtocol
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class ChinaWearBleClient(private val context: Context) {
    companion object {
        private const val SCAN_TIMEOUT_MS = 8_000L
        private const val CONNECT_TIMEOUT_MS = 12_000L
        private const val RESPONSE_TIMEOUT_MS = 20_000L
        private const val GATT_OPERATION_TIMEOUT_MS = 8_000L
        private const val REQUESTED_MTU = 517
        private const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
    }

    suspend fun request(envelope: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        requireBluetoothPermissions()
        val device = scanForWatch()
        val session = GattSession.connect(context, device)
        try {
            session.enableNotifications()
            session.requestMtu(REQUESTED_MTU)
            session.writeEnvelope(envelope)
            withTimeout(RESPONSE_TIMEOUT_MS) { session.awaitResponse() }
        } finally {
            session.close()
        }
    }

    private fun requireBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            check(ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                "未授予蓝牙扫描权限"
            }
            check(ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                "未授予蓝牙连接权限"
            }
        } else {
            check(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                "未授予 BLE 扫描所需的位置权限"
            }
        }
    }

    private suspend fun scanForWatch(): BluetoothDevice {
        val manager = context.getSystemService(BluetoothManager::class.java)
            ?: error("设备不支持蓝牙")
        val adapter = manager.adapter ?: error("设备不支持蓝牙")
        check(adapter.isEnabled) { "蓝牙未开启" }
        val scanner = adapter.bluetoothLeScanner ?: error("无法获取 BLE 扫描器")
        val found = CompletableDeferred<BluetoothDevice>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val record = result.scanRecord ?: return
                val uuids = record.serviceUuids.orEmpty()
                val role = record.getServiceData(ParcelUuid(ChinaWearBleProtocol.SERVICE_UUID))?.firstOrNull()
                if (uuids.any { it.uuid == ChinaWearBleProtocol.SERVICE_UUID } &&
                    role == ChinaWearBleProtocol.ROLE_WATCH
                ) {
                    found.complete(result.device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                found.completeExceptionally(IllegalStateException("BLE 扫描失败($errorCode)"))
            }
        }
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(ChinaWearBleProtocol.SERVICE_UUID))
                .build()
        )
        try {
            scanner.startScan(
                filters,
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .build(),
                callback,
            )
            return withTimeout(SCAN_TIMEOUT_MS) { found.await() }
        } finally {
            runCatching { scanner.stopScan(callback) }
        }
    }

    private class GattSession private constructor(
        private val gatt: BluetoothGatt,
        private val writeCharacteristic: BluetoothGattCharacteristic,
        private val notifyCharacteristic: BluetoothGattCharacteristic,
        private val state: CallbackState,
    ) {
        private var transferId = System.nanoTime()
        private var mtu = 23

        suspend fun enableNotifications() {
            check(gatt.setCharacteristicNotification(notifyCharacteristic, true)) {
                "无法启用 BLE 通知"
            }
            val descriptor = notifyCharacteristic.getDescriptor(ChinaWearBleProtocol.CCCD_UUID)
                ?: error("BLE 通知特征缺少 CCCD")
            val waiter = CompletableDeferred<Unit>()
            state.descriptorWaiter = waiter
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            try {
                check(gatt.writeDescriptor(descriptor)) { "写入 BLE CCCD 失败" }
                withTimeout(GATT_OPERATION_TIMEOUT_MS) { waiter.await() }
            } finally {
                if (state.descriptorWaiter === waiter) state.descriptorWaiter = null
            }
        }

        suspend fun requestMtu(requested: Int) {
            val waiter = CompletableDeferred<Unit>()
            state.mtuWaiter = waiter
            try {
                if (!gatt.requestMtu(requested)) return
                withTimeout(GATT_OPERATION_TIMEOUT_MS) { waiter.await() }
                mtu = state.mtu
            } finally {
                if (state.mtuWaiter === waiter) state.mtuWaiter = null
            }
        }

        suspend fun writeEnvelope(bytes: ByteArray) {
            val chunkSize = (mtu - 3 - ChinaWearBleProtocol.HEADER_SIZE).coerceAtLeast(1)
            val total = (bytes.size + chunkSize - 1) / chunkSize
            require(total in 1..ChinaWearBleProtocol.MAX_FRAME_COUNT) { "同步数据过大" }
            repeat(total) { sequence ->
                val start = sequence * chunkSize
                val end = minOf(bytes.size, start + chunkSize)
                val frame = ChinaWearBleProtocol.encode(
                    transferId = transferId,
                    sequence = sequence,
                    total = total,
                    payload = bytes.copyOfRange(start, end),
                )
                val waiter = CompletableDeferred<Unit>()
                state.writeWaiter = waiter
                try {
                    val started = if (android.os.Build.VERSION.SDK_INT >= 33) {
                        gatt.writeCharacteristic(
                            writeCharacteristic,
                            frame,
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                        ) == BluetoothGatt.GATT_SUCCESS
                    } else {
                        @Suppress("DEPRECATION")
                        writeCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        @Suppress("DEPRECATION")
                        writeCharacteristic.value = frame
                        @Suppress("DEPRECATION")
                        gatt.writeCharacteristic(writeCharacteristic)
                    }
                    check(started) { "写入 BLE 数据失败" }
                    withTimeout(GATT_OPERATION_TIMEOUT_MS) { waiter.await() }
                } finally {
                    if (state.writeWaiter === waiter) state.writeWaiter = null
                }
            }
        }

        suspend fun awaitResponse(): ByteArray = state.response.await()

        fun close() {
            runCatching { gatt.disconnect() }
            runCatching { gatt.close() }
        }

        companion object {
            suspend fun connect(context: Context, device: BluetoothDevice): GattSession =
                withTimeout(CONNECT_TIMEOUT_MS) {
                    suspendCancellableCoroutine { continuation ->
                        val state = CallbackState()
                        var gatt: BluetoothGatt? = null
                        val callback = object : BluetoothGattCallback() {
                            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                                if (!continuation.isActive) {
                                    g.close()
                                    return
                                }
                                if (status != BluetoothGatt.GATT_SUCCESS) {
                                    continuation.resumeWithException(IllegalStateException("BLE 连接失败($status)"))
                                    g.close()
                                    return
                                }
                                if (newState == BluetoothProfile.STATE_CONNECTED) {
                                    if (!g.discoverServices()) {
                                        continuation.resumeWithException(IllegalStateException("BLE 服务发现启动失败"))
                                        g.close()
                                    }
                                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                    continuation.resumeWithException(IllegalStateException("BLE 连接已断开"))
                                    g.close()
                                }
                            }

                            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                                if (!continuation.isActive) {
                                    g.close()
                                    return
                                }
                                if (status != BluetoothGatt.GATT_SUCCESS) {
                                    continuation.resumeWithException(IllegalStateException("BLE 服务发现失败($status)"))
                                    g.close()
                                    return
                                }
                                val service = g.getService(ChinaWearBleProtocol.SERVICE_UUID)
                                val write = service?.getCharacteristic(ChinaWearBleProtocol.WRITE_UUID)
                                val notify = service?.getCharacteristic(ChinaWearBleProtocol.NOTIFY_UUID)
                                if (service == null || write == null || notify == null) {
                                    continuation.resumeWithException(IllegalStateException("未找到 Timetable BLE 服务"))
                                    g.close()
                                    return
                                }
                                continuation.resume(GattSession(g, write, notify, state))
                            }

                            override fun onMtuChanged(g: BluetoothGatt, mtuValue: Int, status: Int) {
                                state.mtu = if (status == BluetoothGatt.GATT_SUCCESS) mtuValue.coerceAtLeast(23) else 23
                                state.mtuWaiter?.complete(Unit)
                            }

                            override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    state.descriptorWaiter?.complete(Unit)
                                } else {
                                    state.descriptorWaiter?.completeExceptionally(
                                        IllegalStateException("BLE CCCD 写入失败($status)")
                                    )
                                }
                            }

                            override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    state.writeWaiter?.complete(Unit)
                                } else {
                                    state.writeWaiter?.completeExceptionally(
                                        IllegalStateException("BLE 写入回调失败($status)")
                                    )
                                }
                            }

                            @Suppress("DEPRECATION")
                            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                                if (characteristic.uuid == ChinaWearBleProtocol.NOTIFY_UUID) {
                                    state.onFrame(characteristic.value)
                                }
                            }

                            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                                if (characteristic.uuid == ChinaWearBleProtocol.NOTIFY_UUID) {
                                    state.onFrame(value)
                                }
                            }
                        }
                        gatt = if (android.os.Build.VERSION.SDK_INT >= 23) {
                            device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
                        } else {
                            @Suppress("DEPRECATION")
                            device.connectGatt(context, false, callback)
                        }
                        if (gatt == null) {
                            continuation.resumeWithException(IllegalStateException("无法创建 BLE GATT 连接"))
                        }
                        continuation.invokeOnCancellation { gatt?.close() }
                    }
                }
        }
    }

    private class CallbackState {
        val response = CompletableDeferred<ByteArray>()
        var descriptorWaiter: CompletableDeferred<Unit>? = null
        var writeWaiter: CompletableDeferred<Unit>? = null
        var mtuWaiter: CompletableDeferred<Unit>? = null
        var mtu: Int = 23
        private val assemblies = HashMap<Long, Assembly>()

        @Synchronized
        fun onFrame(bytes: ByteArray) {
            val frame = runCatching { ChinaWearBleProtocol.decode(bytes) }.getOrNull() ?: return
            if (frame.total !in 1..ChinaWearBleProtocol.MAX_FRAME_COUNT || frame.sequence !in 0 until frame.total) return
            val assembly = assemblies.getOrPut(frame.transferId) { Assembly(frame.total) }
            if (assembly.total != frame.total) {
                assemblies.remove(frame.transferId)
                return
            }
            assembly.parts[frame.sequence] = frame.payload
            if (assembly.parts.size == assembly.total) {
                val size = assembly.parts.values.sumOf { it.size }
                if (size > MAX_RESPONSE_BYTES) {
                    assemblies.remove(frame.transferId)
                    response.completeExceptionally(IllegalStateException("BLE 响应过大"))
                    return
                }
                val combined = ByteBuffer.allocate(size).apply {
                    for (index in 0 until assembly.total) put(assembly.parts[index])
                }.array()
                assemblies.remove(frame.transferId)
                response.complete(combined)
            }
        }

        private data class Assembly(
            val total: Int,
            val parts: MutableMap<Int, ByteArray> = HashMap(),
        )
    }
}
