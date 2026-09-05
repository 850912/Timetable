package com.hufeng943.timetable.shared.export

import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

object CsvImporter {

    private data class CsvRow(
        val semesterName: String,
        val courseName: String,
        val teacher: String?,
        val location: String?,
        val dayOfWeek: DayOfWeek,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val recurrence: WeekPattern,
        val remark: String?
    )

    fun parseCsv(csvContent: String): List<Timetable> {
        val cleanContent = csvContent.removePrefix("\uFEFF").trim()
        val lines = cleanContent.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val rows = mutableListOf<CsvRow>()
        for (i in 1 until lines.size) {
            val tokens = parseCsvLine(lines[i])
            if (tokens.size < 7) continue

            val semName = tokens.getOrNull(0)?.trim()?.ifEmpty { "导入课表" } ?: "导入课表"
            val courseName = tokens.getOrNull(1)?.trim() ?: continue
            if (courseName.isEmpty()) continue

            val teacher = tokens.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
            val location = tokens.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() }
            val dayStr = tokens.getOrNull(4)?.trim() ?: ""
            val startStr = tokens.getOrNull(5)?.trim() ?: ""
            val endStr = tokens.getOrNull(6)?.trim() ?: ""
            val recStr = tokens.getOrNull(7)?.trim() ?: ""
            val remark = tokens.getOrNull(8)?.trim()?.takeIf { it.isNotEmpty() }

            val dayOfWeek = parseDayOfWeek(dayStr) ?: DayOfWeek.MONDAY
            val startTime = parseTime(startStr) ?: LocalTime(8, 0)
            val endTime = parseTime(endStr) ?: LocalTime(9, 40)
            val recurrence = when (recStr) {
                "单周" -> WeekPattern.ODD_WEEK
                "双周" -> WeekPattern.EVEN_WEEK
                else -> WeekPattern.EVERY_WEEK
            }

            rows.add(
                CsvRow(
                    semesterName = semName,
                    courseName = courseName,
                    teacher = teacher,
                    location = location,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    recurrence = recurrence,
                    remark = remark
                )
            )
        }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return rows.groupBy { it.semesterName }.map { (semesterName, sRows) ->
            val courses = sRows.groupBy { Triple(it.courseName, it.teacher, it.location) }.map { (info, cRows) ->
                val (cName, cTeacher, cLocation) = info
                val slots = cRows.map { r ->
                    TimeSlot(
                        id = 0,
                        dayOfWeek = r.dayOfWeek,
                        startTime = r.startTime,
                        endTime = r.endTime,
                        recurrence = r.recurrence,
                        remark = r.remark
                    )
                }
                Course(
                    id = 0,
                    name = cName,
                    teacher = cTeacher,
                    location = cLocation,
                    timeSlots = slots
                )
            }

            Timetable(
                timetableId = 0,
                semesterName = semesterName,
                createdAt = Clock.System.now(),
                semesterStart = today,
                semesterEnd = LocalDate.fromEpochDays(today.toEpochDays() + 140),
                allCourses = courses,
                color = -1L
            )
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    private fun parseDayOfWeek(str: String): DayOfWeek? {
        return when {
            str.contains("一") || str.contains("1") -> DayOfWeek.MONDAY
            str.contains("二") || str.contains("2") -> DayOfWeek.TUESDAY
            str.contains("三") || str.contains("3") -> DayOfWeek.WEDNESDAY
            str.contains("四") || str.contains("4") -> DayOfWeek.THURSDAY
            str.contains("五") || str.contains("5") -> DayOfWeek.FRIDAY
            str.contains("六") || str.contains("6") -> DayOfWeek.SATURDAY
            str.contains("日") || str.contains("天") || str.contains("7") -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun parseTime(str: String): LocalTime? {
        return try {
            val parts = str.split(":")
            if (parts.size >= 2) {
                LocalTime(parts[0].toInt(), parts[1].toInt())
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
