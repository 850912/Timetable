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
import com.hufeng943.timetable.shared.model.TimeSlot
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
    val groupSyncPrompt = _groupSyncPrompt.asStateFlow()
    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed = _completed.asSharedFlow()
    private var originalSlot: TimeSlot? = null

    init {
        viewModelScope.launch {
            try {
                val domain = sId?.let { repository.getTimeSlotById(it).first() ?: TimeSlot() } ?: TimeSlot()
                originalSlot = domain.takeIf { it.id != 0L }
                val course = cId?.let { repository.getCourseById(it).first() } ?: throw AppError.CourseNotFound(cId)
                var ui = domain.toTimeSlotUi(course.color)
                if (!domain.batchGroupId.isNullOrBlank()) {
                    val days = course.timeSlots.filter { it.batchGroupId == domain.batchGroupId }.mapNotNull { it.dayOfWeek }.toSet()
                    if (days.isNotEmpty()) ui = ui.copy(selectedDays = days)
                }
                _uiState.value = UiState.Success(ui)
            } catch (e: Exception) { _uiState.value = UiState.Error(e) }
        }
    }

    fun onAction(action: EditTimeSlotAction) = when (action) {
        is EditTimeSlotAction.UpdateStartTime -> update { c ->
            val end = c.endTime?.let { if (action.startTime >= it) plusMinutes(action.startTime, 60) else it }
            c.copy(startTime = action.startTime, endTime = end)
        }
        is EditTimeSlotAction.UpdateEndTime -> update { c ->
            val start = c.startTime?.let { if (action.endTime <= it) minusMinutes(action.endTime, 60) else it }
            c.copy(startTime = start, endTime = action.endTime)
        }
        is EditTimeSlotAction.UpdateDayOfWeek -> update { it.copy(dayOfWeek = action.dayOfWeek, selectedDays = setOf(action.dayOfWeek)) }
        is EditTimeSlotAction.UpdateDays -> update { c ->
            val days = action.days
            c.copy(selectedDays = days, dayOfWeek = c.dayOfWeek?.takeIf { it in days } ?: days.firstOrNull())
        }
        is EditTimeSlotAction.UpdateRecurrence -> update { it.copy(recurrence = action.recurrence) }
        is EditTimeSlotAction.UpdateRemark -> update { it.copy(remark = action.remark) }
        EditTimeSlotAction.RequestSave -> requestSave()
        is EditTimeSlotAction.ConfirmGroupSync -> save(action.sync)
        EditTimeSlotAction.Delete -> delete()
    }

    private inline fun update(crossinline block: (TimeSlotUi) -> TimeSlotUi) {
        val s = _uiState.value
        if (s is UiState.Success) _uiState.value = UiState.Success(block(s.data))
    }

    private fun requestSave() {
        val ui = (_uiState.value as? UiState.Success)?.data ?: return
        val original = originalSlot
        val changed = original != null && (original.startTime != ui.startTime || original.endTime != ui.endTime)
        if (ui.id != 0L && changed && !ui.batchGroupId.isNullOrBlank()) {
            viewModelScope.launch {
                val siblings = repository.getCourseById(cId ?: return@launch).first()?.timeSlots.orEmpty()
                    .count { it.batchGroupId == ui.batchGroupId && it.id != ui.id }
                if (siblings > 0) _groupSyncPrompt.value = siblings else save(false)
            }
        } else save(false)
    }

    private fun save(syncGroup: Boolean) {
        _groupSyncPrompt.value = null
        viewModelScope.launch {
            try {
                val ui = (_uiState.value as? UiState.Success)?.data ?: throw AppError.UnexpectedEmpty()
                val courseId = cId ?: throw AppError.InvalidParameter(NavArgs.COURSE_ID)
                require(ui.startTime != null && ui.endTime != null && ui.selectedDays.isNotEmpty()) { "请完整设置时间和星期" }
                val base = ui.toTimeSlot()
                val existing = repository.getCourseById(courseId).first()?.timeSlots.orEmpty()
                val groupId = if (ui.selectedDays.size > 1 || !ui.batchGroupId.isNullOrBlank()) ui.batchGroupId ?: UUID.randomUUID().toString() else null
                val siblings = if (groupId == null) emptyList() else existing.filter { it.batchGroupId == groupId }
                val byDay = siblings.mapNotNull { s -> s.dayOfWeek?.let { it to s } }.toMap()
                val keepId = ui.id

                ui.selectedDays.forEach { day ->
                    val old = byDay[day]
                    val id = when {
                        old != null -> old.id
                        day == ui.dayOfWeek && keepId != 0L -> keepId
                        else -> 0L
                    }
                    val source = old ?: base
                    repository.upsertTimeSlot(source.copy(
                        id = id,
                        startTime = base.startTime,
                        endTime = base.endTime,
                        dayOfWeek = day,
                        recurrence = base.recurrence,
                        remark = base.remark,
                        batchGroupId = groupId,
                        overrides = if (old != null) old.overrides else base.overrides,
                    ), courseId)
                }
                siblings.filter { it.dayOfWeek !in ui.selectedDays }.forEach { repository.deleteTimeSlot(it.id) }

                if (syncGroup && groupId != null) {
                    repository.getCourseById(courseId).first()?.timeSlots.orEmpty()
                        .filter { it.batchGroupId == groupId }
                        .forEach { sibling ->
                            repository.upsertTimeSlot(sibling.copy(startTime = base.startTime, endTime = base.endTime), courseId)
                        }
                }
                WearSurfaceRefresher.refresh(appContext)
                _completed.tryEmit(Unit)
            } catch (e: Exception) { _uiState.value = UiState.Error(e) }
        }
    }

    private fun delete() {
        viewModelScope.launch {
            try {
                val id = (_uiState.value as? UiState.Success)?.data?.id ?: throw AppError.TimeSlotNotFound(null)
                if (id != 0L) repository.deleteTimeSlot(id)
                WearSurfaceRefresher.refresh(appContext)
                _completed.tryEmit(Unit)
            } catch (e: Exception) { _uiState.value = UiState.Error(e) }
        }
    }
}

private fun plusMinutes(time: kotlinx.datetime.LocalTime, minutes: Int): kotlinx.datetime.LocalTime {
    val total = (time.hour * 60 + time.minute + minutes).coerceAtMost(1439)
    return kotlinx.datetime.LocalTime(total / 60, total % 60)
}
private fun minusMinutes(time: kotlinx.datetime.LocalTime, minutes: Int): kotlinx.datetime.LocalTime {
    val total = (time.hour * 60 + time.minute - minutes).coerceAtLeast(0)
    return kotlinx.datetime.LocalTime(total / 60, total % 60)
}
private fun SavedStateHandle.longArg(key: String): Long? = when (val v = get<Any?>(key)) {
    is Long -> v; is Int -> v.toLong(); is String -> v.toLongOrNull(); else -> null
}
