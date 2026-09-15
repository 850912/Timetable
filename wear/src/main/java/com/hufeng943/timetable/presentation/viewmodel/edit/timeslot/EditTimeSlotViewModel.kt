package com.hufeng943.timetable.presentation.viewmodel.edit.timeslot

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.presentation.ui.NavArgs
import com.hufeng943.timetable.presentation.ui.common.ui.TimeSlotUi
import com.hufeng943.timetable.presentation.ui.common.ui.mappers.toTimeSlot
import com.hufeng943.timetable.presentation.ui.common.ui.mappers.toTimeSlotUi
import com.hufeng943.timetable.presentation.viewmodel.AppError
import com.hufeng943.timetable.presentation.viewmodel.UiState
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.model.ScheduleOverride
import com.hufeng943.timetable.shared.model.ScheduleOverrideType
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.WeekPattern
import com.hufeng943.timetable.surface.WearSurfaceRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditTimeSlotViewModel @Inject constructor(
    private val repository: TimetableRepository,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cId: Long? = savedStateHandle.longArg(NavArgs.COURSE_ID)
    private val sId: Long? = savedStateHandle.longArg(NavArgs.TIME_SLOT_ID)

    private val _uiState = MutableStateFlow<UiState<TimeSlotUi>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _groupSyncPrompt = MutableStateFlow<Int?>(null)
    /** Number of sibling dates that can receive the same time change. */
    val groupSyncPrompt = _groupSyncPrompt.asStateFlow()

    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed = _completed.asSharedFlow()

    private var originalSlot: TimeSlot? = null

    init {
        viewModelScope.launch {
            try {
                val domainData = sId?.let { id ->
                    repository.getTimeSlotById(id).first() ?: TimeSlot()
                } ?: TimeSlot()
                originalSlot = domainData.takeIf { it.id != 0L }
                val course = cId?.let { id -> repository.getCourseById(id).first() }
                    ?: throw AppError.CourseNotFound(cId)
                _uiState.value = UiState.Success(domainData.toTimeSlotUi(course.color))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e)
            }
        }
    }

    fun onAction(action: EditTimeSlotAction) {
        when (action) {
            is EditTimeSlotAction.UpdateStartTime -> updateSuccessState { current ->
                val newStart = action.startTime
                val newEnd = current.endTime?.let { if (newStart >= it) plusMinutes(newStart, 60) else it }
                current.copy(
                    startTime = newStart,
                    endTime = newEnd,
                    overrides = current.overrides.withTimes(newStart, newEnd),
                )
            }

            is EditTimeSlotAction.UpdateEndTime -> updateSuccessState { current ->
                val newEnd = action.endTime
                val newStart = current.startTime?.let { if (newEnd <= it) minusMinutes(newEnd, 60) else it }
                current.copy(
                    startTime = newStart,
                    endTime = newEnd,
                    overrides = current.overrides.withTimes(newStart, newEnd),
                )
            }

            is EditTimeSlotAction.UpdateDayOfWeek -> updateSuccessState { current ->
                current.copy(
                    dayOfWeek = action.dayOfWeek,
                    recurrence = if (current.recurrence == WeekPattern.DATE_ONLY) WeekPattern.EVERY_WEEK else current.recurrence,
                    selectedDates = emptySet(),
                    overrides = if (current.recurrence == WeekPattern.DATE_ONLY) emptyList() else current.overrides,
                    batchGroupId = if (current.recurrence == WeekPattern.DATE_ONLY) null else current.batchGroupId,
                )
            }

            is EditTimeSlotAction.UpdateDates -> updateSuccessState { current ->
                val dates = action.dates.toSortedSet()
                if (dates.isEmpty()) {
                    current.copy(selectedDates = emptySet())
                } else {
                    val firstDate = dates.first()
                    current.copy(
                        dayOfWeek = firstDate.dayOfWeek,
                        recurrence = WeekPattern.DATE_ONLY,
                        selectedDates = dates,
                        overrides = dates.map { date ->
                            ScheduleOverride(
                                date = date,
                                type = ScheduleOverrideType.EXTRA,
                                startTime = current.startTime,
                                endTime = current.endTime,
                                remark = current.remark,
                            )
                        },
                    )
                }
            }

            is EditTimeSlotAction.UpdateRecurrence -> updateSuccessState { current ->
                current.copy(
                    recurrence = action.recurrence,
                    selectedDates = if (action.recurrence == WeekPattern.DATE_ONLY) current.selectedDates else emptySet(),
                    overrides = if (action.recurrence == WeekPattern.DATE_ONLY) current.overrides else emptyList(),
                    batchGroupId = if (action.recurrence == WeekPattern.DATE_ONLY) current.batchGroupId else null,
                )
            }

            is EditTimeSlotAction.UpdateRemark -> updateSuccessState { current ->
                current.copy(
                    remark = action.remark,
                    overrides = current.overrides.map { it.copy(remark = action.remark) },
                )
            }

            EditTimeSlotAction.RequestSave -> requestSave()
            is EditTimeSlotAction.ConfirmGroupSync -> saveCurrent(syncGroup = action.sync)
            EditTimeSlotAction.Delete -> deleteTimeSlot()
        }
    }

    private inline fun updateSuccessState(crossinline transform: (TimeSlotUi) -> TimeSlotUi) {
        val current = _uiState.value
        if (current is UiState.Success) {
            _uiState.value = UiState.Success(transform(current.data))
        }
    }

    private fun requestSave() {
        val currentUi = (_uiState.value as? UiState.Success)?.data ?: return
        val original = originalSlot
        val timesChanged = original != null &&
            (original.startTime != currentUi.startTime || original.endTime != currentUi.endTime)
        val groupId = currentUi.batchGroupId

        if (currentUi.id != 0L && timesChanged && !groupId.isNullOrBlank()) {
            val courseId = cId ?: return
            viewModelScope.launch {
                val siblings = repository.getCourseById(courseId).first()?.timeSlots.orEmpty()
                    .count { it.batchGroupId == groupId && it.id != currentUi.id }
                if (siblings > 0) {
                    _groupSyncPrompt.value = siblings
                } else {
                    saveCurrent(syncGroup = false)
                }
            }
        } else {
            saveCurrent(syncGroup = false)
        }
    }

    private fun saveCurrent(syncGroup: Boolean) {
        _groupSyncPrompt.value = null
        viewModelScope.launch {
            try {
                val currentUi = (_uiState.value as? UiState.Success)?.data
                    ?: throw AppError.UnexpectedEmpty()
                val courseId = cId ?: throw AppError.InvalidParameter(NavArgs.COURSE_ID)
                val current = currentUi.toTimeSlot()

                if (currentUi.selectedDates.size > 1) {
                    // A multi-date selection always materializes as independent rows. Existing
                    // lessons can also be expanded with additional dates; once that happens the
                    // rows share only batchGroupId, so each date remains independently editable.
                    val groupId = current.batchGroupId ?: UUID.randomUUID().toString()
                    val ordered = currentUi.selectedDates.sorted()
                    val courseSlots = repository.getCourseById(courseId).first()?.timeSlots.orEmpty()
                    val siblingDates = courseSlots
                        .asSequence()
                        .filter { it.id != current.id && it.batchGroupId == groupId }
                        .mapNotNull { it.exactExtraDate() }
                        .toSet()

                    if (current.id == 0L) {
                        ordered.forEach { date ->
                            repository.upsertTimeSlot(current.forExactDate(date, groupId, forceNewId = true), courseId)
                        }
                    } else {
                        val originalDate = originalSlot?.exactExtraDate()
                        val retainedDate = originalDate
                            ?.takeIf { it in ordered && it !in siblingDates }
                            ?: ordered.firstOrNull { it !in siblingDates }
                            ?: ordered.first()

                        repository.upsertTimeSlot(
                            current.forExactDate(retainedDate, groupId, forceNewId = false),
                            courseId,
                        )
                        ordered
                            .asSequence()
                            .filter { it != retainedDate && it !in siblingDates }
                            .forEach { date ->
                                repository.upsertTimeSlot(
                                    current.forExactDate(date, groupId, forceNewId = true),
                                    courseId,
                                )
                            }
                    }
                } else if (currentUi.selectedDates.isNotEmpty()) {
                    val date = currentUi.selectedDates.sorted().first()
                    repository.upsertTimeSlot(
                        current.forExactDate(date, current.batchGroupId, forceNewId = false),
                        courseId,
                    )
                } else {
                    repository.upsertTimeSlot(current, courseId)
                }

                if (syncGroup && !current.batchGroupId.isNullOrBlank()) {
                    val siblings = repository.getCourseById(courseId).first()?.timeSlots.orEmpty()
                        .filter { it.batchGroupId == current.batchGroupId && it.id != current.id }
                    siblings.forEach { sibling ->
                        repository.upsertTimeSlot(
                            sibling.copy(
                                startTime = current.startTime,
                                endTime = current.endTime,
                                overrides = sibling.overrides.withTimes(current.startTime, current.endTime),
                            ),
                            courseId,
                        )
                    }
                }

                WearSurfaceRefresher.refresh(appContext)
                _completed.tryEmit(Unit)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e)
            }
        }
    }

    private fun deleteTimeSlot() {
        viewModelScope.launch {
            try {
                val slotId = (_uiState.value as? UiState.Success)?.data?.id
                    ?: throw AppError.TimeSlotNotFound(null)
                if (slotId != 0L) {
                    repository.deleteTimeSlot(slotId)
                    WearSurfaceRefresher.refresh(appContext)
                }
                _completed.tryEmit(Unit)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e)
            }
        }
    }
}

