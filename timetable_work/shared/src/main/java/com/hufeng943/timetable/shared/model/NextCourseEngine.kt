package com.hufeng943.timetable.shared.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

enum class NextCoursePhase {
    NO_SCHEDULE,
    BEFORE_CLASS,
    BETWEEN_CLASSES,
    IN_CLASS,
    NO_CLASS_TODAY,
    DAY_FINISHED,
    NO_UPCOMING,
}

data class NextCourseOccurrence(
    val timetableId: Long,
    val courseId: Long,
    val timeSlotId: Long,
    val date: LocalDate,
    val courseName: String,
    val location: String?,
    val teacher: String?,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val startInstant: Instant,
    val endInstant: Instant,
)

data class NextCourseState(
    val phase: NextCoursePhase,
    val today: LocalDate,
    val current: NextCourseOccurrence? = null,
    val next: NextCourseOccurrence? = null,
    val minutesRemaining: Int? = null,
    val minutesUntilNext: Int? = null,
    val currentProgress: Float? = null,
    val todayCourseCount: Int = 0,
) {
    val hasCurrentCourse: Boolean get() = current != null
    val hasUpcomingCourse: Boolean get() = next != null
    val dayFinished: Boolean get() = phase == NextCoursePhase.DAY_FINISHED
}

/**
 * Single source of truth for the time-sensitive "current / next course" state used by Wear UI,
 * Tile, complications and reminders. Weekly recurrence and date overrides are delegated to
 * [Timetable.resolveDate] so every surface follows the same scheduling rules.
 */
object NextCourseEngine {
    const val DEFAULT_LOOK_AHEAD_DAYS: Int = 21

    fun resolve(
        timetables: List<Timetable>,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        lookAheadDays: Int = DEFAULT_LOOK_AHEAD_DAYS,
    ): NextCourseState {
        require(lookAheadDays >= 0) { "lookAheadDays must be non-negative" }

        val localNow = now.toLocalDateTime(timeZone)
        val today = localNow.date
        val nowMillis = now.toEpochMilliseconds()
        val todayOccurrences = occurrencesForDate(timetables, today, timeZone)

        if (timetables.isEmpty()) {
            return NextCourseState(
                phase = NextCoursePhase.NO_SCHEDULE,
                today = today,
            )
        }

        val current = todayOccurrences.firstOrNull { occurrence ->
            nowMillis >= occurrence.startInstant.toEpochMilliseconds() &&
                nowMillis < occurrence.endInstant.toEpochMilliseconds()
        }
        val nextToday = todayOccurrences.firstOrNull { occurrence ->
            occurrence.startInstant.toEpochMilliseconds() > nowMillis
        }
        val next = nextToday ?: findNextOccurrence(
            timetables = timetables,
            startDate = today,
            timeZone = timeZone,
            lookAheadDays = lookAheadDays,
        )

        val completedToday = todayOccurrences.any {
            it.endInstant.toEpochMilliseconds() <= nowMillis
        }
        val phase = when {
            current != null -> NextCoursePhase.IN_CLASS
            nextToday != null && completedToday -> NextCoursePhase.BETWEEN_CLASSES
            nextToday != null -> NextCoursePhase.BEFORE_CLASS
            todayOccurrences.isNotEmpty() -> NextCoursePhase.DAY_FINISHED
            next != null -> NextCoursePhase.NO_CLASS_TODAY
            else -> NextCoursePhase.NO_UPCOMING
        }

        val remaining = current?.let { ceilMinutesBetween(now, it.endInstant) }
        val untilNext = next?.let { ceilMinutesBetween(now, it.startInstant) }
        val progress = current?.let { occurrence ->
            val start = occurrence.startInstant.toEpochMilliseconds()
            val end = occurrence.endInstant.toEpochMilliseconds()
            if (end <= start) null
            else ((nowMillis - start).toDouble() / (end - start).toDouble()).toFloat().coerceIn(0f, 1f)
        }

        return NextCourseState(
            phase = phase,
            today = today,
            current = current,
            next = next,
            minutesRemaining = remaining,
            minutesUntilNext = untilNext,
            currentProgress = progress,
            todayCourseCount = todayOccurrences.size,
        )
    }

    fun occurrencesForDate(
        timetables: List<Timetable>,
        date: LocalDate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): List<NextCourseOccurrence> = timetables
        .flatMap { timetable ->
            timetable.resolveDate(date).map { resolved ->
                val startInstant = date.atTime(resolved.startTime).toInstant(timeZone)
                val endInstant = date.atTime(resolved.endTime).toInstant(timeZone)
                NextCourseOccurrence(
                    timetableId = timetable.timetableId,
                    courseId = resolved.course.id,
                    timeSlotId = resolved.timeSlot.id,
                    date = date,
                    courseName = resolved.course.name,
                    location = resolved.location,
                    teacher = resolved.course.teacher,
                    startTime = resolved.startTime,
                    endTime = resolved.endTime,
                    startInstant = startInstant,
                    endInstant = endInstant,
                )
            }
        }
        .sortedWith(
            compareBy<NextCourseOccurrence> { it.startInstant }
                .thenBy { it.endInstant }
                .thenBy { it.timetableId }
                .thenBy { it.courseId }
                .thenBy { it.timeSlotId }
        )

    private fun findNextOccurrence(
        timetables: List<Timetable>,
        startDate: LocalDate,
        timeZone: TimeZone,
        lookAheadDays: Int,
    ): NextCourseOccurrence? {
        for (offset in 1..lookAheadDays) {
            val target = startDate.plus(offset, DateTimeUnit.DAY)
            val occurrence = occurrencesForDate(timetables, target, timeZone).firstOrNull()
            if (occurrence != null) return occurrence
        }
        return null
    }

    private fun ceilMinutesBetween(from: Instant, to: Instant): Int {
        val deltaMillis = to.toEpochMilliseconds() - from.toEpochMilliseconds()
        if (deltaMillis <= 0L) return 0
        return ((deltaMillis + 59_999L) / 60_000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
