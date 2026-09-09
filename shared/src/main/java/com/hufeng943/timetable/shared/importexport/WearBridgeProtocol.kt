package com.hufeng943.timetable.shared.importexport

/** Bootstrap handshake used for Samsung/Wear OS devices where the phone-side
 * Wearable API may not become available until the watch has initiated traffic. */
object WearBridgeProtocol {
    const val HELLO_PATH = "/timetable/file-transfer/v1/bootstrap/hello"
    const val READY_PATH = "/timetable/file-transfer/v1/bootstrap/ready"
    const val PROTOCOL_VERSION = 1

    const val PREFS = "wear_bridge_state"
    const val KEY_READY_NODE_ID = "ready_node_id"
    const val KEY_READY_AT = "ready_at"
    const val READY_TTL_MS = 10 * 60 * 1000L
}
