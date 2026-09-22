package com.hufeng943.timetable.shared.export

/**
 * Build12 export destination.
 * PHONE_APP: send to mobile importer
 * DOWNLOAD: save through Storage Access Framework
 * BOTH: execute both flows
 */
enum class ExportTarget {
    PHONE_APP,
    DOWNLOAD,
    BOTH
}
