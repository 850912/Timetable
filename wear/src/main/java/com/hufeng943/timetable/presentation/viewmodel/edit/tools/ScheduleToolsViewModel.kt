package com.hufeng943.timetable.presentation.viewmodel.edit.tools

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.data.repository.TimeSlotMutation
import com.hufeng943.timetable.shared.model.OpenEndedBatchAction
import com.hufeng943.timetable.shared.model.ScheduleBatchOperations
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.surface.WearSurfaceRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import javax.inject.Inject

enum class BatchAction { SHIFT, CANCEL, RESTORE }

data class ScheduleToolsEditorState(
    val start: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val end: LocalDate? = null,
    val offsetMinutes: Int = 10,
    val useWindow: Boolean = false,
    val windowStart: LocalTime = LocalTime(8, 0),
    val windowEnd: LocalTime = LocalTime(18, 0),
    val action: BatchAction = BatchAction.SHIFT,
)

sealed interface ScheduleToolsState {
    data object Loading : ScheduleToolsState
    data class Ready(val timetables: List<Timetable>) : ScheduleToolsState
    data class Error(val message: String) : ScheduleToolsState
}

@HiltViewModel
class ScheduleToolsViewModel @Inject constructor(
    private val repository: TimetableRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _state = MutableStateFlow<ScheduleToolsState>(ScheduleToolsState.Loading)
    val state: StateFlow<ScheduleToolsState> = _state.asStateFlow()

    private val _editor = MutableStateFlow(ScheduleToolsEditorState())
    val editor: StateFlow<ScheduleToolsEditorState> = _editor.asStateFlow()

    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed = _completed.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getAllTimetables().collect { tables ->
                _state.value = if (tables.isEmpty()) ScheduleToolsState.Error("暂无课表")
                else ScheduleToolsState.Ready(tables)
            }
        }
    }

    fun updateStart(date: LocalDate) {
        _editor.value = _editor.value.let { current ->
            current.copy(start = date, end = current.end?.let { if (it < date) date else it })
        }
    }

    fun updateEnd(date: LocalDate?) {
        _editor.value = _editor.value.copy(end = date)
    }

    fun updateOffset(minutes: Int) {
        if (minutes in -30..30 && minutes != 0) _editor.value = _editor.value.copy(offsetMinutes = minutes)
    }

    fun toggleWindow() {
        _editor.value = _editor.value.copy(useWindow = !_editor.value.useWindow)
    }

    fun enableWindow() {
        _editor.value = _editor.value.copy(useWindow = true)
    }

    fun updateWindowStart(time: LocalTime) {
        _editor.value = _editor.value.copy(windowStart = time)
    }

    fun updateWindowEnd(time: LocalTime) {
        _editor.value = _editor.value.copy(windowEnd = time)
    }

    fun cycleAction() {
        val current = _editor.value.action
        _editor.value = _editor.value.copy(
            action = BatchAction.entries[(current.ordinal + 1) % BatchAction.entries.size],
        )
    }

    fun applyEditor() {
        val e = _editor.value
        apply(
            action = e.action,
            startDate = e.start,
            endDate = e.end,
            offsetMinutes = e.offsetMinutes,
            timetableId = null,
            courseId = null,
            timeWindowStart = if (e.useWindow) e.windowStart else null,
            timeWindowEnd = if (e.useWindow) e.windowEnd else null,
        )
    }

    fun apply(
        action: BatchAction,
        startDate: LocalDate,
        endDate: LocalDate?,
        offsetMinutes: Int,
        timetableId: Long?,
        courseId: Long?,
        timeWindowStart: LocalTime?,
        timeWindowEnd: LocalTime?,
    ) {
        viewModelScope.launch {
            runCatching {
                if (action == BatchAction.SHIFT) require(offsetMinutes in -30..30 && offsetMinutes != 0) { "提前/延时必须为 1–30 分钟" }
                val all = repository.getAllTimetables().first()
                // Batch tools are intentionally global: one confirmation applies to every
                // timetable, every course and every time slot in the selected date/time range.
                val mutations = all.flatMap { timetable ->
                    buildMutationsForTimetable(
                        timetable = timetable,
                        action = action,
                        startDate = startDate,
                        endDate = endDate,
                        offsetMinutes = offsetMinutes,
                        timeWindowStart = timeWindowStart,
                        timeWindowEnd = timeWindowEnd,
                    )
                }
                // One global confirmation is committed in one Room transaction. This prevents a
                // later timetable failure from leaving earlier timetables partially modified.
                repository.applyTimeSlotMutations(mutations)
                WearSurfaceRefresher.refresh(appContext)
                _completed.tryEmit(Unit)
            }.onFailure { _state.value = ScheduleToolsState.Error(it.message ?: "批量处理失败") }
        }
    }

    private fun buildMutationsForTimetable(
        timetable: Timetable,
        action: BatchAction,
        startDate: LocalDate,
        endDate: LocalDate?,
        offsetMinutes: Int,
        timeWindowStart: LocalTime?,
        timeWindowEnd: LocalTime?,
    ): List<TimeSlotMutation> {
        val mutations = mutableListOf<TimeSlotMutation>()
        if (endDate == null) {
            timetable.allCourses
                .asSequence()
                .forEach { course ->
                    course.timeSlots.forEach slotLoop@ { slot ->
                        if (!ScheduleBatchOperations.overlapsWindow(slot, timeWindowStart, timeWindowEnd)) return@slotLoop
                        val updated = ScheduleBatchOperations.applyOpenEnded(
                            slot = slot,
                            startDate = startDate,
                            action = when (action) {
                                BatchAction.SHIFT -> OpenEndedBatchAction.SHIFT
                                BatchAction.CANCEL -> OpenEndedBatchAction.CANCEL
                                BatchAction.RESTORE -> OpenEndedBatchAction.RESTORE
                            },
                            offsetMinutes = offsetMinutes,
                        )
                        if (updated != slot) mutations += TimeSlotMutation(updated, course.id)
                    }
                }
        } else {
            val matching = if (action == BatchAction.RESTORE) emptyMap() else {
                ScheduleBatchOperations.matchingDates(
                    timetable, startDate, endDate, timeWindowStart, timeWindowEnd,
                )
            }
            timetable.allCourses
                .asSequence()
                .forEach { course ->
                    course.timeSlots.forEach slotLoop@ { slot ->
                        val updated = if (action == BatchAction.RESTORE) {
                            ScheduleBatchOperations.clearRange(slot, startDate, endDate, timeWindowStart, timeWindowEnd)
                        } else {
                            val dates = matching[slot.id].orEmpty()
                            if (dates.isEmpty()) return@slotLoop
                            when (action) {
                                BatchAction.SHIFT -> ScheduleBatchOperations.shiftDates(slot, dates, offsetMinutes)
                                BatchAction.CANCEL -> ScheduleBatchOperations.cancelDates(slot, dates)
                                BatchAction.RESTORE -> slot
                            }
                        }
                        if (updated != slot) mutations += TimeSlotMutation(updated, course.id)
                    }
                }
        }
        return mutations
    }
}
