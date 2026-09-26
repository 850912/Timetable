package com.hufeng943.timetable.shared.importexport

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Transport-neutral envelope shared by Google/China/BLE adapters. */
@Serializable
data class ChinaWearEnvelope(
    val requestId: String,
    val type: String,
    val timestamp: Long,
    val version: Int,
    val payload: String = "",
)

object ChinaWearEnvelopeSecurity {
    const val MAX_CLOCK_SKEW_MS = 5 * 60 * 1000L

    fun isFresh(envelope: ChinaWearEnvelope, nowMs: Long = System.currentTimeMillis()): Boolean =
        kotlin.math.abs(nowMs - envelope.timestamp) <= MAX_CLOCK_SKEW_MS
}

object ChinaWearPacketCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(packet: ChinaWearEnvelope): ByteArray =
        json.encodeToString(packet).encodeToByteArray()

    fun decode(bytes: ByteArray): ChinaWearEnvelope =
        json.decodeFromString(bytes.decodeToString())
}
