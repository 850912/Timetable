package com.hufeng943.timetable.shared.importexport

import com.hufeng943.timetable.shared.model.Timetable
import kotlinx.datetime.LocalDate

enum class ImportDisposition { NEW, UNCHANGED, CHANGED }

data class ImportPreviewItem(
    val timetable: Timetable,
    val disposition: ImportDisposition,
    val courseCount: Int,
    val eventCount: Int,
)

data class ImportPreview(val items: List<ImportPreviewItem>) {
    val newCount: Int get() = items.count { it.disposition == ImportDisposition.NEW }
    val unchangedCount: Int get() = items.count { it.disposition == ImportDisposition.UNCHANGED }
    val changedCount: Int get() = items.count { it.disposition == ImportDisposition.CHANGED }

    fun selected(replaceExisting: Boolean): List<Timetable> = items
        .filter { it.disposition == ImportDisposition.NEW || (replaceExisting && it.disposition == ImportDisposition.CHANGED) }
        .map { it.timetable }
}

object ImportPreviewBuilder {
    fun build(incoming: List<Timetable>, existing: List<Timetable>): ImportPreview {
        val seen = mutableSetOf<Pair<String, LocalDate>>()
        val items = incoming.map { table ->
            val identity = table.semesterName.trim() to table.semesterStart
            require(seen.add(identity)) { "导入文件包含重复课表：${table.semesterName}" }
            val match = existing.firstOrNull { current ->
                (table.syncId != null && table.syncId == current.syncId) ||
                    (table.semesterName.trim() == current.semesterName.trim() && table.semesterStart == current.semesterStart)
            }
            val disposition = when {
                match == null -> ImportDisposition.NEW
                contentKey(table) == contentKey(match) -> ImportDisposition.UNCHANGED
                else -> ImportDisposition.CHANGED
            }
            ImportPreviewItem(table, disposition, table.allCourses.size, table.events.size)
        }
        return ImportPreview(items)
    }

    private fun contentKey(table: Timetable): Any = listOf(
        table.semesterName, table.semesterStart, table.semesterEnd, table.color,
        table.allCourses.map { course ->
            listOf(course.name, course.teacher, course.location, course.color, course.timeSlots.map { slot ->
                listOf(slot.dayOfWeek, slot.startTime, slot.endTime, slot.recurrence, slot.remark, slot.overrides, slot.batchGroupId)
            })
        },
        table.events.map { event ->
            listOf(event.title, event.type, event.date, event.time, event.courseName, event.location,
                event.note, event.reminderMinutesBefore, event.completed)
        },
    )
}
