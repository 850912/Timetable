package com.hufeng943.timetable.presentation.viewmodel.edit.timeslot

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.presentation.ui.NavArgs
import com.hufeng943.timetable.presentation.ui.common.ui.mappers.toCourseUi
import com.hufeng943.timetable.presentation.viewmodel.AppError
import com.hufeng943.timetable.presentation.viewmodel.UiState
import com.hufeng943.timetable.presentation.viewmodel.toSafeStateFlow
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.model.ScheduleOverrideType
import com.hufeng943.timetable.shared.model.WeekPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TimeSlotListViewModel @Inject constructor(
    private val repository: TimetableRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val cId: Long? = savedStateHandle.get<String>(NavArgs.COURSE_ID)?.toLongOrNull()

    init {
        // 3.4: the old multi-date tool stored several dates inside one DATE_ONLY TimeSlot.
        // Split those legacy records lazily when the course is opened so every date becomes
        // an independent editable lesson while still retaining a shared batchGroupId.
        viewModelScope.launch {
            val courseId = cId ?: return@launch
            val course = repository.getCourseById(courseId).first() ?: return@launch
            course.timeSlots.forEach { slot ->
                val exactOverrides = slot.overrides
                    .filter { it.endDate == null }
                    .filter { it.type == ScheduleOverrideType.EXTRA || it.type == ScheduleOverrideType.CANCELLED }
                val dates = exactOverrides.map { it.date }.distinct().sorted()
                val isLegacyMergedDateSlot =
                    slot.recurrence == WeekPattern.DATE_ONLY &&
                        dates.size > 1 &&
                        slot.overrides.all { it.endDate == null }

                if (!isLegacyMergedDateSlot) return@forEach

                val groupId = slot.batchGroupId ?: UUID.randomUUID().toString()
                dates.forEachIndexed { index, date ->
                    val dateOverrides = slot.overrides.filter { it.date == date }
                    repository.upsertTimeSlot(
                        slot.copy(
                            id = if (index == 0) slot.id else 0L,
                            dayOfWeek = date.dayOfWeek,
                            recurrence = WeekPattern.DATE_ONLY,
                            overrides = dateOverrides,
                            batchGroupId = groupId,
                        ),
                        courseId,
                    )
                }
            }
        }
    }

    val uiState = flow {
        val id = cId ?: throw AppError.InvalidParameter(NavArgs.COURSE_ID)
        emitAll(repository.getCourseById(id))
    }.map { course ->
        if (course == null) {
            throw AppError.CourseNotFound(cId)
        } else {
            UiState.Success(course.toCourseUi())
        }
    }.toSafeStateFlow(viewModelScope)
}
