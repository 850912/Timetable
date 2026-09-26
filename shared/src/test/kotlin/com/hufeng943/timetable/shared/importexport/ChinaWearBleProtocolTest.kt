package com.hufeng943.timetable.shared.importexport

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChinaWearBleProtocolTest {
    @Test
    fun envelopeFreshnessRejectsReplayAndAcceptsCurrentMessages() {
        val now = 1_700_000_000_000L
        val current = ChinaWearEnvelope("id", "PING", now, ChinaWearProtocol.VERSION)
        val replayed = current.copy(timestamp = now - ChinaWearEnvelopeSecurity.MAX_CLOCK_SKEW_MS - 1)
        assertEquals(true, ChinaWearEnvelopeSecurity.isFresh(current, now))
        assertEquals(false, ChinaWearEnvelopeSecurity.isFresh(replayed, now))
    }

    @Test
    fun frameRoundTripsBinaryPayload() {
        val payload = byteArrayOf(0x00, 0x7f, 0x80.toByte(), 0xff.toByte(), 0x01)
        val encoded = ChinaWearBleProtocol.encode(123456789L, 7, 9, payload)
        val decoded = ChinaWearBleProtocol.decode(encoded)
        assertEquals(123456789L, decoded.transferId)
        assertEquals(7, decoded.sequence)
        assertEquals(9, decoded.total)
        assertArrayEquals(payload, decoded.payload)
    }

    @Test
    fun frameCountIsBounded() {
        assertThrows(IllegalArgumentException::class.java) {
            ChinaWearBleProtocol.encode(1L, 0, ChinaWearBleProtocol.MAX_FRAME_COUNT + 1, byteArrayOf(1))
        }
    }

    @Test
    fun headerSizeMatchesWireLayout() {
        val encoded = ChinaWearBleProtocol.encode(1L, 0, 1, byteArrayOf(1, 2, 3))
        assertEquals(ChinaWearBleProtocol.HEADER_SIZE + 3, encoded.size)
    }
}
