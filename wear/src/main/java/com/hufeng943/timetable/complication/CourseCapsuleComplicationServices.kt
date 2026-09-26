package com.hufeng943.timetable.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.hufeng943.timetable.R
import com.hufeng943.timetable.data.PreferenceStorage
import com.hufeng943.timetable.presentation.MainActivity
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.model.NextCourseEngine
import com.hufeng943.timetable.shared.model.NextCourseOccurrence
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import javax.inject.Inject

private data class CapsuleCourse(
    val occurrence: NextCourseOccurrence,
    val start: String,
    val end: String,
)

enum class CapsuleMode { CURRENT, NEXT }

@AndroidEntryPoint
abstract class BaseCourseCapsuleService(private val mode: CapsuleMode) : SuspendingComplicationDataSourceService() {
    @Inject lateinit var repository: TimetableRepository
    @Inject lateinit var preferenceStorage: PreferenceStorage

    private fun tapAction() = PendingIntent.getActivity(
        this,
        if (mode == CapsuleMode.CURRENT) 101 else 102,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(
        type = type,
        text = if (mode == CapsuleMode.CURRENT) "高等数学" else "大学英语",
        title = if (mode == CapsuleMode.CURRENT) "上课中" else "10:20",
        detail = "08:00–09:40",
        progress = if (mode == CapsuleMode.CURRENT) 0.55f else null,
    )

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val is24Hour = runCatching {
            preferenceStorage.appConfigFlow.firstOrNull()?.is24HourFormat ?: true
        }.getOrDefault(true)
        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val tables = repository.getAllTimetables().firstOrNull().orEmpty()
        val state = NextCourseEngine.resolve(tables, now, zone)
        val occurrence = when (mode) {
            CapsuleMode.CURRENT -> state.current
            CapsuleMode.NEXT -> state.next
        }

        if (occurrence == null) {
            return build(
                type = request.complicationType,
                text = if (mode == CapsuleMode.CURRENT) getString(R.string.home_no_course_today) else getString(R.string.home_day_finished_title),
                title = getString(R.string.app_name),
                detail = "—",
                progress = null,
            )
        }

        val course = CapsuleCourse(
            occurrence = occurrence,
            start = occurrence.startTime.toDisplayString(is24Hour),
            end = occurrence.endTime.toDisplayString(is24Hour),
        )
        val title = when (mode) {
            CapsuleMode.CURRENT -> getString(
                R.string.course_in_progress,
                state.minutesRemaining?.coerceAtLeast(1) ?: 1,
            )
            CapsuleMode.NEXT -> course.start
        }
        val dynamicTitle: ComplicationText? = if (mode == CapsuleMode.NEXT) {
            TimeDifferenceComplicationText.Builder(
                TimeDifferenceStyle.SHORT_SINGLE_UNIT,
                CountDownTimeReference(
                    java.time.Instant.ofEpochMilli(occurrence.startInstant.toEpochMilliseconds())
                ),
            ).build()
        } else null

        return build(
            type = request.complicationType,
            text = occurrence.courseName.ifBlank { getString(R.string.tile_unnamed_course) },
            title = title,
            detail = "${course.start}–${course.end}",
            progress = if (mode == CapsuleMode.CURRENT) state.currentProgress else null,
            dynamicTitle = dynamicTitle,
        )
    }

    private fun build(
        type: ComplicationType,
        text: String,
        title: String,
        detail: String,
        progress: Float?,
        dynamicTitle: ComplicationText? = null,
    ): ComplicationData? {
        val shortLabel = if (text.length > 6) text.take(5) + "…" else text
        val resolvedTitle = dynamicTitle ?: PlainComplicationText.Builder(title).build()
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(shortLabel).build(),
                PlainComplicationText.Builder("${getString(R.string.app_name)} $text $detail").build(),
            ).setTitle(resolvedTitle)
                .setTapAction(tapAction())
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                PlainComplicationText.Builder("$text  $detail").build(),
                PlainComplicationText.Builder("${getString(R.string.app_name)} $text $detail").build(),
            ).setTitle(resolvedTitle)
                .setTapAction(tapAction())
                .build()

            ComplicationType.RANGED_VALUE -> progress?.let {
                RangedValueComplicationData.Builder(
                    it,
                    0f,
                    1f,
                    PlainComplicationText.Builder("$text ${getString(R.string.complication_current_course_label)}").build(),
                ).setText(PlainComplicationText.Builder(text).build())
                    .setTitle(PlainComplicationText.Builder("${(it * 100).toInt()}%").build())
                    .setTapAction(tapAction())
                    .build()
            }

            else -> null
        }
    }
}

@AndroidEntryPoint
class CurrentCourseComplicationService : BaseCourseCapsuleService(CapsuleMode.CURRENT)

@AndroidEntryPoint
class NextCourseComplicationService : BaseCourseCapsuleService(CapsuleMode.NEXT)
