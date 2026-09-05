package com.hufeng943.timetable.shared.export

import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlin.time.Clock

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

    fun parseIcs(icsContent: String): List<Timetable> {
        val lines = icsContent.lines().map { it.trim() }
        var currentCalName = "导入课表"
        val events = mutableListOf<ParsedEvent>()

        var inEvent = false
        var summary = ""
        var location: String? = null
        var description = ""
        var dtStartStr = ""
        var dtEndStr = ""

        for (line in lines) {
            when {
                line.startsWith("X-WR-CALNAME:", ignoreCase = true) -> {
                    currentCalName = unescapeIcs(line.substringAfter(":")).ifEmpty { "导入课表" }
                }
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> {
                    inEvent = true
                    summary = ""
                    location = null
                    description = ""
                    dtStartStr = ""
                    dtEndStr = ""
                }
                inEvent && line.startsWith("SUMMARY:", ignoreCase = true) -> {
                    summary = unescapeIcs(line.substringAfter(":"))
                }
                inEvent && line.startsWith("LOCATION:", ignoreCase = true) -> {
                    location = unescapeIcs(line.substringAfter(":")).takeIf { it.isNotBlank() }
                }
                inEvent && line.startsWith("DESCRIPTION:", ignoreCase = true) -> {
                    description = unescapeIcs(line.substringAfter(":"))
                }
                inEvent && line.startsWith("DTSTART", ignoreCase = true) -> {
                    dtStartStr = line.substringAfter(":")
                }
                inEvent && line.startsWith("DTEND", ignoreCase = true) -> {
                    dtEndStr = line.substringAfter(":")
                }
                line.equals("END:VEVENT", ignoreCase = true) -> {
                    inEvent = false
                    val startDt = parseDateTime(dtStartStr)
                    val endDt = parseDateTime(dtEndStr)
                    if (summary.isNotEmpty() && startDt != null && endDt != null) {
                        var teacher: String? = null
                        var remark: String? = null
                        var semName: String? = null

                        description.split("\n", "\\n").forEach { part ->
                            val p = part.trim()
                            when {
                                p.startsWith("教师:") -> teacher = p.substringAfter("教师:").trim().takeIf { it.isNotEmpty() }
                                p.startsWith("备注:") -> remark = p.substringAfter("备注:").trim().takeIf { it.isNotEmpty() }
                                p.startsWith("学期:") -> semName = p.substringAfter("学期:").trim().takeIf { it.isNotEmpty() }
                            }
                        }

                        events.add(
                            ParsedEvent(
                                summary = summary,
                                location = location,
                                teacher = teacher,
                                remark = remark,
                                date = startDt.first,
                                startTime = startDt.second,
                                endTime = endDt.second,
                                semesterName = semName ?: currentCalName
                            )
                        )
                    }
                }
            }
        }

        if (events.isEmpty()) return emptyList()

        return events.groupBy { it.semesterName ?: currentCalName }.map { (semesterName, semesterEvents) ->
            val minDate = semesterEvents.minOf { it.date }
            val maxDate = semesterEvents.maxOf { it.date }

            val offsetDays = (minDate.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber).mod(7)
            val semesterStartMonday = LocalDate.fromEpochDays(minDate.toEpochDays() - offsetDays)

            val courseGroups = semesterEvents.groupBy { Triple(it.summary, it.location, it.teacher) }
            val courses = courseGroups.entries.map { (_, cEvents) ->
                val (courseName, cLocation, cTeacher) = Triple(cEvents.first().summary, cEvents.first().location, cEvents.first().teacher)

                val slotGroups = cEvents.groupBy { Triple(it.date.dayOfWeek, it.startTime, it.endTime) }
                val timeSlots = slotGroups.entries.map { (slotInfo, sEvents) ->
                    val (dayOfWeek, sTime, eTime) = slotInfo
                    val weeks = sEvents.map { ev ->
                        val evOffset = (ev.date.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber).mod(7)
                        val evMonday = LocalDate.fromEpochDays(ev.date.toEpochDays() - evOffset)
                        ((evMonday.toEpochDays() - semesterStartMonday.toEpochDays()) / 7).toInt() + 1
                    }.toSet()

                    val allOdd = weeks.all { it % 2 != 0 }
                    val allEven = weeks.all { it % 2 == 0 }
                    val recurrence = when {
                        allOdd && !allEven && weeks.size > 1 -> WeekPattern.ODD_WEEK
                        allEven && !allOdd && weeks.size > 1 -> WeekPattern.EVEN_WEEK
                        else -> WeekPattern.EVERY_WEEK
                    }

                    TimeSlot(
                        id = 0,
                        dayOfWeek = dayOfWeek,
                        startTime = sTime,
                        endTime = eTime,
                        recurrence = recurrence,
                        remark = sEvents.firstNotNullOfOrNull { it.remark }
                    )
                }

                Course(
                    id = 0,
                    name = courseName,
                    teacher = cTeacher,
                    location = cLocation,
                    timeSlots = timeSlots
                )
            }

            Timetable(
                timetableId = 0,
                semesterName = semesterName,
                createdAt = Clock.System.now(),
                semesterStart = minDate,
                semesterEnd = maxDate,
                allCourses = courses,
                color = -1L
            )
        }
    }

    private fun parseDateTime(raw: String): Pair<LocalDate, LocalTime>? {
        val clean = raw.substringAfterLast(":")
        if (clean.length < 15 || !clean.contains("T")) return null
        return try {
            val datePart = clean.substringBefore("T")
            val timePart = clean.substringAfter("T").take(6)
            val y = datePart.substring(0, 4).toInt()
            val m = datePart.substring(4, 6).toInt()
            val d = datePart.substring(6, 8).toInt()
            val hh = timePart.substring(0, 2).toInt()
            val mm = timePart.substring(2, 4).toInt()
            val ss = timePart.substring(4, 6).toInt()
            Pair(LocalDate(y, m, d), LocalTime(hh, mm, ss))
        } catch (_: Exception) {
            null
        }
    }

    private fun unescapeIcs(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }
}
