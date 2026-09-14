package com.hufeng943.timetable.presentation.viewmodel.edit.tools

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.presentation.ui.NavArgs
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.model.Course
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
import javax.inject.Inject

data class MultiDateSlotData(
    val course: Course,
    val timetable: Timetable,
)

sealed interface MultiDateSlotState {
    data object Loading : MultiDateSlotState
    data class Ready(val data: MultiDateSlotData) : MultiDateSlotState
    data class Error(val message: String) : MultiDateSlotState
}

@HiltViewModel
class MultiDateSlotViewModel @Inject constructor(
    private val repository: TimetableRepository,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val courseId = savedStateHandle.longArg(NavArgs.COURSE_ID)

    private val _state = MutableStateFlow<MultiDateSlotState>(MultiDateSlotState.Loading)
    val state: StateFlow<MultiDateSlotState> = _state.asStateFlow()

    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed = _completed.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val id = requireNotNull(courseId) { "缺少课程参数" }
                val course = requireNotNull(repository.getCourseById(id).first()) { "课程不存在" }
                val timetable = requireNotNull(repository.getTimetableByCourseId(id).first()) { "课表不存在" }
                MultiDateSlotData(course, timetable)
            }.onSuccess { _state.value = MultiDateSlotState.Ready(it) }
                .onFailure { _state.value = MultiDateSlotState.Error(it.message ?: "加载失败") }
        }
    }

    fun create(dates: Collection<LocalDate>, startTime: LocalTime, endTime: LocalTime) {
        val id = courseId ?: return
        viewModelScope.launch {
            runCatching {
                repository.upsertTimeSlot(
                    ScheduleBatchOperations.dateOnlySlot(dates, startTime, endTime),
                    id,
                )
                WearSurfaceRefresher.refresh(appContext)
                _completed.tryEmit(Unit)
            }.onFailure { _state.value = MultiDateSlotState.Error(it.message ?: "创建失败") }
        }
    }
}

private fun SavedStateHandle.longArg(key: String): Long? = when (val value = get<Any?>(key)) {
    is Long -> value
    is Int -> value.toLong()
    is String -> value.toLongOrNull()
    else -> null
}
