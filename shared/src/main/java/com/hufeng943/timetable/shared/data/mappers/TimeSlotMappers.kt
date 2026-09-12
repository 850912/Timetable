package com.hufeng943.timetable.shared.data.mappers

import java.util.UUID
import com.hufeng943.timetable.shared.data.entities.TimeSlotEntity
import com.hufeng943.timetable.shared.model.ScheduleOverride
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.WeekPattern
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val overrideJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun TimeSlot.toTimeSlotEntity(courseId: Long): TimeSlotEntity {
    val startTime = requireNotNull(startTime) { "开始时间不能为空" }
    val endTime = requireNotNull(endTime) { "结束时间不能为空" }
    val dayOfWeek = requireNotNull(dayOfWeek) { "星期不能为空" }
    val startMinute = startTime.hour * 60 + startTime.minute
    val endMinute = endTime.hour * 60 + endTime.minute
    require(startMinute in 0..1439) { "开始时间无效" }
    require(endMinute in 0..1439) { "结束时间无效" }
    require(endMinute > startMinute) { "结束时间必须晚于开始时间（暂不支持跨午夜课程）" }
    overrides.forEach { override ->
        val overrideStart = override.startTime
        val overrideEnd = override.endTime
        if (override.type != com.hufeng943.timetable.shared.model.ScheduleOverrideType.CANCELLED &&
            overrideStart != null && overrideEnd != null
        ) {
            require(overrideEnd > overrideStart) { "日期例外的结束时间必须晚于开始时间" }
        }
    }

    return TimeSlotEntity(
        id = id,
        syncId = UUID.randomUUID().toString(),
        courseId = courseId,
        dayOfWeek = dayOfWeek.isoDayNumber,
        startMinute = startMinute,
        endMinute = endMinute,
        recurrence = recurrence.ordinal,
        remark = remark,
        overridesJson = overrideJson.encodeToString(overrides)
    )
}

fun TimeSlotEntity.toTimeSlot(): TimeSlot {
    val overrides = runCatching {
        overrideJson.decodeFromString<List<ScheduleOverride>>(overridesJson)
    }.getOrDefault(emptyList())
    return TimeSlot(
        id = id,
        startTime = LocalTime(hour = startMinute / 60, minute = startMinute % 60),
        endTime = LocalTime(hour = endMinute / 60, minute = endMinute % 60),
        dayOfWeek = DayOfWeek(dayOfWeek),
        recurrence = WeekPattern.entries.getOrElse(recurrence) { WeekPattern.EVERY_WEEK },
        remark = remark,
        overrides = overrides
    )
}
