package com.hufeng943.timetable.shared.export

import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.ScheduleBatchOperations
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlin.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object IcsImporter {
    private data class ParsedEvent(
        val summary: String,
        val location: String?,
        val teacher: String?,
        val remark: String?,
        val date: LocalDate,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val semesterName: String?
    )

    private data class IcsProperty(
        val name: String,
        val params: Map<String, String>,
        val value: String,
    )

    fun parseIcs(icsContent: String): List<Timetable> {
        val lines = unfoldLines(icsContent)
        var currentCalName = "导入课表"
        val events = mutableListOf<ParsedEvent>()
        val errors = mutableListOf<String>()

        var inEvent = false
        var eventIndex = 0
        var summary = ""
        var location: String? = null
        var description = ""
        var dtStart: IcsProperty? = null
        var dtEnd: IcsProperty? = null

        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            val property = parseProperty(line)
            when {
                property?.name == "X-WR-CALNAME" && !inEvent -> {
                    currentCalName = unescapeIcs(property.value).ifEmpty { "导入课表" }
                }
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> {
                    inEvent = true
                    eventIndex++
                    summary = ""
                    location = null
                    description = ""
                    dtStart = null
                    dtEnd = null
                }
                inEvent && property?.name == "SUMMARY" -> summary = unescapeIcs(property.value)
                inEvent && property?.name == "LOCATION" -> location = unescapeIcs(property.value).takeIf { it.isNotBlank() }
                inEvent && property?.name == "DESCRIPTION" -> description = unescapeIcs(property.value)
                inEvent && property?.name == "DTSTART" -> dtStart = property
                inEvent && property?.name == "DTEND" -> dtEnd = property
                line.equals("END:VEVENT", ignoreCase = true) -> {
                    inEvent = false
                    runCatching {
                        require(summary.isNotBlank()) { "SUMMARY 为空" }
                        val start = parseDateTime(requireNotNull(dtStart) { "缺少 DTSTART" })
                        val end = parseDateTime(requireNotNull(dtEnd) { "缺少 DTEND" })
                        require(end.first > start.first || end.second > start.second) { "DTEND 必须晚于 DTSTART" }
                        require(end.first == start.first) { "暂不支持跨午夜课程" }

                        var teacher: String? = null
                        var remark: String? = null
                        var semName: String? = null
                        description.lines().forEach { part ->
                            val p = part.trim()
                            when {
                                p.startsWith("教师:") -> teacher = p.substringAfter("教师:").trim().takeIf { it.isNotEmpty() }
                                p.startsWith("备注:") -> remark = p.substringAfter("备注:").trim().takeIf { it.isNotEmpty() }
                                p.startsWith("学期:") -> semName = p.substringAfter("学期:").trim().takeIf { it.isNotEmpty() }
                            }
                        }
                        events += ParsedEvent(
                            summary = summary,
                            location = location,
                            teacher = teacher,
                            remark = remark,
                            date = start.first,
                            startTime = start.second,
                            endTime = end.second,
                            semesterName = semName ?: currentCalName,
                        )
                    }.onFailure { errors += "VEVENT #$eventIndex：${it.message ?: "解析失败"}" }
                }
            }
        }

        require(errors.isEmpty()) { "ICS 导入失败：\n${errors.joinToString("\n")}" }
        if (events.isEmpty()) return emptyList()

        return events.groupBy { it.semesterName ?: currentCalName }.map { (semesterName, semesterEvents) ->
            val minDate = semesterEvents.minOf { it.date }
            val maxDate = semesterEvents.maxOf { it.date }
            val semesterStartMonday = mondayOf(minDate)

            val courses = semesterEvents.groupBy { Triple(it.summary, it.location, it.teacher) }.values.map { cEvents ->
                val firstEvent = cEvents.first()
                val timeSlots = cEvents.groupBy { Triple(it.date.dayOfWeek, it.startTime, it.endTime) }.map { (slotInfo, sEvents) ->
                    val (dayOfWeek, startTime, endTime) = slotInfo
                    val uniqueEvents = sEvents.distinctBy { it.date }.sortedBy { it.date }
                    val weeks = uniqueEvents.map { weekNumber(semesterStartMonday, it.date) }.toSet()
                    val recurrence = inferRecurrence(weeks)

                    if (recurrence == WeekPattern.DATE_ONLY) {
                        ScheduleBatchOperations.dateOnlySlot(
                            dates = uniqueEvents.map { it.date },
                            startTime = startTime,
                            endTime = endTime,
                            remark = uniqueEvents.firstNotNullOfOrNull { it.remark },
                            location = firstEvent.location,
                        )
                    } else {
                        TimeSlot(
                            id = 0,
                            dayOfWeek = dayOfWeek,
                            startTime = startTime,
                            endTime = endTime,
                            recurrence = recurrence,
                            remark = uniqueEvents.firstNotNullOfOrNull { it.remark },
                        )
                    }
                }

                Course(
                    id = 0,
                    name = firstEvent.summary,
                    teacher = firstEvent.teacher,
                    location = firstEvent.location,
                    timeSlots = timeSlots,
                )
            }

            Timetable(
                timetableId = 0,
                semesterName = semesterName,
                createdAt = Clock.System.now(),
                semesterStart = semesterStartMonday,
                semesterEnd = maxDate,
                allCourses = courses,
                color = -1L,
            )
        }
    }

    /** RFC 5545 content-line unfolding: CRLF + single SP/HTAB continues the previous line. */
    private fun unfoldLines(content: String): List<String> {
        val physical = content.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val logical = mutableListOf<String>()
        for (line in physical) {
            if ((line.startsWith(' ') || line.startsWith('\t')) && logical.isNotEmpty()) {
                logical[logical.lastIndex] += line.drop(1)
            } else {
                logical += line
            }
        }
        return logical
    }

    private fun parseProperty(line: String): IcsProperty? {
        val colon = line.indexOf(':')
        if (colon <= 0) return null
        val left = line.substring(0, colon)
        val value = line.substring(colon + 1)
        val parts = left.split(';')
        val name = parts.first().uppercase()
        val params = parts.drop(1).mapNotNull { parameter ->
            val eq = parameter.indexOf('=')
            if (eq <= 0) null else parameter.substring(0, eq).uppercase() to parameter.substring(eq + 1).trim('"')
        }.toMap()
        return IcsProperty(name, params, value)
    }

    /**
     * RFC 5545: a trailing Z is UTC; TZID identifies local time in that zone; a value with
     * neither is floating time. Fixed instants are converted to the device zone so the imported
     * course shows at the correct local clock time.
     */
    private fun parseDateTime(property: IcsProperty): Pair<LocalDate, LocalTime> {
        require(property.params["VALUE"]?.uppercase() != "DATE") { "全天 DATE 事件不能作为课时导入" }
        val value = property.value.trim()
        require(value.contains('T')) { "日期时间格式错误：$value" }
        val base = value.removeSuffix("Z")
        val formatter = when (base.length) {
            13 -> DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")
            15 -> DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
            else -> throw IllegalArgumentException("日期时间格式错误：$value")
        }
        val local = LocalDateTime.parse(base, formatter)
        val deviceZone = ZoneId.systemDefault()
        val converted = when {
            value.endsWith("Z", ignoreCase = true) -> local.toInstant(ZoneOffset.UTC).atZone(deviceZone).toLocalDateTime()
            property.params["TZID"] != null -> {
                val sourceZone = runCatching { ZoneId.of(property.params.getValue("TZID")) }
                    .getOrElse { throw IllegalArgumentException("未知 TZID：${property.params["TZID"]}") }
                local.atZone(sourceZone).withZoneSameInstant(deviceZone).toLocalDateTime()
            }
            else -> local // floating local time, interpreted in the current device zone
        }
        return LocalDate(converted.year, converted.monthValue, converted.dayOfMonth) to
            LocalTime(converted.hour, converted.minute, converted.second)
    }

    private fun mondayOf(date: LocalDate): LocalDate {
        val offset = (date.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber).mod(7)
        return LocalDate.fromEpochDays(date.toEpochDays() - offset)
    }

    private fun weekNumber(semesterMonday: LocalDate, date: LocalDate): Int {
        val eventMonday = mondayOf(date)
        return ((eventMonday.toEpochDays() - semesterMonday.toEpochDays()) / 7).toInt() + 1
    }

    private fun inferRecurrence(weeks: Set<Int>): WeekPattern {
        if (weeks.size <= 1) return WeekPattern.DATE_ONLY
        val sorted = weeks.sorted()
        val everyWeek = sorted.zipWithNext().all { (a, b) -> b - a == 1 }
        if (everyWeek) return WeekPattern.EVERY_WEEK
        val allOdd = sorted.all { it % 2 == 1 }
        val allEven = sorted.all { it % 2 == 0 }
        val everyOtherWeek = sorted.zipWithNext().all { (a, b) -> b - a == 2 }
        return when {
            allOdd && everyOtherWeek -> WeekPattern.ODD_WEEK
            allEven && everyOtherWeek -> WeekPattern.EVEN_WEEK
            else -> WeekPattern.DATE_ONLY
        }
    }

    private fun unescapeIcs(value: String): String = value
        .replace("\\n", "\n", ignoreCase = true)
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")
}
