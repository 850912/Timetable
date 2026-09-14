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
import com.hufeng943.timetable.surface.WearSurfaceRefresher
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.ScheduleOverride
import com.hufeng943.timetable.shared.model.ScheduleOverrideType
import com.hufeng943.timetable.shared.model.WeekPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTimeSlotViewModel @Inject constructor(
    private val repository: TimetableRepository,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 使用 get<Long>() 直接获取 Long 类型参数（更简洁、类型安全）
    private val cId: Long? = savedStateHandle.longArg(NavArgs.COURSE_ID)
    private val sId: Long? = savedStateHandle.longArg(NavArgs.TIME_SLOT_ID)

    private val _uiState = MutableStateFlow<UiState<TimeSlotUi>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed = _completed.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                val domainData =
                    sId?.let { id ->
                        repository.getTimeSlotById(id).first() ?: TimeSlot()
                    } ?: TimeSlot()
                val courseUi = cId?.let { id ->
                    repository.getCourseById(id).first()
                } ?: throw AppError.CourseNotFound(cId)
                _uiState.value = UiState.Success(domainData.toTimeSlotUi(courseUi.color))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e)
            }
        }
    }

    fun onAction(action: EditTimeSlotAction) {
        when (action) {
            is EditTimeSlotAction.UpdateStartTime -> updateSuccessState { current ->
                val newStart = action.startTime
                val newEnd =
                    current.endTime?.let { if (newStart > it) newStart else current.endTime }
                current.copy(startTime = newStart, endTime = newEnd)
            }

            is EditTimeSlotAction.UpdateEndTime -> updateSuccessState { current ->
                val newEnd = action.endTime
                val newStart =
                    current.startTime?.let { if (newEnd < it) newEnd else current.startTime }
                current.copy(startTime = newStart, endTime = newEnd)
            }

            is EditTimeSlotAction.UpdateDayOfWeek -> updateSuccessState { it.copy(dayOfWeek = action.dayOfWeek) }
            is EditTimeSlotAction.UpdateRecurrence -> updateSuccessState { current ->
                current.copy(
                    recurrence = action.recurrence,
                    overrides = if (current.id == 0L && action.recurrence != WeekPattern.DATE_ONLY) emptyList() else current.overrides,
                )
            }
            is EditTimeSlotAction.UpdateSelectedDates -> updateSuccessState { current ->
                val dates = action.dates.toList().sorted()
                if (dates.isEmpty()) {
                    current.copy(
                        recurrence = if (current.recurrence == WeekPattern.DATE_ONLY) WeekPattern.EVERY_WEEK else current.recurrence,
                        overrides = if (current.id == 0L) emptyList() else current.overrides,
                    )
                } else {
                    val start = current.startTime
                    val end = current.endTime
                    current.copy(
                        dayOfWeek = dates.first().dayOfWeek,
                        recurrence = WeekPattern.DATE_ONLY,
                        overrides = dates.map { date ->
                            ScheduleOverride(
                                date = date,
                                type = ScheduleOverrideType.EXTRA,
                                startTime = start,
                                endTime = end,
                                remark = current.remark,
                            )
                        },
                    )
                }
            }
            is EditTimeSlotAction.UpdateRemark -> updateSuccessState { it.copy(remark = action.remark) }
            EditTimeSlotAction.Upsert -> upsertTimeSlot()
            EditTimeSlotAction.Delete -> deleteTimeSlot()
        }
    }

    private inline fun updateSuccessState(crossinline transform: (TimeSlotUi) -> TimeSlotUi) {
        val current = _uiState.value
        if (current is UiState.Success) {
            _uiState.value = UiState.Success(transform(current.data))
        }
    }

    private fun upsertTimeSlot() {
        viewModelScope.launch {
            try {
                val currentUi =
                    (uiState.value as? UiState.Success)?.data ?: throw AppError.UnexpectedEmpty()
                val courseId = cId ?: throw AppError.InvalidParameter(NavArgs.COURSE_ID)

                val normalized = if (currentUi.recurrence == WeekPattern.DATE_ONLY && currentUi.selectedDates.isNotEmpty()) {
                    currentUi.toTimeSlot().copy(
                        dayOfWeek = currentUi.selectedDates.minOrNull()!!.dayOfWeek,
                        overrides = currentUi.selectedDates.sorted().map { date ->
                            ScheduleOverride(
                                date = date,
                                type = ScheduleOverrideType.EXTRA,
                                startTime = currentUi.startTime,
                                endTime = currentUi.endTime,
                                remark = currentUi.remark,
                            )
                        },
                    )
                } else currentUi.toTimeSlot()
                repository.upsertTimeSlot(normalized, courseId)
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
                val slotId = (uiState.value as? UiState.Success)?.data?.id
                    ?: throw AppError.TimeSlotNotFound(null)
                if (slotId != 0L) {
                    repository.deleteTimeSlot(slotId)
                    WearSurfaceRefresher.refresh(appContext)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e)
            }
        }
    }
}


private fun SavedStateHandle.longArg(key: String): Long? {
    val value: Any? = get<Any?>(key)
    return when (value) {
        is Long -> value
        is Int -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
}
