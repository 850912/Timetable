package com.hufeng943.timetable.shared.export

import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import com.hufeng943.timetable.shared.model.resolveDate
import com.hufeng943.timetable.shared.model.weekNumberFor
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class ExportPreviewStats(
    val semesterName: String,
    val totalCourses: Int,
    val totalInstances: Int,
    val hasRecurrenceRules: Boolean
)

object IcsExporter {

    private inline fun forEachCourseOccurrence(
        timetables: List<Timetable>,
        action: (timetable: Timetable, courseName: String, location: String?, teacher: String?, remark: String?, date: LocalDate, weekIndex: Int, slotId: Long, courseId: Long, startTimeStr: String, endTimeStr: String) -> Unit
    ) {
        for (timetable in timetables) {
            val startDate = timetable.semesterStart
            val endDate = timetable.semesterEnd ?: LocalDate.fromEpochDays(startDate.toEpochDays() + 140)
            var date = startDate
            while (date <= endDate) {
                for (occurrence in timetable.resolveDate(date)) {
                    fun timeString(time: kotlinx.datetime.LocalTime): String =
                        "%02d%02d%02d".format(time.hour, time.minute, time.second)
                    action(
                        timetable,
                        occurrence.course.name,
                        occurrence.location,
                        occurrence.course.teacher,
                        occurrence.override?.remark ?: occurrence.timeSlot.remark,
                        date,
                        timetable.weekNumberFor(date) ?: 0,
                        occurrence.timeSlot.id,
                        occurrence.course.id,
                        timeString(occurrence.startTime),
                        timeString(occurrence.endTime),
                    )
                }
                date = LocalDate.fromEpochDays(date.toEpochDays() + 1)
            }
        }
    }

    fun calculateStats(timetables: List<Timetable>): ExportPreviewStats {
        var totalCourses = 0
        var totalInstances = 0
        var hasRecurrence = false
        val names = timetables.joinToString(", ") { it.semesterName.ifEmpty { "课表" } }

        for (timetable in timetables) {
            totalCourses += timetable.allCourses.size
            for (c in timetable.allCourses) {
                if (c.timeSlots.any { it.recurrence != WeekPattern.EVERY_WEEK }) {
                    hasRecurrence = true
                }
            }
        }

        forEachCourseOccurrence(timetables) { _, _, _, _, _, _, _, _, _, _, _ ->
            totalInstances++
        }

        return ExportPreviewStats(
            semesterName = names.ifEmpty { "当前学期" },
            totalCourses = totalCourses,
            totalInstances = totalInstances,
            hasRecurrenceRules = hasRecurrence
        )
    }

    fun streamIcs(outputStream: OutputStream, timetables: List<Timetable>) {
        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
            writer.write("BEGIN:VCALENDAR\r\n")
            writer.write("VERSION:2.0\r\n")
            writer.write("PRODID:-//HuFeng943//Timetable//CN\r\n")
            writer.write("CALSCALE:GREGORIAN\r\n")
            writer.write("METHOD:PUBLISH\r\n")
            writer.write("X-WR-TIMEZONE:${java.time.ZoneId.systemDefault().id}\r\n")

            forEachCourseOccurrence(timetables) { timetable, courseName, location, teacher, remark, targetDate, weekIndex, slotId, courseId, startTimeStr, endTimeStr ->
                val y = targetDate.year.toString().padStart(4, '0')
                val m = targetDate.month.number.toString().padStart(2, '0')
                val d = targetDate.day.toString().padStart(2, '0')
                val dtStart = "${y}${m}${d}T${startTimeStr}"
                val dtEnd = "${y}${m}${d}T${endTimeStr}"

                val stableKey = "${timetable.timetableId}_${courseId}_${slotId}_${targetDate.toEpochDays()}"
                val md5 = MessageDigest.getInstance("MD5").digest(stableKey.toByteArray(StandardCharsets.UTF_8))
                val uid = md5.joinToString("") { "%02x".format(it) }

                writer.write("BEGIN:VEVENT\r\n")
                writer.write("UID:${uid}@timetable\r\n")
                writer.write("SUMMARY:${escapeIcs(courseName)}\r\n")
                location?.takeIf { it.isNotEmpty() }?.let {
                    writer.write("LOCATION:${escapeIcs(it)}\r\n")
                }
                val descList = mutableListOf<String>()
                if (timetable.semesterName.isNotEmpty()) descList.add("学期: ${timetable.semesterName}")
                teacher?.takeIf { it.isNotEmpty() }?.let { descList.add("教师: $it") }
                remark?.takeIf { it.isNotEmpty() }?.let { descList.add("备注: $it") }
                descList.add("第 ${weekIndex} 周")

                writer.write("DESCRIPTION:${escapeIcs(descList.joinToString("\n"))}\r\n")
                writer.write("DTSTART;TZID=${java.time.ZoneId.systemDefault().id}:${dtStart}\r\n")
                writer.write("DTEND;TZID=${java.time.ZoneId.systemDefault().id}:${dtEnd}\r\n")
                writer.write("STATUS:CONFIRMED\r\n")
                writer.write("END:VEVENT\r\n")
            }

            writer.write("END:VCALENDAR\r\n")
            writer.flush()
        }
    }

    private fun escapeIcs(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n")
    }
}
