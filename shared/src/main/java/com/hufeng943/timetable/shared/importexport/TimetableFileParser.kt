package com.hufeng943.timetable.shared.importexport

import com.hufeng943.timetable.shared.export.BackupManager
import com.hufeng943.timetable.shared.export.CsvImporter
import com.hufeng943.timetable.shared.export.IcsImporter
import com.hufeng943.timetable.shared.model.Timetable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream

/** Parses any timetable file format currently supported by the app. */
object TimetableFileParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(bytes: ByteArray): List<Timetable> {
        val content = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val trimmed = content.trimStart()

        return when {
            trimmed.startsWith("{") || trimmed.startsWith("[") -> parseJson(bytes, content)
            content.contains("BEGIN:VCALENDAR", ignoreCase = true) -> IcsImporter.parseIcs(content)
            content.contains("学期,课程名称") || content.contains("星期") -> CsvImporter.parseCsv(content)
            else -> throw IllegalArgumentException("无法识别的文件格式，仅支持 .json/.ics/.csv")
        }.also {
            require(it.isNotEmpty()) { "未在文件中解析到有效课表" }
        }
    }

    private fun parseJson(bytes: ByteArray, content: String): List<Timetable> {
        runCatching { BackupManager.restore(ByteArrayInputStream(bytes)) }
            .getOrNull()
            ?.let { return it }
        runCatching { json.decodeFromString<List<Timetable>>(content) }
            .getOrNull()
            ?.let { return it }
        runCatching { json.decodeFromString<Timetable>(content) }
            .getOrNull()
            ?.let { return listOf(it) }
        throw IllegalArgumentException("JSON 课表文件格式不受支持或文件已损坏")
    }
}
