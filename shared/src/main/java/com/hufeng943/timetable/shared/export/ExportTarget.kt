package com.hufeng943.timetable.shared.export

/**
 * Wear export destination. Every option is fulfilled on the paired phone:
 * PHONE_APP imports a canonical backup into the phone app,
 * PHONE_FILE saves the selected file format into the phone Downloads folder,
 * BOTH performs both operations.
 */
enum class ExportTarget {
    PHONE_APP,
    PHONE_FILE,
    BOTH;

    val importsIntoPhoneApp: Boolean
        get() = this == PHONE_APP || this == BOTH

    val savesFileOnPhone: Boolean
        get() = this == PHONE_FILE || this == BOTH
}
