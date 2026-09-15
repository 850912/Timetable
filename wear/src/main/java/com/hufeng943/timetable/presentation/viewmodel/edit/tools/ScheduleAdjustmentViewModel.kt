package com.hufeng943.timetable.presentation.viewmodel.edit.tools

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.ResolvedSchedule
import com.hufeng943.timetable.shared.model.ScheduleBatchOperations
import com.hufeng943.timetable.shared.model.ScheduleOverride
import com.hufeng943.timetable.shared.model.ScheduleOverrideType
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import com.hufeng943.timetable.shared.model.resolveDate
import com.hufeng943.timetable.surface.WearSurfaceRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import javax.inject.Inject

enum class CourseAdjustmentMode { SWAP, OCCUPY }

sealed interface ScheduleAdjustmentState {
    data object Loading : ScheduleAdjustmentState
    data class Ready(val timetables: List<Timetable>) : ScheduleAdjustmentState
    data class Error(val message: String) : ScheduleAdjustmentState
}

@HiltViewModel
class ScheduleAdjustmentViewModel @Inject constructor(
    private val repository: TimetableRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _state = MutableStateFlow<ScheduleAdjustmentState>(ScheduleAdjustmentState.Loading)
    val state: StateFlow<ScheduleAdjustmentState> = _state.asStateFlow()

    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed = _completed.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getAllTimetables().collect { tables ->
                _state.value = if (tables.isEmpty()) ScheduleAdjustmentState.Error("暂无课表")
                else ScheduleAdjustmentState.Ready(tables)
            }
        }
    }

    fun applyDayArrangement(timetableId: Long, targetDate: LocalDate, sourceDay: DayOfWeek) {
        viewModelScope.launch {
            runCatching {
                val tables = (_state.value as? ScheduleAdjustmentState.Ready)?.timetables.orEmpty()
                    .filter { it.timetableId == timetableId }
                require(tables.isNotEmpty()) { "课表不存在" }
                val dayDelta = sourceDay.isoNumber() - targetDate.dayOfWeek.isoNumber()
                val sourceDate = targetDate.plus(dayDelta, DateTimeUnit.DAY)
                require(sourceDate != targetDate) { "目标日期与来源星期相同" }

                tables.forEach { table ->
                    val target = table.resolveDate(targetDate)
                    val source = table.resolveDate(sourceDate)
                    val slotById = table.allCourses.flatMap { course -> course.timeSlots.map { it.id to (course.id to it) } }.toMap()
                    val updates = mutableMapOf<Long, Pair<Long, TimeSlot>>()

                    target.forEach { occurrence ->
                        val current = updates[occurrence.timeSlot.id]?.second ?: occurrence.timeSlot
                        updates[occurrence.timeSlot.id] = occurrence.course.id to
                            ScheduleBatchOperations.cancelDates(current, listOf(targetDate))
                    }
                    source.forEach { occurrence ->
                        val original = updates[occurrence.timeSlot.id]?.second
                            ?: slotById[occurrence.timeSlot.id]?.second
                            ?: occurrence.timeSlot
                        val extra = original.withExactOverride(
                            ScheduleOverride(
                                date = targetDate,
                                type = ScheduleOverrideType.EXTRA,
                                startTime = occurrence.startTime,
                                endTime = occurrence.endTime,
                                location = occurrence.location,
                                remark = occurrence.timeSlot.remark,
                            )
                        )
                        updates[occurrence.timeSlot.id] = occurrence.course.id to extra
                    }
                    updates.values.forEach { (courseId, slot) -> repository.upsertTimeSlot(slot, courseId) }
                }
                WearSurfaceRefresher.refresh(appContext)
                _completed.tryEmit(Unit)
            }.onFailure { _state.value = ScheduleAdjustmentState.Error(it.message ?: "调休失败") }
        }
    }

    fun applyCourseAdjustment(
        timetableId: Long,
        targetDate: LocalDate,
        sourceSlotId: Long,
        targetCourseId: Long,
        mode: CourseAdjustmentMode,
        permanent: Boolean,
    ) {
        viewModelScope.launch {
            runCatching {
                val table = (_state.value as? ScheduleAdjustmentState.Ready)?.timetables
                    ?.firstOrNull { it.timetableId == timetableId } ?: error("课表不存在")
                val occurrences = table.resolveDate(targetDate)
                val a = occurrences.firstOrNull { it.timeSlot.id == sourceSlotId } ?: error("A 课在当天不存在")
                val bCourse = table.allCourses.firstOrNull { it.id == targetCourseId } ?: error("B 课不存在")
                require(a.course.id != bCourse.id) { "A 课与 B 课不能相同" }
                val bOccurrence = occurrences.firstOrNull { it.course.id == bCourse.id }

                if (permanent) {
                    applyPermanent(a, bCourse, bOccurrence, mode)
                } else {
                    applyToday(targetDate, a, bCourse, bOccurrence, mode)
                }
                WearSurfaceRefresher.refresh(appContext)
                _completed.tryEmit(Unit)
            }.onFailure { _state.value = ScheduleAdjustmentState.Error(it.message ?: "课程调节失败") }
        }
    }

    private suspend fun applyToday(
        date: LocalDate,
        a: ResolvedSchedule,
        bCourse: Course,
        bOccurrence: ResolvedSchedule?,
        mode: CourseAdjustmentMode,
    ) {
        when (mode) {
            CourseAdjustmentMode.OCCUPY -> {
                repository.upsertTimeSlot(ScheduleBatchOperations.cancelDates(a.timeSlot, listOf(date)), a.course.id)
                val bSlot = bOccurrence?.timeSlot ?: bCourse.timeSlots.firstOrNull()
                if (bSlot != null) {
                    repository.upsertTimeSlot(
                        bSlot.withExactOverride(
                            ScheduleOverride(
                                date = date,
                                type = ScheduleOverrideType.EXTRA,
                                startTime = a.startTime,
                                endTime = a.endTime,
                                location = bCourse.location,
                                remark = bSlot.remark,
                            )
                        ),
                        bCourse.id,
                    )
                } else {
                    repository.upsertTimeSlot(
                        TimeSlot(
                            startTime = a.startTime,
                            endTime = a.endTime,
                            dayOfWeek = date.dayOfWeek,
                            recurrence = WeekPattern.DATE_ONLY,
                            overrides = listOf(
                                ScheduleOverride(date, ScheduleOverrideType.EXTRA, a.startTime, a.endTime, bCourse.location)
                            ),
                        ),
                        bCourse.id,
                    )
                }
            }
            CourseAdjustmentMode.SWAP -> {
                val b = requireNotNull(bOccurrence) { "换课要求 B 课当天也有课时" }
                repository.upsertTimeSlot(
                    a.timeSlot.withExactOverride(
                        ScheduleOverride(date, ScheduleOverrideType.EXTRA, b.startTime, b.endTime, a.location, a.timeSlot.remark)
                    ),
                    a.course.id,
                )
                repository.upsertTimeSlot(
                    b.timeSlot.withExactOverride(
                        ScheduleOverride(date, ScheduleOverrideType.EXTRA, a.startTime, a.endTime, b.location, b.timeSlot.remark)
                    ),
                    b.course.id,
                )
            }
        }
    }

    private suspend fun applyPermanent(
        a: ResolvedSchedule,
        bCourse: Course,
        bOccurrence: ResolvedSchedule?,
        mode: CourseAdjustmentMode,
    ) {
        when (mode) {
            CourseAdjustmentMode.OCCUPY -> {
                // Re-parent A's recurring slot to B. This keeps the same time definition,
                // so the edit page, home page and sync payload all stay aligned.
                repository.upsertTimeSlot(a.timeSlot, bCourse.id)
            }
            CourseAdjustmentMode.SWAP -> {
                val b = requireNotNull(bOccurrence) { "永久换课要求 B 课当天也有课时" }
                repository.upsertTimeSlot(a.timeSlot, b.course.id)
                repository.upsertTimeSlot(b.timeSlot, a.course.id)
            }
        }
    }
}

private fun TimeSlot.withExactOverride(override: ScheduleOverride): TimeSlot =
    copy(overrides = overrides.filterNot { it.date == override.date && it.endDate == null } + override)

private fun DayOfWeek.isoNumber(): Int = ordinal + 1
