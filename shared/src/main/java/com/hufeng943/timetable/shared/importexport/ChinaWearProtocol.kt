package com.hufeng943.timetable.shared.importexport

/**
 * Build11 final protocol.
 *
 * Transport independent. The channel can be Google Data Layer,
 * vendor bridge or other adapters.
 */
object ChinaWearProtocol {
    const val EXPORT_REQUEST = "/timetable/china/export/request"
    const val EXPORT_PACKAGE = "/timetable/china/export/package"
    const val SYNC_REQUEST = "/timetable/china/sync/request"
    const val SYNC_ACK = "/timetable/china/sync/ack"
    const val SYNC_APPLIED = "/timetable/china/sync/applied"
    const val SYNC_ERROR = "/timetable/china/sync/error"
    const val PING = "/timetable/china/ping"
    const val VERSION = 2
}
