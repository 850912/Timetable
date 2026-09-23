package com.hufeng943.timetable.shared.importexport

import kotlinx.serialization.Serializable

@Serializable
data class ChinaWearProfilePayload(
    val target: String,
    val appUri: String? = null,
    val fallbackUrl: String? = null,
)
