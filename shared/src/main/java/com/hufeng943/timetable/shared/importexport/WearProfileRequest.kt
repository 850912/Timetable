package com.hufeng943.timetable.shared.importexport

/** Cross-module profile-open request used by the phone router and Wear senders. */
data class WearProfileRequest(
    val target: String,
    val appUri: String? = null,
    val fallbackUrl: String? = null,
)
