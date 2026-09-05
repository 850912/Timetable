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
import com.hufeng943.timetable.presentation.MainActivity
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.WeekPattern
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import javax.inject.Inject
import java.time.Instant as JavaInstant

@AndroidEntryPoint
class MainComplicationService : SuspendingTimelineComplicationDataSourceService() {

    @Inject
    lateinit var repository: TimetableRepository

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
        val timeZone = TimeZone.currentSystemDefault()
        val javaNow = java.time.LocalDate.now()
        val today = LocalDate(javaNow.year, javaNow.monthValue, javaNow.dayOfMonth)

        val currentTable = allTimetables.filter { tt ->
            val end = tt.semesterEnd ?: LocalDate.fromEpochDays(tt.semesterStart.toEpochDays() + 140)
            today >= tt.semesterStart && today <= end
        }.maxByOrNull { it.semesterStart } ?: allTimetables.maxByOrNull { it.semesterStart }

        val defaultData = buildComplicationData(
            request.complicationType, "课程表", "—", tapIntent
        ) ?: return null

        if (currentTable == null) {
            return ComplicationDataTimeline(defaultData, emptyList())
        }

        val semesterStart = currentTable.semesterStart
        val semesterEnd = currentTable.semesterEnd ?: LocalDate.fromEpochDays(semesterStart.toEpochDays() + 140)
        val semesterStartMonday = currentTable.semesterStartMonday

        val todayStartInstant = today.atTime(0, 0).toInstant(timeZone)
        val timelineEndInstant = today.plus(2, DateTimeUnit.DAY).atTime(0, 0).toInstant(timeZone)

        val activeIntervals = mutableListOf<CourseInterval>()

        val daysToScan = listOf(
            today.minus(1, DateTimeUnit.DAY),
            today,
            today.plus(1, DateTimeUnit.DAY)
        )

        for (targetDate in daysToScan) {
            if (targetDate < semesterStart || targetDate > semesterEnd) continue

            val dayOffset = (targetDate.dayOfWeek.isoDayNumber - 1).mod(7)
            val dateMonday = LocalDate.fromEpochDays(targetDate.toEpochDays() - dayOffset)
            val weekIndex = (((dateMonday.toEpochDays() - semesterStartMonday.toEpochDays()) / 7) + 1).toInt()

            for (course in currentTable.allCourses) {
                for (slot in course.timeSlots) {
                    val slotStart = slot.startTime ?: continue
                    val slotEnd = slot.endTime ?: continue

                    if (slot.dayOfWeek == targetDate.dayOfWeek) {
                        val isMatch = when (slot.recurrence) {
                            WeekPattern.EVERY_WEEK -> true
                            WeekPattern.ODD_WEEK -> weekIndex % 2 != 0
                            WeekPattern.EVEN_WEEK -> weekIndex % 2 == 0
                        }
                        if (isMatch) {
                            val isCrossMidnight = (slotEnd.hour * 3600 + slotEnd.minute * 60) <
                                    (slotStart.hour * 3600 + slotStart.minute * 60)
                            val startInst = targetDate.atTime(slotStart).toInstant(timeZone)
                            val endInst = if (isCrossMidnight) {
                                targetDate.plus(1, DateTimeUnit.DAY).atTime(slotEnd).toInstant(timeZone)
                            } else {
                                targetDate.atTime(slotEnd).toInstant(timeZone)
                            }

                            if (endInst > todayStartInstant && startInst < timelineEndInstant) {
                                activeIntervals.add(CourseInterval(course, slot, slotStart, slotEnd, startInst, endInst))
                            }
                        }
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
                    title = "%02d:%02d".format(item.startTime.hour, item.startTime.minute),
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
