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
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class ChinaWearBleClient(private val context: Context) {
    companion object {
        private const val SCAN_TIMEOUT_MS = 8_000L
        private const val CONNECT_TIMEOUT_MS = 12_000L
        private const val RESPONSE_TIMEOUT_MS = 20_000L
        private const val OP_TIMEOUT_MS = 8_000L
        private const val REQUESTED_MTU = 517
        private const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
    }

    suspend fun request(envelope: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        requirePermissions()
        val session = GattSession.connect(context, scanForPhone())
        try {
            // User-initiated sync/export should favor a short reliable transfer over background
            // power saving. This also improves interoperability with Xiaomi/China-ROM GATT stacks.
            session.requestHighPriority()
            session.enableNotifications()
            session.requestMtu(REQUESTED_MTU)
            session.write(envelope)
            withTimeout(RESPONSE_TIMEOUT_MS) { session.state.response.await() }
        } finally { session.close() }
    }

    private fun requirePermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            check(ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) { "未授予 BLE 扫描权限" }
            check(ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) { "未授予 BLE 连接权限" }
        } else if (android.os.Build.VERSION.SDK_INT >= 23) {
            check(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) { "未授予 BLE 扫描所需的位置权限" }
        }
    }

    private suspend fun scanForPhone(): BluetoothDevice {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: error("设备不支持蓝牙")
        check(adapter.isEnabled) { "蓝牙未开启" }
        val scanner = adapter.bluetoothLeScanner ?: error("无法获取 BLE 扫描器")
        val found = CompletableDeferred<BluetoothDevice>()
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val record = result.scanRecord ?: return
                val uuids = record.serviceUuids.orEmpty()
                val role = record.getServiceData(ParcelUuid(ChinaWearBleProtocol.SERVICE_UUID))?.firstOrNull()
                if (uuids.any { it.uuid == ChinaWearBleProtocol.SERVICE_UUID } && role == ChinaWearBleProtocol.ROLE_PHONE) found.complete(result.device)
            }
            override fun onScanFailed(errorCode: Int) { found.completeExceptionally(IllegalStateException("BLE 扫描失败($errorCode)")) }
        }
        try {
            scanner.startScan(listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(ChinaWearBleProtocol.SERVICE_UUID)).build()),
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), cb)
            return withTimeout(SCAN_TIMEOUT_MS) { found.await() }
        } finally { runCatching { scanner.stopScan(cb) } }
    }

    private class GattSession private constructor(
        private val gatt: BluetoothGatt,
        private val writeCharacteristic: BluetoothGattCharacteristic,
        private val notifyCharacteristic: BluetoothGattCharacteristic,
        val state: State,
    ) {
        private var mtu = 23
        private var transferId = System.nanoTime()

        suspend fun enableNotifications() {
            check(gatt.setCharacteristicNotification(notifyCharacteristic, true))
            val descriptor = notifyCharacteristic.getDescriptor(ChinaWearBleProtocol.CCCD_UUID) ?: error("缺少 CCCD")
            val waiter = CompletableDeferred<Unit>(); state.descriptorWaiter = waiter
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            try { check(gatt.writeDescriptor(descriptor)); withTimeout(OP_TIMEOUT_MS) { waiter.await() } }
            finally { if (state.descriptorWaiter === waiter) state.descriptorWaiter = null }
        }

        fun requestHighPriority() {
            runCatching { gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH) }
        }

        suspend fun requestMtu(requested: Int) {
            val waiter = CompletableDeferred<Unit>(); state.mtuWaiter = waiter
            try {
                if (!gatt.requestMtu(requested)) return
                // Some vendor stacks negotiate successfully but fail to deliver the callback.
                // Continue with the safe 23-byte MTU rather than failing the whole transfer.
                val completed = withTimeoutOrNull(OP_TIMEOUT_MS) { waiter.await(); true } ?: false
                if (completed) mtu = state.mtu
            } finally {
                if (state.mtuWaiter === waiter) state.mtuWaiter = null
            }
        }

        suspend fun write(envelope: ByteArray) {
            val bytes = envelope
            val size = (mtu - 3 - ChinaWearBleProtocol.HEADER_SIZE).coerceAtLeast(1)
            val total = (bytes.size + size - 1) / size
            require(total in 1..ChinaWearBleProtocol.MAX_FRAME_COUNT) { "导出数据过大" }
            repeat(total) { seq ->
                val frame = ChinaWearBleProtocol.encode(transferId, seq, total, bytes.copyOfRange(seq * size, minOf(bytes.size, (seq + 1) * size)))
                val waiter = CompletableDeferred<Unit>(); state.writeWaiter = waiter
                try {
                    val started = if (android.os.Build.VERSION.SDK_INT >= 33) {
                        gatt.writeCharacteristic(writeCharacteristic, frame, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS
                    } else {
                        @Suppress("DEPRECATION") writeCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        @Suppress("DEPRECATION") writeCharacteristic.value = frame
                        @Suppress("DEPRECATION") gatt.writeCharacteristic(writeCharacteristic)
                    }
                    check(started); withTimeout(OP_TIMEOUT_MS) { waiter.await() }
                } finally { if (state.writeWaiter === waiter) state.writeWaiter = null }
            }
        }

        fun close() { runCatching { gatt.disconnect() }; runCatching { gatt.close() } }

        companion object {
            suspend fun connect(context: Context, device: BluetoothDevice): GattSession = withTimeout(CONNECT_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    val state = State()
                    var gatt: BluetoothGatt? = null
                    val cb = object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                            if (!cont.isActive) { g.close(); return }
                            if (status != BluetoothGatt.GATT_SUCCESS) { cont.resumeWithException(IllegalStateException("BLE 连接失败($status)")); g.close(); return }
                            when (newState) {
                                BluetoothProfile.STATE_CONNECTED -> if (!g.discoverServices()) { cont.resumeWithException(IllegalStateException("服务发现启动失败")); g.close() }
                                BluetoothProfile.STATE_DISCONNECTED -> { cont.resumeWithException(IllegalStateException("BLE 连接已断开")); g.close() }
                            }
                        }
                        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                            if (!cont.isActive) { g.close(); return }
                            if (status != BluetoothGatt.GATT_SUCCESS) { cont.resumeWithException(IllegalStateException("服务发现失败($status)")); g.close(); return }
                            val svc = g.getService(ChinaWearBleProtocol.SERVICE_UUID)
                            val w = svc?.getCharacteristic(ChinaWearBleProtocol.WRITE_UUID)
                            val n = svc?.getCharacteristic(ChinaWearBleProtocol.NOTIFY_UUID)
                            if (w == null || n == null) { cont.resumeWithException(IllegalStateException("未找到手机 BLE 服务")); g.close(); return }
                            cont.resume(GattSession(g, w, n, state))
                        }
                        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) { state.mtu = if (status == 0) mtu.coerceAtLeast(23) else 23; state.mtuWaiter?.complete(Unit) }
                        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) { if (status == 0) state.descriptorWaiter?.complete(Unit) else state.descriptorWaiter?.completeExceptionally(IllegalStateException("CCCD 写入失败($status)")) }
                        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) { if (status == 0) state.writeWaiter?.complete(Unit) else state.writeWaiter?.completeExceptionally(IllegalStateException("BLE 写入失败($status)")) }
                        @Suppress("DEPRECATION") override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) { if (c.uuid == ChinaWearBleProtocol.NOTIFY_UUID) state.onFrame(c.value) }
                        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) { if (c.uuid == ChinaWearBleProtocol.NOTIFY_UUID) state.onFrame(value) }
                    }
                    gatt = if (android.os.Build.VERSION.SDK_INT >= 23) device.connectGatt(context, false, cb, BluetoothDevice.TRANSPORT_LE) else device.connectGatt(context, false, cb)
                    if (gatt == null) cont.resumeWithException(IllegalStateException("无法创建 GATT"))
                    cont.invokeOnCancellation { gatt?.close() }
                }
            }
        }
    }

    private class State {
        val response = CompletableDeferred<ByteArray>()
        var descriptorWaiter: CompletableDeferred<Unit>? = null
        var writeWaiter: CompletableDeferred<Unit>? = null
        var mtuWaiter: CompletableDeferred<Unit>? = null
        var mtu = 23
        private val assemblies = HashMap<Long, Assembly>()
        @Synchronized fun onFrame(bytes: ByteArray) {
            val frame = runCatching { ChinaWearBleProtocol.decode(bytes) }.getOrNull() ?: return
            val a = assemblies.getOrPut(frame.transferId) { Assembly(frame.total) }
            if (a.total != frame.total) { assemblies.remove(frame.transferId); return }
            a.parts[frame.sequence] = frame.payload
            if (a.parts.size == a.total) {
                val size = a.parts.values.sumOf { it.size }
                if (size > MAX_RESPONSE_BYTES) { response.completeExceptionally(IllegalStateException("响应过大")); return }
                val bytesOut = ByteBuffer.allocate(size).apply { for (i in 0 until a.total) put(a.parts[i]) }.array()
                assemblies.remove(frame.transferId)
                response.complete(bytesOut)
            }
        }
        data class Assembly(val total: Int, val parts: MutableMap<Int, ByteArray> = HashMap())
    }
}
