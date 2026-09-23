package com.hufeng943.timetable.shared.importexport

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/** Small, transport-neutral framing used by the China compatibility BLE channel. */
object ChinaWearBleProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("7e8c4e01-2b50-4d84-9a8e-4b1f4a3d5101")
    val WRITE_UUID: UUID = UUID.fromString("7e8c4e02-2b50-4d84-9a8e-4b1f4a3d5101")
    val NOTIFY_UUID: UUID = UUID.fromString("7e8c4e03-2b50-4d84-9a8e-4b1f4a3d5101")
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val DEVICE_NAME_PREFIX = "Timetable-"
    const val FRAME_VERSION: Byte = 1
    const val HEADER_SIZE = 16 // version + kind + transferId + sequence + total
    const val KIND_DATA: Byte = 1
    const val MAX_FRAME_COUNT = 8192
    const val ROLE_PHONE: Byte = 1
    const val ROLE_WATCH: Byte = 2

    data class Frame(
        val transferId: Long,
        val sequence: Int,
        val total: Int,
        val payload: ByteArray,
    )

    fun encode(
        transferId: Long,
        sequence: Int,
        total: Int,
        payload: ByteArray,
    ): ByteArray {
        require(sequence in 0 until total) { "无效 BLE 分片序号" }
        require(total in 1..MAX_FRAME_COUNT) { "BLE 分片总数超出上限" }
        return ByteBuffer.allocate(HEADER_SIZE + payload.size)
            .order(ByteOrder.BIG_ENDIAN)
            .put(FRAME_VERSION)
            .put(KIND_DATA)
            .putLong(transferId)
            .putInt(sequence)
            .putShort(total.toShort())
            .put(payload)
            .array()
    }

    fun decode(bytes: ByteArray): Frame {
        require(bytes.size >= HEADER_SIZE) { "BLE 数据帧过短" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        require(buffer.get() == FRAME_VERSION) { "BLE 帧版本不支持" }
        require(buffer.get() == KIND_DATA) { "BLE 帧类型不支持" }
        val transferId = buffer.long
        val sequence = buffer.int
        val total = buffer.short.toInt() and 0xffff
        require(total in 1..MAX_FRAME_COUNT && sequence in 0 until total) { "BLE 帧序列无效" }
        val payload = ByteArray(buffer.remaining())
        buffer.get(payload)
        return Frame(transferId, sequence, total, payload)
    }
}
