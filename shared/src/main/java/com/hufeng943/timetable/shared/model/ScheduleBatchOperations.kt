package com.hufeng943.timetable.shared.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Shared date-scoped timetable editing helpers used by both phone and Wear UI.
 *
 * These operations intentionally stay inside the existing TimeSlot + ScheduleOverride
 * model so they remain compatible with the current JSON/CSV/ICS formats and the legacy
 * Wear Data Layer transport.
 */
object ScheduleBatchOperations {
    fun dateOnlySlot(
        dates: Collection<LocalDate>,
        startTime: LocalTime,
        endTime: LocalTime,
        remark: String? = null,
        location: String? = null,
    ): TimeSlot {
        val orderedDates = dates.distinct().sorted()
        require(orderedDates.isNotEmpty()) { "请至少选择一个日期" }
        require(endTime > startTime) { "结束时间必须晚于开始时间" }

        return TimeSlot(
            startTime = startTime,
            endTime = endTime,
            dayOfWeek = orderedDates.first().dayOfWeek,
            recurrence = WeekPattern.DATE_ONLY,
            remark = remark,
            overrides = orderedDates.map { date ->
                ScheduleOverride(
                    date = date,
                    type = ScheduleOverrideType.EXTRA,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    remark = remark,
                )
            },
        )
    }

    fun cancelDates(slot: TimeSlot, dates: Collection<LocalDate>): TimeSlot {
        val dateSet = dates.toSet()
        if (dateSet.isEmpty()) return slot
        val retained = slot.overrides.filterNot { it.date in dateSet }
        return slot.copy(
            overrides = retained + dateSet.sorted().map { date ->
                val previous = slot.overrides.lastOrNull { it.date == date }
                ScheduleOverride(
                    date = date,
                    type = ScheduleOverrideType.CANCELLED,
                    // Preserve one-off details so DATE_ONLY lessons can be restored later.
                    startTime = previous?.startTime ?: slot.startTime,
                    endTime = previous?.endTime ?: slot.endTime,
                    location = previous?.location,
                    remark = previous?.remark,
                )
            }
        )
    }

    fun clearDates(slot: TimeSlot, dates: Collection<LocalDate>): TimeSlot {
        val dateSet = dates.toSet()
        if (dateSet.isEmpty()) return slot
        if (slot.recurrence == WeekPattern.DATE_ONLY) {
            return slot.copy(
                overrides = slot.overrides.map { override ->
                    if (override.date in dateSet && override.type == ScheduleOverrideType.CANCELLED) {
                        override.copy(
                            type = ScheduleOverrideType.EXTRA,
                            startTime = override.startTime ?: slot.startTime,
                            endTime = override.endTime ?: slot.endTime,
                        )
                    } else {
                        override
                    }
                }
            )
        }
        return slot.copy(overrides = slot.overrides.filterNot { it.date in dateSet })
    }

    /**
     * Shift concrete occurrences on [dates] while keeping each occurrence on the same day.
     * Existing date-specific location/remark data is preserved.
     */
    fun shiftDates(slot: TimeSlot, dates: Collection<LocalDate>, offsetMinutes: Int): TimeSlot {
        if (offsetMinutes == 0) return slot
        val dateSet = dates.toSet()
        if (dateSet.isEmpty()) return slot
        val baseStart = requireNotNull(slot.startTime) { "课时缺少开始时间" }
        val baseEnd = requireNotNull(slot.endTime) { "课时缺少结束时间" }

        val retained = slot.overrides.filterNot { it.date in dateSet }
        val replacements = dateSet.sorted().map { date ->
            val previous = slot.overrides.lastOrNull { it.date == date }
            if (previous?.type == ScheduleOverrideType.CANCELLED) {
                previous
            } else {
                val currentStart = previous?.startTime ?: baseStart
                val currentEnd = previous?.endTime ?: baseEnd
                val shiftedStart = currentStart.shiftSameDay(offsetMinutes)
                val shiftedEnd = currentEnd.shiftSameDay(offsetMinutes)
                require(shiftedEnd > shiftedStart) { "调整后结束时间必须晚于开始时间" }
                ScheduleOverride(
                    date = date,
                    type = if (slot.recurrence == WeekPattern.DATE_ONLY) ScheduleOverrideType.EXTRA else ScheduleOverrideType.MODIFIED,
                    startTime = shiftedStart,
                    endTime = shiftedEnd,
                    location = previous?.location,
                    remark = previous?.remark,
                )
            }
        }
        return slot.copy(overrides = retained + replacements)
    }


    /** Dates that already have an explicit override and match the optional time window.
     * Used by the restore action because cancelled occurrences are intentionally absent from resolveDate().
     */
    fun overrideDates(
        slot: TimeSlot,
        startDate: LocalDate,
        endDate: LocalDate,
        timeWindowStart: LocalTime? = null,
        timeWindowEnd: LocalTime? = null,
    ): List<LocalDate> {
        require(endDate >= startDate) { "结束日期不能早于开始日期" }
        return slot.overrides.asSequence()
            .filter { it.date >= startDate && it.date <= endDate }
            .filter { override -> slot.recurrence != WeekPattern.DATE_ONLY || override.type == ScheduleOverrideType.CANCELLED }
            .filter { override ->
                val start = override.startTime ?: slot.startTime
                val end = override.endTime ?: slot.endTime
                if (timeWindowStart == null && timeWindowEnd == null) {
                    true
                } else if (start == null || end == null) {
                    false
                } else {
                    when {
                        timeWindowStart != null && timeWindowEnd != null -> start < timeWindowEnd && end > timeWindowStart
                        timeWindowStart != null -> end > timeWindowStart
                        else -> start < requireNotNull(timeWindowEnd)
                    }
                }
            }
            .map { it.date }
            .distinct()
            .sorted()
            .toList()
    }

    fun matchingDates(
        timetable: Timetable,
        startDate: LocalDate,
        endDate: LocalDate,
        timeWindowStart: LocalTime? = null,
        timeWindowEnd: LocalTime? = null,
    ): Map<Long, List<LocalDate>> {
        require(endDate >= startDate) { "结束日期不能早于开始日期" }
        val result = linkedMapOf<Long, MutableList<LocalDate>>()
        var day = startDate
        while (day <= endDate) {
            timetable.resolveDate(day).forEach { occurrence ->
                val inWindow = when {
                    timeWindowStart == null && timeWindowEnd == null -> true
                    timeWindowStart != null && timeWindowEnd != null ->
                        occurrence.startTime < timeWindowEnd && occurrence.endTime > timeWindowStart
                    timeWindowStart != null -> occurrence.endTime > timeWindowStart
                    else -> occurrence.startTime < requireNotNull(timeWindowEnd)
                }
                if (inWindow) result.getOrPut(occurrence.timeSlot.id) { mutableListOf() } += day
            }
            day = LocalDate.fromEpochDays(day.toEpochDays() + 1)
        }
        return result
    }
}

private fun LocalTime.shiftSameDay(offsetMinutes: Int): LocalTime {
    val minuteOfDay = hour * 60 + minute + offsetMinutes
    require(minuteOfDay in 0..1439) { "调整后时间超出当天范围" }
    return LocalTime(minuteOfDay / 60, minuteOfDay % 60, second, nanosecond)
}
