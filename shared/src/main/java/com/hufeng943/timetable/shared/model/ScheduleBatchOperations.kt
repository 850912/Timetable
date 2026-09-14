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


    /** Apply a recurring override from [startDate] onward without materialising years of dates. */
    /** Apply one compact inclusive date-range rule to a recurring slot.
     * DATE_ONLY slots are intentionally handled by the concrete-date helpers.
     */
    fun applyRange(
        slot: TimeSlot,
        startDate: LocalDate,
        endDate: LocalDate,
        action: OpenEndedBatchAction,
        offsetMinutes: Int = 0,
    ): TimeSlot {
        require(endDate >= startDate) { "结束日期不能早于开始日期" }
        if (slot.recurrence == WeekPattern.DATE_ONLY) {
            val dates = slot.overrides.asSequence()
                .filter { it.date in startDate..endDate }
                .filter { it.type == ScheduleOverrideType.EXTRA || it.type == ScheduleOverrideType.MODIFIED || it.type == ScheduleOverrideType.CANCELLED }
                .map { it.date }.distinct().toList()
            return when (action) {
                OpenEndedBatchAction.SHIFT -> shiftDates(slot, dates, offsetMinutes)
                OpenEndedBatchAction.CANCEL -> cancelDates(slot, dates)
                OpenEndedBatchAction.RESTORE -> clearRange(slot, startDate, endDate)
            }
        }
        if (action == OpenEndedBatchAction.RESTORE) return clearRange(slot, startDate, endDate)

        val baseStart = requireNotNull(slot.startTime) { "课时缺少开始时间" }
        val baseEnd = requireNotNull(slot.endTime) { "课时缺少结束时间" }
        val rangeOverride = when (action) {
            OpenEndedBatchAction.CANCEL -> ScheduleOverride(
                date = startDate, endDate = endDate, type = ScheduleOverrideType.CANCELLED,
                startTime = baseStart, endTime = baseEnd,
            )
            OpenEndedBatchAction.SHIFT -> {
                require(offsetMinutes != 0) { "移动分钟不能为 0" }
                val shiftedStart = baseStart.shiftSameDay(offsetMinutes)
                val shiftedEnd = baseEnd.shiftSameDay(offsetMinutes)
                require(shiftedEnd > shiftedStart) { "调整后结束时间必须晚于开始时间" }
                ScheduleOverride(
                    date = startDate, endDate = endDate, type = ScheduleOverrideType.MODIFIED,
                    startTime = shiftedStart, endTime = shiftedEnd,
                )
            }
            OpenEndedBatchAction.RESTORE -> error("handled above")
        }
        return slot.copy(overrides = slot.overrides + rangeOverride)
    }

    fun applyOpenEnded(
        slot: TimeSlot,
        startDate: LocalDate,
        action: OpenEndedBatchAction,
        offsetMinutes: Int = 0,
    ): TimeSlot {
        if (slot.recurrence == WeekPattern.DATE_ONLY) {
            val dates = slot.overrides.asSequence()
                .filter { it.date >= startDate }
                .filter { it.type == ScheduleOverrideType.EXTRA || it.type == ScheduleOverrideType.MODIFIED || it.type == ScheduleOverrideType.CANCELLED }
                .map { it.date }.distinct().toList()
            return when (action) {
                OpenEndedBatchAction.SHIFT -> shiftDates(slot, dates, offsetMinutes)
                OpenEndedBatchAction.CANCEL -> cancelDates(slot, dates)
                OpenEndedBatchAction.RESTORE -> clearDates(slot, dates)
            }
        }

        if (action == OpenEndedBatchAction.RESTORE) return clearFrom(slot, startDate)
        val baseStart = requireNotNull(slot.startTime) { "课时缺少开始时间" }
        val baseEnd = requireNotNull(slot.endTime) { "课时缺少结束时间" }
        val rangeOverride = when (action) {
            OpenEndedBatchAction.CANCEL -> ScheduleOverride(
                date = startDate, endDate = LocalDate(9999, 12, 31), type = ScheduleOverrideType.CANCELLED,
                startTime = baseStart, endTime = baseEnd,
            )
            OpenEndedBatchAction.SHIFT -> {
                require(offsetMinutes != 0) { "移动分钟不能为 0" }
                val shiftedStart = baseStart.shiftSameDay(offsetMinutes)
                val shiftedEnd = baseEnd.shiftSameDay(offsetMinutes)
                require(shiftedEnd > shiftedStart) { "调整后结束时间必须晚于开始时间" }
                ScheduleOverride(
                    date = startDate, endDate = LocalDate(9999, 12, 31), type = ScheduleOverrideType.MODIFIED,
                    startTime = shiftedStart, endTime = shiftedEnd,
                )
            }
            OpenEndedBatchAction.RESTORE -> error("handled above")
        }
        return slot.copy(overrides = slot.overrides + rangeOverride)
    }

    /** Clear exact and range overrides from [startDate] onward, preserving earlier history. */
    fun clearFrom(slot: TimeSlot, startDate: LocalDate): TimeSlot {
        val previousDay = LocalDate.fromEpochDays(startDate.toEpochDays() - 1)
        val restored = buildList {
            slot.overrides.forEach { override ->
                val rangeEnd = override.endDate
                if (rangeEnd != null) {
                    when {
                        rangeEnd < startDate -> add(override)
                        override.date < startDate -> add(override.copy(endDate = previousDay))
                        else -> Unit
                    }
                } else if (override.date < startDate) {
                    add(override)
                } else if (slot.recurrence == WeekPattern.DATE_ONLY && override.type == ScheduleOverrideType.CANCELLED) {
                    add(override.copy(type = ScheduleOverrideType.EXTRA))
                }
            }
        }
        return slot.copy(overrides = restored)
    }


    /** Remove date overrides inside an inclusive range, splitting long-running overrides when needed. */
    fun clearRange(
        slot: TimeSlot,
        startDate: LocalDate,
        endDate: LocalDate,
        timeWindowStart: LocalTime? = null,
        timeWindowEnd: LocalTime? = null,
    ): TimeSlot {
        require(endDate >= startDate) { "结束日期不能早于开始日期" }
        val before = LocalDate.fromEpochDays(startDate.toEpochDays() - 1)
        val after = LocalDate.fromEpochDays(endDate.toEpochDays() + 1)
        val result = buildList {
            slot.overrides.forEach { override ->
                val overrideEnd = override.endDate ?: override.date
                val overlapsDate = override.date <= endDate && overrideEnd >= startDate
                val start = override.startTime ?: slot.startTime
                val finish = override.endTime ?: slot.endTime
                val overlapsTime = when {
                    timeWindowStart == null && timeWindowEnd == null -> true
                    start == null || finish == null -> false
                    timeWindowStart != null && timeWindowEnd != null -> start < timeWindowEnd && finish > timeWindowStart
                    timeWindowStart != null -> finish > timeWindowStart
                    else -> start < requireNotNull(timeWindowEnd)
                }
                if (!overlapsDate || !overlapsTime) {
                    add(override)
                    return@forEach
                }

                if (override.endDate == null) {
                    if (slot.recurrence == WeekPattern.DATE_ONLY && override.type == ScheduleOverrideType.CANCELLED) {
                        add(override.copy(type = ScheduleOverrideType.EXTRA))
                    }
                    return@forEach
                }

                if (override.date < startDate) add(override.copy(endDate = before))
                if (overrideEnd > endDate) add(override.copy(date = after))
            }
        }
        return slot.copy(overrides = result)
    }

    fun overlapsWindow(slot: TimeSlot, start: LocalTime?, end: LocalTime?): Boolean {
        if (start == null && end == null) return true
        val slotStart = slot.startTime ?: return false
        val slotEnd = slot.endTime ?: return false
        return when {
            start != null && end != null -> slotStart < end && slotEnd > start
            start != null -> slotEnd > start
            else -> slotStart < requireNotNull(end)
        }
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

enum class OpenEndedBatchAction { SHIFT, CANCEL, RESTORE }

private fun LocalTime.shiftSameDay(offsetMinutes: Int): LocalTime {
    val minuteOfDay = hour * 60 + minute + offsetMinutes
    require(minuteOfDay in 0..1439) { "调整后时间超出当天范围" }
    return LocalTime(minuteOfDay / 60, minuteOfDay % 60, second, nanosecond)
}
