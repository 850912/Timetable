package com.hufeng943.timetable.shared.importexport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearFileTransferProtocolTest {
    @Test
    fun matchesLegacyAndRequestScopedPaths() {
        assertTrue(WearFileTransferProtocol.matchesPath(WearFileTransferProtocol.PATH))
        assertTrue(WearFileTransferProtocol.matchesPath(WearFileTransferProtocol.path("request-1")))
        assertFalse(WearFileTransferProtocol.matchesPath("/timetable/file-transfer/v10/request-1"))
        assertFalse(WearFileTransferProtocol.matchesPath(null))
    }
}
