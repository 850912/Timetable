package com.hufeng943.timetable.shared.export

sealed interface ExportResult {
    data class Success(val message: String): ExportResult
    data class Failed(val reason: String): ExportResult
}
