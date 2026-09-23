package com.hufeng943.timetable.shared.importexport

import kotlinx.serialization.Serializable

@Serializable
data class ChinaWearExportPayload(
    val fileName: String,
    val mimeType: String,
    val exportBytesBase64: String,
    val backupBytesBase64: String,
)
