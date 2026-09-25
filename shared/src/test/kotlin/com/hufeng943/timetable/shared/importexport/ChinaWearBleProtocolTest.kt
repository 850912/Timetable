package com.hufeng943.timetable.shared.importexport

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChinaWearBleProtocolTest {
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

    @Test
    fun checkedFrameRoundTripsAndRejectsCorruption() {
        val encoded = ChinaWearBleProtocol.encodeChecked(1L, 0, 1, byteArrayOf(1, 2, 3))
        assertArrayEquals(byteArrayOf(1, 2, 3), ChinaWearBleProtocol.decode(encoded).payload)
        encoded[encoded.lastIndex - 1] = (encoded[encoded.lastIndex - 1].toInt() xor 1).toByte()
        assertThrows(IllegalArgumentException::class.java) {
            ChinaWearBleProtocol.decode(encoded)
        }
    }
}
