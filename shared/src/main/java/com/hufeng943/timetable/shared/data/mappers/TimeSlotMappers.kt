package com.hufeng943.timetable.shared.data.mappers

import java.util.UUID
import com.hufeng943.timetable.shared.data.entities.TimeSlotEntity
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.WeekPattern
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber

fun TimeSlot.toTimeSlotEntity(courseId: Long): TimeSlotEntity {
    // LocalTime/DayOfWeek are nullable in the UI model. Reject malformed writes
    // explicitly instead of leaking a KotlinNullPointerException into Room.
    val startTime = requireNotNull(this.startTime) { "开始时间不能为空" }
    val endTime = requireNotNull(this.endTime) { "结束时间不能为空" }
    val dayOfWeek = requireNotNull(this.dayOfWeek) { "星期不能为空" }

    // LocalTime -> Int (分钟数)
    val startMinute = startTime.hour * 60 + startTime.minute
    val endMinute = endTime.hour * 60 + endTime.minute

    // DayOfWeek -> Int (isoDayNumber)
    val dayOfWeekInt = dayOfWeek.isoDayNumber
    require(startMinute in 0..1439) { "开始时间无效" }
    require(endMinute in 0..1439) { "结束时间无效" }
    require(startMinute != endMinute) { "课程开始和结束时间不能相同" }

    // WeekPattern -> Int (ordinal)
    val recurrenceInt = this.recurrence.ordinal

    return TimeSlotEntity(
        id = this.id,
        syncId = UUID.randomUUID().toString(),
        courseId = courseId,
        dayOfWeek = dayOfWeekInt,
        startMinute = startMinute,
        endMinute = endMinute,
        recurrence = recurrenceInt,
        remark = this.remark
    )
}

fun TimeSlotEntity.toTimeSlot(): TimeSlot {
    // Int (分钟数) -> LocalTime
    val startTime = LocalTime(hour = this.startMinute / 60, minute = this.startMinute % 60)
    val endTime = LocalTime(hour = this.endMinute / 60, minute = this.endMinute % 60)

    // Int (isoDayNumber) -> DayOfWeek
    val dayOfWeek = DayOfWeek(this.dayOfWeek)

    // Int (ordinal) -> WeekPattern
    val recurrence = WeekPattern.entries.getOrElse(this.recurrence) {
        WeekPattern.EVERY_WEEK // 默认值
    }

    return TimeSlot(
        id = this.id,
        startTime = startTime,
        endTime = endTime,
        dayOfWeek = dayOfWeek,
        recurrence = recurrence,
        remark = this.remark
    )
}
