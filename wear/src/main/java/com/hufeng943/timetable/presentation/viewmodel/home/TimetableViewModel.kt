package com.hufeng943.timetable.presentation.viewmodel.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.hufeng943.timetable.presentation.ui.common.ui.mappers.toCourseUi
import com.hufeng943.timetable.presentation.viewmodel.UiState
import com.hufeng943.timetable.presentation.viewmodel.toSafeStateFlow
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.hufeng943.timetable.shared.model.NextCourseEngine
import com.hufeng943.timetable.shared.model.NextCourseState
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.resolveDate
import com.hufeng943.timetable.shared.model.weekNumberFor
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@HiltViewModel
class TimetableViewModel @Inject constructor(
    repository: TimetableRepository, private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        const val KEY_SELECTED_DATE = "selected_date"
    }

    // Keep the raw domain stream so every time-sensitive surface can share NextCourseEngine.
    private val timetableList = repository.getAllTimetables().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
    )

    // 保留领域模型，按选中日期过滤后再转换为 UI，避免大课表反复转换全部课程/课时。
    private val allTimetables = timetableList.map { list ->
        if (list.isEmpty()) UiState.Empty else UiState.Success(list)
    }.toSafeStateFlow(viewModelScope)

    private val clockState = MutableStateFlow(Clock.System.now())

    init {
        viewModelScope.launch {
            while (true) {
                val nowMillis = System.currentTimeMillis()
                delay((60_000L - (nowMillis % 60_000L)).coerceAtLeast(1_000L))
                clockState.value = Clock.System.now()
            }
        }
    }

    val nextCourseState = combine(timetableList, clockState) { tables, now ->
        NextCourseEngine.resolve(
            timetables = tables,
            now = now,
            timeZone = TimeZone.currentSystemDefault(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = NextCourseState(
            phase = com.hufeng943.timetable.shared.model.NextCoursePhase.NO_SCHEDULE,
            today = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        ),
    )

    private val _selectedDate = savedStateHandle.getStateFlow<LocalDate>(
        KEY_SELECTED_DATE, Clock.System.todayIn(TimeZone.currentSystemDefault())
    )

    val selectedDate = _selectedDate

    val selectedWeekNumber = combine(allTimetables, _selectedDate) { state, date ->
        val tables = (state as? UiState.Success)?.data.orEmpty()
        tables.mapNotNull { it.weekNumberFor(date) }.minOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = null,
    )

    val selectedSemesterWeekCount = combine(allTimetables, _selectedDate) { state, date ->
        val tables = (state as? UiState.Success)?.data.orEmpty()
        tables.filter { it.weekNumberFor(date) != null }
            .mapNotNull { table -> table.semesterEnd?.let { table.weekNumberFor(it) } }
            .maxOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = null,
    )

    val selectedDateEvents = combine(allTimetables, _selectedDate) { state, date ->
        val tables = (state as? UiState.Success)?.data.orEmpty()
        tables.flatMap { it.events }
            .filter { !it.completed && it.date == date }
            .sortedWith(compareBy<com.hufeng943.timetable.shared.model.AcademicEvent> { it.time == null }.thenBy { it.time })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
    )

    // Quick View always follows the current local date and does not mutate the date selected on
    // the main timetable page. This keeps the two home surfaces independent while sharing data.
    val todayCoursesUi = combine(allTimetables, clockState) { state, now ->
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Empty -> UiState.Empty
            is UiState.Error -> UiState.Error(state.throwable)
            is UiState.Success -> {
                val sortedCoursesUi = state.data.flatMap { table -> table.toDayCoursesUi(today) }
                    .sortedBy { it.timeSlot.startTime }
                    .mapIndexed { index, course -> course.copy(dailyOrder = index + 1) }
                UiState.Success(sortedCoursesUi)
            }
        }
    }.toSafeStateFlow(viewModelScope)

    // 当前选中的课表要展示 UI 数据
    val dateCoursesUi = combine(allTimetables, _selectedDate) { state, selectedDate ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Empty -> UiState.Empty
            is UiState.Error -> UiState.Error(state.throwable)
            is UiState.Success -> {
                val allDailyCourses = state.data.flatMap { table ->
                    table.toDayCoursesUi(selectedDate)
                }
                // 只对当天结果排序和编号，复杂度随“当天课程数”而不是“全部课时数”增长。
                val sortedCoursesUi = allDailyCourses
                    .sortedBy { it.timeSlot.startTime }
                    .mapIndexed { index, course -> course.copy(dailyOrder = index + 1) }
                UiState.Success(sortedCoursesUi)
            }
        }
    }.toSafeStateFlow(viewModelScope)

    fun updateSelectedDate(date: LocalDate) {
        savedStateHandle[KEY_SELECTED_DATE] = date
    }

    fun refreshTimeState() {
        clockState.value = Clock.System.now()
    }
}


private fun Timetable.toDayCoursesUi(date: LocalDate) = resolveDate(date).map { occurrence ->
    val effectiveCourse = occurrence.course.copy(location = occurrence.location)
    val effectiveSlot = occurrence.timeSlot.copy(
        startTime = occurrence.startTime,
        endTime = occurrence.endTime,
    )
    val ui = effectiveCourse.toCourseUi(effectiveSlot)
    val tableColor = if (color == -1L) Color.Unspecified else Color(color)
    val effectiveColor = if (ui.color == Color.Unspecified) tableColor else ui.color
    ui.copy(color = effectiveColor, selectedTimeSlot = ui.timeSlot.copy(color = effectiveColor))
}
