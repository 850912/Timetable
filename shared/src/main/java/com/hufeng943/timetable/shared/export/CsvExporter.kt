package com.hufeng943.timetable.shared.export

import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import kotlinx.datetime.DayOfWeek
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object CsvExporter {

    fun streamCsv(outputStream: OutputStream, timetables: List<Timetable>) {
        BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { writer ->
            writer.write("\uFEFF")
            writer.write("学期,课程名称,教师,上课地点,星期,开始时间,结束时间,单双周,备注\r\n")

            for (timetable in timetables) {
                for (course in timetable.allCourses) {
                    for (slot in course.timeSlots) {
                        val dayStr = when (slot.dayOfWeek) {
                            DayOfWeek.MONDAY -> "星期一"
                            DayOfWeek.TUESDAY -> "星期二"
                            DayOfWeek.WEDNESDAY -> "星期三"
                            DayOfWeek.THURSDAY -> "星期四"
                            DayOfWeek.FRIDAY -> "星期五"
                            DayOfWeek.SATURDAY -> "星期六"
                            DayOfWeek.SUNDAY -> "星期日"
                            null -> "未知"
                        }
                        val startTimeStr = slot.startTime?.let { "%02d:%02d".format(it.hour, it.minute) } ?: ""
                        val endTimeStr = slot.endTime?.let { "%02d:%02d".format(it.hour, it.minute) } ?: ""
                        val recurrenceStr = when (slot.recurrence) {
                            WeekPattern.EVERY_WEEK -> "每周"
                            WeekPattern.ODD_WEEK -> "单周"
                            WeekPattern.EVEN_WEEK -> "双周"
                        }

                        writer.write(escapeCsv(timetable.semesterName) + ",")
                        writer.write(escapeCsv(course.name) + ",")
                        writer.write(escapeCsv(course.teacher ?: "") + ",")
                        writer.write(escapeCsv(course.location ?: "") + ",")
                        writer.write(escapeCsv(dayStr) + ",")
                        writer.write(escapeCsv(startTimeStr) + ",")
                        writer.write(escapeCsv(endTimeStr) + ",")
                        writer.write(escapeCsv(recurrenceStr) + ",")
                        writer.write(escapeCsv(slot.remark ?: "") + "\r\n")
                    }
                }
            }
            writer.flush()
        }
    }

    private fun escapeCsv(field: String): String {
        val escaped = field.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
