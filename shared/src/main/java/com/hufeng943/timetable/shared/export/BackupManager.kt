package com.hufeng943.timetable.shared.export

import com.hufeng943.timetable.shared.model.AcademicEvent
import com.hufeng943.timetable.shared.model.AcademicEventType
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.ScheduleOverride
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Clock

@Serializable
data class TimeSlotBackupDto(
    val syncId: String? = null,
    val dayOfWeek: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val recurrence: Int,
    val remark: String? = null,
    val overrides: List<ScheduleOverride> = emptyList(),
    val batchGroupId: String? = null,
)

@Serializable
data class CourseBackupDto(
    val syncId: String? = null,
    val name: String,
    val teacher: String? = null,
    val location: String? = null, val color: Long = -1L,
    val slots: List<TimeSlotBackupDto> = emptyList()
)


@Serializable
data class AcademicEventBackupDto(
    val syncId: String? = null,
    val title: String,
    val type: Int,
    val dateEpochDays: Long,
    val timeMinute: Int? = null,
    val courseName: String? = null,
    val location: String? = null,
    val note: String? = null,
    val reminderMinutesBefore: Int? = null,
    val completed: Boolean = false,
)

@Serializable
data class TimetableBackupDto(
    val syncId: String? = null,
    val semesterName: String,
    val startEpochDays: Long,
    val endEpochDays: Long?,
    val color: Long = -1L,
    val courses: List<CourseBackupDto> = emptyList(),
    val events: List<AcademicEventBackupDto> = emptyList(),
)

@Serializable
data class TimetableBackupContainer(
    val schemaVersion: Int = 4,
    val appVersion: String = "3.4.0",
    val backupEpochMillis: Long,
    val timetables: List<TimetableBackupDto>
)

@OptIn(ExperimentalSerializationApi::class)
object BackupManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun backup(outputStream: OutputStream, timetables: List<Timetable>) {
        val dtos = timetables.map { tt ->
            TimetableBackupDto(
                syncId = tt.syncId,
                semesterName = tt.semesterName,
                startEpochDays = tt.semesterStart.toEpochDays(),
                endEpochDays = tt.semesterEnd?.toEpochDays(),
                color = tt.color,
                courses = tt.allCourses.map { course ->
                    CourseBackupDto(
                        syncId = course.syncId,
                        name = course.name,
                        teacher = course.teacher,
                        location = course.location, color = course.color,
                        slots = course.timeSlots.mapNotNull { slot ->
                            val dow = slot.dayOfWeek ?: return@mapNotNull null
                            val st = slot.startTime ?: return@mapNotNull null
                            val et = slot.endTime ?: return@mapNotNull null
                            val rec = when (slot.recurrence) {
                                WeekPattern.EVERY_WEEK -> 0
                                WeekPattern.ODD_WEEK -> 1
                                WeekPattern.EVEN_WEEK -> 2
                                WeekPattern.DATE_ONLY -> 3
                            }
                            TimeSlotBackupDto(
                                syncId = slot.syncId,
                                dayOfWeek = dow.ordinal + 1,
                                startHour = st.hour,
                                startMinute = st.minute,
                                endHour = et.hour,
                                endMinute = et.minute,
                                recurrence = rec,
                                remark = slot.remark,
                                overrides = slot.overrides,
                                batchGroupId = slot.batchGroupId,
                            )
                        }
                    )
                },
                events = tt.events.map { event ->
                    AcademicEventBackupDto(
                        syncId = event.syncId,
                        title = event.title,
                        type = event.type.ordinal,
                        dateEpochDays = event.date.toEpochDays(),
                        timeMinute = event.time?.let { it.hour * 60 + it.minute },
                        courseName = event.courseName,
                        location = event.location,
                        note = event.note,
                        reminderMinutesBefore = event.reminderMinutesBefore,
                        completed = event.completed,
                    )
                }
            )
        }

        val container = TimetableBackupContainer(
            schemaVersion = 4,
            backupEpochMillis = Clock.System.now().toEpochMilliseconds(),
            timetables = dtos
        )
        json.encodeToStream(container, outputStream)
        outputStream.flush()
    }

    fun restore(inputStream: InputStream): List<Timetable> {
        val container = json.decodeFromStream<TimetableBackupContainer>(inputStream)
        val now = Clock.System.now()

        return container.timetables.map { dto ->
            val start = LocalDate.fromEpochDays(dto.startEpochDays.toInt())
            val end = dto.endEpochDays?.let { LocalDate.fromEpochDays(it.toInt()) }

            val courses = dto.courses.map { cDto ->
                val slots = cDto.slots.map { sDto ->
                    val dow = DayOfWeek.entries.getOrNull(sDto.dayOfWeek - 1) ?: DayOfWeek.MONDAY
                    val rec = when (sDto.recurrence) {
                        1 -> WeekPattern.ODD_WEEK
                        2 -> WeekPattern.EVEN_WEEK
                        3 -> WeekPattern.DATE_ONLY
                        else -> WeekPattern.EVERY_WEEK
                    }
                    TimeSlot(
                        id = 0,
                        dayOfWeek = dow,
                        startTime = LocalTime(sDto.startHour, sDto.startMinute),
                        endTime = LocalTime(sDto.endHour, sDto.endMinute),
                        recurrence = rec,
                        remark = sDto.remark,
                        overrides = sDto.overrides,
                        batchGroupId = sDto.batchGroupId,
                        syncId = sDto.syncId,
                    )
                }

                Course(
                    id = 0,
                    name = cDto.name,
                    teacher = cDto.teacher,
                    location = cDto.location, color = cDto.color,
                    timeSlots = slots,
                    syncId = cDto.syncId
                )
            }

            val events = dto.events.map { event ->
                AcademicEvent(
                    id = 0,
                    title = event.title,
                    type = AcademicEventType.entries.getOrElse(event.type) { AcademicEventType.OTHER },
                    date = LocalDate.fromEpochDays(event.dateEpochDays.toInt()),
                    time = event.timeMinute?.let { LocalTime(it / 60, it % 60) },
                    courseName = event.courseName,
                    location = event.location,
                    note = event.note,
                    reminderMinutesBefore = event.reminderMinutesBefore,
                    completed = event.completed,
                    syncId = event.syncId,
                )
            }

            Timetable(
                timetableId = 0,
                semesterName = dto.semesterName,
                createdAt = now,
                semesterStart = start,
                semesterEnd = end,
                allCourses = courses,
                events = events,
                color = dto.color,
                syncId = dto.syncId
            )
        }
    }
}
