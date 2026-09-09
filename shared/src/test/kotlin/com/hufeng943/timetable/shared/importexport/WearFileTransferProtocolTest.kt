package com.hufeng943.timetable.shared.importexport

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WearFileTransferProtocolTest {
    @Test
    fun matchesLegacyAndRequestScopedPaths() {
        assertTrue(WearFileTransferProtocol.matchesPath(WearFileTransferProtocol.PATH))
        assertTrue(WearFileTransferProtocol.matchesPath(WearFileTransferProtocol.path("request-1")))
        assertFalse(WearFileTransferProtocol.matchesPath("/timetable/file-transfer/v10/request-1"))
        assertFalse(WearFileTransferProtocol.matchesPath(null))
    }
}
