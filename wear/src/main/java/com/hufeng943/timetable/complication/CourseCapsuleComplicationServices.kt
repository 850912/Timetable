package com.hufeng943.timetable.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.hufeng943.timetable.presentation.MainActivity
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.WeekPattern
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import javax.inject.Inject

private data class CapsuleCourse(val name: String, val start: String, val end: String, val startMin: Int, val endMin: Int)
enum class CapsuleMode { CURRENT, NEXT }

private suspend fun TimetableRepository.capsuleCourse(mode: CapsuleMode): CapsuleCourse? {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    val nowMin = now.hour * 60 + now.minute
    val tables = getAllTimetables().firstOrNull().orEmpty()
    val courses = tables.flatMap { table ->
        val end = table.semesterEnd
        if (today < table.semesterStart || (end != null && today > end)) return@flatMap emptyList()
        val offset = (table.semesterStart.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber).mod(7)
        val semesterMonday = table.semesterStart.minus(offset.toLong(), DateTimeUnit.DAY)
        val todayOffset = (today.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber).mod(7)
        val todayMonday = today.minus(todayOffset.toLong(), DateTimeUnit.DAY)
        val week = ((todayMonday.toEpochDays() - semesterMonday.toEpochDays()) / 7 + 1).toInt()
        if (week <= 0) return@flatMap emptyList()
        table.allCourses.flatMap { course ->
            course.timeSlots.filter { slot ->
                slot.dayOfWeek == today.dayOfWeek && slot.matchesWeek(week) && slot.startTime != null && slot.endTime != null
            }.mapNotNull { slot ->
                val start = slot.startTime ?: return@mapNotNull null
                val finish = slot.endTime ?: return@mapNotNull null
                CapsuleCourse(
                    course.name.ifBlank { "课程" },
                    "%02d:%02d".format(start.hour, start.minute),
                    "%02d:%02d".format(finish.hour, finish.minute),
                    start.hour * 60 + start.minute,
                    finish.hour * 60 + finish.minute
                )
            }
        }
    }.sortedBy { it.startMin }

    fun active(c: CapsuleCourse) = if (c.startMin <= c.endMin) nowMin in c.startMin until c.endMin else nowMin >= c.startMin || nowMin < c.endMin
    return when (mode) {
        CapsuleMode.CURRENT -> courses.firstOrNull(::active)
        CapsuleMode.NEXT -> courses.firstOrNull { !active(it) && it.startMin > nowMin }
    }
}

private fun TimeSlot.matchesWeek(week: Int): Boolean = when (recurrence) {
    WeekPattern.EVERY_WEEK -> true
    WeekPattern.ODD_WEEK -> week % 2 == 1
    WeekPattern.EVEN_WEEK -> week % 2 == 0
}

@AndroidEntryPoint
abstract class BaseCourseCapsuleService(private val mode: CapsuleMode) : SuspendingComplicationDataSourceService() {
    @Inject lateinit var repository: TimetableRepository

    private fun tapAction() = PendingIntent.getActivity(
        this, if (mode == CapsuleMode.CURRENT) 101 else 102,
        Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(
        type,
        if (mode == CapsuleMode.CURRENT) "高等数学" else "大学英语",
        if (mode == CapsuleMode.CURRENT) "上课中" else "10:20",
        "08:00–09:40"
    )

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val course = repository.capsuleCourse(mode)
        return if (course == null) {
            build(request.complicationType, if (mode == CapsuleMode.CURRENT) "当前无课" else "后续无课", "课程表", "—")
        } else {
            build(request.complicationType, course.name, if (mode == CapsuleMode.CURRENT) "上课中" else course.start, "${course.start}–${course.end}")
        }
    }

    private fun build(type: ComplicationType, text: String, title: String, detail: String): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(text).build(), PlainComplicationText.Builder("课程表 $text $detail").build()
        ).setTitle(PlainComplicationText.Builder(title).build()).setTapAction(tapAction()).build()
        ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
            PlainComplicationText.Builder("$text  $detail").build(), PlainComplicationText.Builder("课程表 $text $detail").build()
        ).setTitle(PlainComplicationText.Builder(title).build()).setTapAction(tapAction()).build()
        else -> null
    }
}

@AndroidEntryPoint
class CurrentCourseComplicationService : BaseCourseCapsuleService(CapsuleMode.CURRENT)

@AndroidEntryPoint
class NextCourseComplicationService : BaseCourseCapsuleService(CapsuleMode.NEXT)
