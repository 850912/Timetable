package com.hufeng943.timetable.shared.export

import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.ScheduleOverride
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import kotlin.time.Clock

object CsvImporter {
    private val json = Json { ignoreUnknownKeys = true }

    private data class CsvRow(
        val semesterName: String,
        val courseName: String,
        val teacher: String?,
        val location: String?,
        val dayOfWeek: DayOfWeek,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val recurrence: WeekPattern,
        val remark: String?,
        val semesterStart: LocalDate?,
        val semesterEnd: LocalDate?,
        val overrides: List<ScheduleOverride>,
        val hasSemesterMetadata: Boolean,
    )

    /**
     * Strict CSV import. Invalid rows are rejected with all line errors collected instead of
     * silently substituting Monday/08:00/09:40 and creating believable-but-wrong lessons.
     */
    fun parseCsv(csvContent: String): List<Timetable> {
        val cleanContent = csvContent.removePrefix("\uFEFF").trim()
        val lines = cleanContent.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val rows = mutableListOf<CsvRow>()
        val errors = mutableListOf<String>()
        for (i in 1 until lines.size) {
            val lineNo = i + 1
            val tokens = parseCsvLine(lines[i])
            if (tokens.size < 7) {
                errors += "第${lineNo}行：字段不足，至少需要 7 列"
                continue
            }

            runCatching {
                val semName = tokens.getOrNull(0)?.trim()?.ifEmpty { "导入课表" } ?: "导入课表"
                val courseName = tokens.getOrNull(1)?.trim().orEmpty()
                require(courseName.isNotEmpty()) { "课程名为空" }

                val teacher = tokens.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
                val location = tokens.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() }
                val dayStr = tokens.getOrNull(4)?.trim().orEmpty()
                val startStr = tokens.getOrNull(5)?.trim().orEmpty()
                val endStr = tokens.getOrNull(6)?.trim().orEmpty()
                val recStr = tokens.getOrNull(7)?.trim().orEmpty()
                val remark = tokens.getOrNull(8)?.trim()?.takeIf { it.isNotEmpty() }
                val hasSemesterMetadata = tokens.size >= 11

                val semesterStart = parseOptionalDate(tokens.getOrNull(9), "学期开始日期")
                val semesterEnd = parseOptionalDate(tokens.getOrNull(10), "学期结束日期")
                require(semesterEnd == null || semesterStart == null || semesterEnd >= semesterStart) {
                    "学期结束日期早于开始日期"
                }

                val overrides = tokens.getOrNull(11)?.trim()?.takeIf { it.isNotEmpty() }?.let { encoded ->
                    runCatching { json.decodeFromString<List<ScheduleOverride>>(encoded) }
                        .getOrElse { throw IllegalArgumentException("日期例外 JSON 无法解析") }
                }.orEmpty()

                val dayOfWeek = parseDayOfWeek(dayStr)
                    ?: throw IllegalArgumentException("无法识别星期：$dayStr")
                val startTime = parseTime(startStr)
                    ?: throw IllegalArgumentException("无法识别开始时间：$startStr")
                val endTime = parseTime(endStr)
                    ?: throw IllegalArgumentException("无法识别结束时间：$endStr")
                require(endTime > startTime) { "结束时间必须晚于开始时间（不支持跨午夜课程）" }

                val recurrence = when (recStr.lowercase()) {
                    "", "每周", "every_week", "weekly" -> WeekPattern.EVERY_WEEK
                    "单周", "odd_week", "odd" -> WeekPattern.ODD_WEEK
                    "双周", "even_week", "even" -> WeekPattern.EVEN_WEEK
                    "仅指定日期", "指定日期", "date_only" -> WeekPattern.DATE_ONLY
                    else -> throw IllegalArgumentException("无法识别重复规则：$recStr")
                }

                CsvRow(
                    semesterName = semName,
                    courseName = courseName,
                    teacher = teacher,
                    location = location,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    recurrence = recurrence,
                    remark = remark,
                    semesterStart = semesterStart,
                    semesterEnd = semesterEnd,
                    overrides = overrides,
                    hasSemesterMetadata = hasSemesterMetadata,
                )
            }.onSuccess(rows::add).onFailure { error ->
                errors += "第${lineNo}行：${error.message ?: "格式错误"}"
            }
        }

        require(errors.isEmpty()) { "CSV 导入失败：\n${errors.joinToString("\n")}" }

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return rows.groupBy { it.semesterName to Triple(it.semesterStart, it.semesterEnd, it.hasSemesterMetadata) }
            .map { (semesterInfo, sRows) ->
                val semesterName = semesterInfo.first
                val (importedStart, importedEnd, hasSemesterMetadata) = semesterInfo.second
                val courses = sRows.groupBy { Triple(it.courseName, it.teacher, it.location) }.map { (info, cRows) ->
                    val (cName, cTeacher, cLocation) = info
                    Course(
                        id = 0,
                        name = cName,
                        teacher = cTeacher,
                        location = cLocation,
                        timeSlots = cRows.map { r ->
                            TimeSlot(
                                id = 0,
                                dayOfWeek = r.dayOfWeek,
                                startTime = r.startTime,
                                endTime = r.endTime,
                                recurrence = r.recurrence,
                                remark = r.remark,
                                overrides = r.overrides,
                            )
                        }
                    )
                }

                Timetable(
                    timetableId = 0,
                    semesterName = semesterName,
                    createdAt = Clock.System.now(),
                    semesterStart = importedStart ?: today,
                    semesterEnd = if (hasSemesterMetadata) importedEnd else {
                        importedStart?.let { LocalDate.fromEpochDays(it.toEpochDays() + 140) }
                            ?: LocalDate.fromEpochDays(today.toEpochDays() + 140)
                    },
                    allCourses = courses,
                    color = -1L
                )
            }
    }

    private fun parseOptionalDate(raw: String?, label: String): LocalDate? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { LocalDate.parse(value) }
            .getOrElse { throw IllegalArgumentException("$label 无法解析：$value") }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"'); i++
                    } else inQuotes = !inQuotes
                }
                c == ',' && !inQuotes -> { result.add(sb.toString()); sb.setLength(0) }
                else -> sb.append(c)
            }
            i++
        }
        require(!inQuotes) { "CSV 引号未闭合" }
        result.add(sb.toString())
        return result
    }

    private fun parseDayOfWeek(str: String): DayOfWeek? = when (str.trim().lowercase()) {
        "周一", "星期一", "一", "1", "mon", "monday" -> DayOfWeek.MONDAY
        "周二", "星期二", "二", "2", "tue", "tuesday" -> DayOfWeek.TUESDAY
        "周三", "星期三", "三", "3", "wed", "wednesday" -> DayOfWeek.WEDNESDAY
        "周四", "星期四", "四", "4", "thu", "thursday" -> DayOfWeek.THURSDAY
        "周五", "星期五", "五", "5", "fri", "friday" -> DayOfWeek.FRIDAY
        "周六", "星期六", "六", "6", "sat", "saturday" -> DayOfWeek.SATURDAY
        "周日", "周天", "星期日", "星期天", "日", "天", "7", "sun", "sunday" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun parseTime(str: String): LocalTime? = runCatching {
        val parts = str.trim().split(":")
        require(parts.size in 2..3)
        LocalTime(parts[0].toInt(), parts[1].toInt(), parts.getOrNull(2)?.toInt() ?: 0)
    }.getOrNull()
}
