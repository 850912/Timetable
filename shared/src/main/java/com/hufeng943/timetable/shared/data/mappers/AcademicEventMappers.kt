package com.hufeng943.timetable.shared.data.mappers

import com.hufeng943.timetable.shared.data.entities.AcademicEventEntity
import com.hufeng943.timetable.shared.model.AcademicEvent
import com.hufeng943.timetable.shared.model.AcademicEventType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import java.util.UUID

fun AcademicEventEntity.toAcademicEvent(): AcademicEvent = AcademicEvent(
    id = id,
    title = title,
    type = AcademicEventType.entries.getOrElse(type) { AcademicEventType.OTHER },
    date = LocalDate.fromEpochDays(dateEpochDay.toInt()),
    time = timeMinute?.let { LocalTime(it / 60, it % 60) },
    courseName = courseName,
    location = location,
    note = note,
    reminderMinutesBefore = reminderMinutesBefore,
    completed = completed,
)

fun AcademicEvent.toAcademicEventEntity(timetableId: Long): AcademicEventEntity = AcademicEventEntity(
    id = id,
    syncId = UUID.randomUUID().toString(),
    timetableId = timetableId,
    title = title,
    type = type.ordinal,
    dateEpochDay = date.toEpochDays(),
    timeMinute = time?.let { it.hour * 60 + it.minute },
    courseName = courseName,
    location = location,
    note = note,
    reminderMinutesBefore = reminderMinutesBefore,
    completed = completed,
)