private fun TimeSlot.exactExtraDate(): kotlinx.datetime.LocalDate? =
    overrides.firstOrNull { it.endDate == null && it.type == ScheduleOverrideType.EXTRA }?.date

private fun TimeSlot.forExactDate(
    date: kotlinx.datetime.LocalDate,
    groupId: String?,
    forceNewId: Boolean,
): TimeSlot = copy(
    id = if (forceNewId) 0L else id,
    dayOfWeek = date.dayOfWeek,
    recurrence = WeekPattern.DATE_ONLY,
    batchGroupId = groupId,
    overrides = listOf(
        ScheduleOverride(
            date = date,
            type = ScheduleOverrideType.EXTRA,
            startTime = startTime,
            endTime = endTime,
            remark = remark,
        )
    ),
)

private fun List<ScheduleOverride>.withTimes(
    start: kotlinx.datetime.LocalTime?,
    end: kotlinx.datetime.LocalTime?,
): List<ScheduleOverride> = map { override ->
    if (override.type == ScheduleOverrideType.CANCELLED) override
    else override.copy(startTime = start, endTime = end)
}

private fun plusMinutes(time: kotlinx.datetime.LocalTime, minutes: Int): kotlinx.datetime.LocalTime {
    val total = (time.hour * 60 + time.minute + minutes).coerceAtMost(1439)
    return kotlinx.datetime.LocalTime(total / 60, total % 60)
}

private fun minusMinutes(time: kotlinx.datetime.LocalTime, minutes: Int): kotlinx.datetime.LocalTime {
    val total = (time.hour * 60 + time.minute - minutes).coerceAtLeast(0)
    return kotlinx.datetime.LocalTime(total / 60, total % 60)
}

private fun SavedStateHandle.longArg(key: String): Long? {
    val value: Any? = get<Any?>(key)
    return when (value) {
        is Long -> value
        is Int -> value.toLong()
        is String -> value.toLongOrNull()?.takeIf { it >= 0L }
        else -> null
    }
}
