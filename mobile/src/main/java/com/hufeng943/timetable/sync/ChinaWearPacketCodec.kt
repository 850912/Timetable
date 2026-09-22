package com.hufeng943.timetable.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ChinaWearEnvelope(
    val requestId: String,
    val type: String,
    val timestamp: Long,
    val version: Int,
    val payload: String = ""
)

object ChinaWearPacketCodec {
    private val json = Json { encodeDefaults = true }

    fun encode(packet: ChinaWearEnvelope): ByteArray =
        json.encodeToString(packet).encodeToByteArray()

    fun decode(bytes: ByteArray): ChinaWearEnvelope =
        json.decodeFromString(bytes.decodeToString())
}
