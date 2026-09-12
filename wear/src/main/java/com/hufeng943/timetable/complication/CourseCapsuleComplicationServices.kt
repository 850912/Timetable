package com.hufeng943.timetable.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.hufeng943.timetable.R
import com.hufeng943.timetable.data.PreferenceStorage
import com.hufeng943.timetable.presentation.MainActivity
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.model.resolveDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import javax.inject.Inject

private data class CapsuleCourse(val name: String, val start: String, val end: String, val startMin: Int, val endMin: Int)
enum class CapsuleMode { CURRENT, NEXT }

private suspend fun TimetableRepository.capsuleCourse(mode: CapsuleMode, is24Hour: Boolean): CapsuleCourse? {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    val nowMin = now.hour * 60 + now.minute
    val tables = getAllTimetables().firstOrNull().orEmpty()
    val courses = tables.flatMap { table ->
        table.resolveDate(today).map { occurrence ->
            val start = occurrence.startTime
            val finish = occurrence.endTime
            CapsuleCourse(
                occurrence.course.name.ifBlank { "课程" },
                start.toDisplayString(is24Hour),
                finish.toDisplayString(is24Hour),
                start.hour * 60 + start.minute,
                finish.hour * 60 + finish.minute
            )
        }
    }.sortedBy { it.startMin }

    fun active(c: CapsuleCourse) = if (c.startMin <= c.endMin) nowMin in c.startMin until c.endMin else nowMin >= c.startMin || nowMin < c.endMin
    return when (mode) {
        CapsuleMode.CURRENT -> courses.firstOrNull(::active)
        CapsuleMode.NEXT -> courses.firstOrNull { !active(it) && it.startMin > nowMin }
    }
}


@AndroidEntryPoint
abstract class BaseCourseCapsuleService(private val mode: CapsuleMode) : SuspendingComplicationDataSourceService() {
    @Inject lateinit var repository: TimetableRepository
    @Inject lateinit var preferenceStorage: PreferenceStorage

    private fun tapAction() = PendingIntent.getActivity(
        this, if (mode == CapsuleMode.CURRENT) 101 else 102,
        Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(
        type,
        if (mode == CapsuleMode.CURRENT) "高等数学" else "大学英语",
        if (mode == CapsuleMode.CURRENT) "上课中" else "10:20",
        "08:00–09:40",
        if (mode == CapsuleMode.CURRENT) 0.55f else null
    )

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val is24Hour = runCatching { preferenceStorage.appConfigFlow.firstOrNull()?.is24HourFormat ?: true }.getOrDefault(true)
        val course = repository.capsuleCourse(mode, is24Hour)
        return if (course == null) {
            build(request.complicationType, if (mode == CapsuleMode.CURRENT) "当前无课" else "后续无课", "课程表", "—", null)
        } else {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
            val nowMin = now.hour * 60 + now.minute
            val title = if (mode == CapsuleMode.CURRENT) {
                val left = if (course.endMin >= nowMin) course.endMin - nowMin else 24 * 60 - nowMin + course.endMin
                getString(R.string.course_in_progress, left.coerceAtLeast(1))
            } else course.start
            val progress = if (mode == CapsuleMode.CURRENT) {
                val duration = if (course.endMin >= course.startMin) course.endMin - course.startMin else 24 * 60 - course.startMin + course.endMin
                val elapsed = if (nowMin >= course.startMin) nowMin - course.startMin else 24 * 60 - course.startMin + nowMin
                if (duration > 0) (elapsed.toFloat() / duration).coerceIn(0f, 1f) else 0f
            } else null
            build(request.complicationType, course.name, title, "${course.start}–${course.end}", progress)
        }
    }

    private fun build(type: ComplicationType, text: String, title: String, detail: String, progress: Float?): ComplicationData? = when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(text).build(), PlainComplicationText.Builder("课程表 $text $detail").build()
        ).setTitle(PlainComplicationText.Builder(title).build()).setTapAction(tapAction()).build()
        ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
            PlainComplicationText.Builder("$text  $detail").build(), PlainComplicationText.Builder("课程表 $text $detail").build()
        ).setTitle(PlainComplicationText.Builder(title).build()).setTapAction(tapAction()).build()
        ComplicationType.RANGED_VALUE -> progress?.let {
            RangedValueComplicationData.Builder(
                it,
                0f,
                1f,
                PlainComplicationText.Builder("$text 课程进度").build(),
            ).setText(PlainComplicationText.Builder(text).build())
                .setTitle(PlainComplicationText.Builder("${(it * 100).toInt()}%").build())
                .setTapAction(tapAction())
                .build()
        }
        else -> null
    }
}

@AndroidEntryPoint
class CurrentCourseComplicationService : BaseCourseCapsuleService(CapsuleMode.CURRENT)

@AndroidEntryPoint
class NextCourseComplicationService : BaseCourseCapsuleService(CapsuleMode.NEXT)
