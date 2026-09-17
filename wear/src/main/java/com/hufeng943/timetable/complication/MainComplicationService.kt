package com.hufeng943.timetable.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingTimelineComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import com.hufeng943.timetable.data.PreferenceStorage
import com.hufeng943.timetable.presentation.MainActivity
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.resolveDate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import javax.inject.Inject
import java.time.Instant as JavaInstant

@AndroidEntryPoint
class MainComplicationService : SuspendingTimelineComplicationDataSourceService() {

    @Inject
    lateinit var repository: TimetableRepository

    @Inject
    lateinit var preferenceStorage: PreferenceStorage

    private data class CourseInterval(
        val course: Course,
        val slot: TimeSlot,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val startInstant: Instant,
        val endInstant: Instant
    )

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val tapIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("高数").build(),
                contentDescription = PlainComplicationText.Builder("课程表").build()
            ).setTitle(PlainComplicationText.Builder("08:00").build())
                .setTapAction(tapIntent).build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("高等数学 08:00-09:40").build(),
                contentDescription = PlainComplicationText.Builder("课程表").build()
            ).setTitle(PlainComplicationText.Builder("上课中").build())
                .setTapAction(tapIntent).build()

            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
        val tapIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val allTimetables = repository.getAllTimetables().firstOrNull() ?: emptyList()
        val is24Hour = runCatching { preferenceStorage.appConfigFlow.firstOrNull()?.is24HourFormat ?: true }.getOrDefault(true)
        val timeZone = TimeZone.currentSystemDefault()
        val javaNow = java.time.LocalDate.now()
        val today = LocalDate(javaNow.year, javaNow.monthValue, javaNow.dayOfMonth)

        val defaultData = buildComplicationData(
            request.complicationType, "课程表", "—", tapIntent
        ) ?: return null

        if (allTimetables.isEmpty()) {
            return ComplicationDataTimeline(defaultData, emptyList())
        }

        val todayStartInstant = today.atTime(0, 0).toInstant(timeZone)
        val timelineEndInstant = today.plus(2, DateTimeUnit.DAY).atTime(0, 0).toInstant(timeZone)
        val activeIntervals = mutableListOf<CourseInterval>()

        val daysToScan = listOf(today, today.plus(1, DateTimeUnit.DAY))
        for (targetDate in daysToScan) {
            for (table in allTimetables) {
                for (occurrence in table.resolveDate(targetDate)) {
                    val startInst = targetDate.atTime(occurrence.startTime).toInstant(timeZone)
                    val endInst = targetDate.atTime(occurrence.endTime).toInstant(timeZone)
                    if (endInst > todayStartInstant && startInst < timelineEndInstant) {
                        activeIntervals.add(
                            CourseInterval(
                                occurrence.course.copy(location = occurrence.location),
                                occurrence.timeSlot,
                                occurrence.startTime,
                                occurrence.endTime,
                                startInst,
                                endInst,
                            )
                        )
                    }
                }
            }
        }

        activeIntervals.sortBy { it.startInstant }

        if (activeIntervals.isEmpty()) {
            val noClassData = buildComplicationData(request.complicationType, "今日无课", "无待办", tapIntent) ?: defaultData
            return ComplicationDataTimeline(
                defaultData,
                listOf(
                    TimelineEntry(
                        validity = TimeInterval(
                            JavaInstant.ofEpochMilli(todayStartInstant.toEpochMilliseconds()),
                            JavaInstant.ofEpochMilli(timelineEndInstant.toEpochMilliseconds())
                        ),
                        complicationData = noClassData
                    )
                )
            )
        }

        val timelineEntries = mutableListOf<TimelineEntry>()
        var cursorInstant = todayStartInstant

        for (item in activeIntervals) {
            if (item.startInstant > cursorInstant) {
                val nextData = buildComplicationData(
                    request.complicationType,
                    title = item.startTime.toDisplayString(is24Hour),
                    text = item.course.name,
                    tapIntent = tapIntent
                )
                if (nextData != null) {
                    timelineEntries.add(
                        TimelineEntry(
                            validity = TimeInterval(
                                JavaInstant.ofEpochMilli(cursorInstant.toEpochMilliseconds()),
                                JavaInstant.ofEpochMilli(item.startInstant.toEpochMilliseconds())
                            ),
                            complicationData = nextData
                        )
                    )
                }
            }

            val inProgressData = buildComplicationData(
                request.complicationType,
                title = "上课中",
                text = item.course.name,
                tapIntent = tapIntent
            )
            val effectiveStart = if (item.startInstant < cursorInstant) cursorInstant else item.startInstant
            if (inProgressData != null && item.endInstant > effectiveStart) {
                timelineEntries.add(
                    TimelineEntry(
                        validity = TimeInterval(
                            JavaInstant.ofEpochMilli(effectiveStart.toEpochMilliseconds()),
                            JavaInstant.ofEpochMilli(item.endInstant.toEpochMilliseconds())
                        ),
                        complicationData = inProgressData
                    )
                )
            }

            if (item.endInstant > cursorInstant) {
                cursorInstant = item.endInstant
            }
        }

        if (cursorInstant < timelineEndInstant) {
            val finishedData = buildComplicationData(
                request.complicationType,
                title = "已结课",
                text = "后续无课",
                tapIntent = tapIntent
            )
            if (finishedData != null) {
                timelineEntries.add(
                    TimelineEntry(
                        validity = TimeInterval(
                            JavaInstant.ofEpochMilli(cursorInstant.toEpochMilliseconds()),
                            JavaInstant.ofEpochMilli(timelineEndInstant.toEpochMilliseconds())
                        ),
                        complicationData = finishedData
                    )
                )
            }
        }

        return ComplicationDataTimeline(defaultData, timelineEntries)
    }

    private fun buildComplicationData(
        type: ComplicationType,
        title: String,
        text: String,
        tapIntent: PendingIntent
    ): ComplicationData? {
        val shortName = if (text.length > 5) text.take(4) + "…" else text
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(shortName).build(),
                contentDescription = PlainComplicationText.Builder("课程表").build()
            ).setTitle(PlainComplicationText.Builder(title).build())
                .setTapAction(tapIntent).build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = PlainComplicationText.Builder("课程表").build()
            ).setTitle(PlainComplicationText.Builder(title).build())
                .setTapAction(tapIntent).build()

            else -> null
        }
    }
}
