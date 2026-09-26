package com.hufeng943.timetable.presentation.ui.screens.edit.timeslot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.TimePickerType
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.common.DynamicSubTheme
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.HandleEditUiState
import com.hufeng943.timetable.presentation.ui.components.WearTimePickerPage
import com.hufeng943.timetable.presentation.ui.screens.common.DayOfWeekSelectionScreen
import com.hufeng943.timetable.presentation.ui.screens.common.DeleteConfirmScreen
import com.hufeng943.timetable.presentation.ui.screens.common.RecurrenceSelectionScreen
import com.hufeng943.timetable.presentation.ui.screens.common.TextEditScreen
import com.hufeng943.timetable.presentation.viewmodel.edit.timeslot.EditTimeSlotAction
import com.hufeng943.timetable.presentation.viewmodel.edit.timeslot.EditTimeSlotViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
private fun TimeSlotCompletionEffect(viewModel: EditTimeSlotViewModel) {
    val nav = LocalNavController.current
    LaunchedEffect(viewModel) {
        viewModel.completed.collect {
            nav.popBackStack(NavRoutes.EDIT_TIMESLOT, inclusive = true)
        }
    }
}

/** Main destination of the time-slot editor graph. */
@Composable
fun EditTimeSlotScreen(viewModel: EditTimeSlotViewModel) {
    TimeSlotCompletionEffect(viewModel)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupSyncPrompt by viewModel.groupSyncPrompt.collectAsStateWithLifecycle()
    val nav = LocalNavController.current

    HandleEditUiState(uiState) { timeSlot ->
        DynamicSubTheme(seedColor = timeSlot.color) {
            val siblingCount = groupSyncPrompt
            if (siblingCount != null) {
                GroupSyncConfirmScreen(
                    siblingCount = siblingCount,
                    onSync = { viewModel.onAction(EditTimeSlotAction.ConfirmGroupSync(true)) },
                    onCurrentOnly = { viewModel.onAction(EditTimeSlotAction.ConfirmGroupSync(false)) },
                )
            } else {
                EditTimeSlotMainPager(
                    timeSlot = timeSlot,
                    onSave = { viewModel.onAction(EditTimeSlotAction.RequestSave) },
                    onStartTimeClick = { nav.navigateSingle(NavRoutes.EDIT_TIMESLOT_START_TIME) },
                    onEndTimeClick = { nav.navigateSingle(NavRoutes.EDIT_TIMESLOT_END_TIME) },
                    onDayOfWeekClick = { nav.navigateSingle(NavRoutes.EDIT_TIMESLOT_DATES) },
                    onRecurrenceClick = { nav.navigateSingle(NavRoutes.EDIT_TIMESLOT_RECURRENCE) },
                    onRemarkClick = { nav.navigateSingle(NavRoutes.EDIT_TIMESLOT_REMARK) },
                    onRemarkLongClick = { viewModel.onAction(EditTimeSlotAction.UpdateRemark(null)) },
                    onDelete = { nav.navigateSingle(NavRoutes.EDIT_TIMESLOT_DELETE_CONFIRM) },
                )
            }
        }
    }
}

@Composable
fun EditTimeSlotStartTimeScreen(viewModel: EditTimeSlotViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    val config = LocalAppConfig.current
    HandleEditUiState(uiState) { timeSlot ->
        DynamicSubTheme(seedColor = timeSlot.color) {
            WearTimePickerPage(
                initialTime = (timeSlot.startTime ?: Clock.System.now().toLocalDateTime(
                    TimeZone.currentSystemDefault(),
                ).time).toJavaLocalTime(),
                onTimePicked = { newTime ->
                    viewModel.onAction(EditTimeSlotAction.UpdateStartTime(newTime.toKotlinLocalTime()))
                    nav.popSafe()
                },
                timePickerType = if (config.is24HourFormat) {
                    TimePickerType.HoursMinutes24H
                } else {
                    TimePickerType.HoursMinutesAmPm12H
                },
            )
        }
    }
}

@Composable
fun EditTimeSlotEndTimeScreen(viewModel: EditTimeSlotViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    val config = LocalAppConfig.current
    HandleEditUiState(uiState) { timeSlot ->
        DynamicSubTheme(seedColor = timeSlot.color) {
            WearTimePickerPage(
                initialTime = (timeSlot.endTime ?: Clock.System.now().toLocalDateTime(
                    TimeZone.currentSystemDefault(),
                ).time).toJavaLocalTime(),
                onTimePicked = { newTime ->
                    viewModel.onAction(EditTimeSlotAction.UpdateEndTime(newTime.toKotlinLocalTime()))
                    nav.popSafe()
                },
                timePickerType = if (config.is24HourFormat) {
                    TimePickerType.HoursMinutes24H
                } else {
                    TimePickerType.HoursMinutesAmPm12H
                },
            )
        }
    }
}

@Composable
fun EditTimeSlotDatesScreen(viewModel: EditTimeSlotViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    HandleEditUiState(uiState) { timeSlot ->
        DynamicSubTheme(seedColor = timeSlot.color) {
            MultiDateSelectionScreen(
                initialDays = timeSlot.selectedDays,
                onConfirm = { days ->
                    viewModel.onAction(EditTimeSlotAction.UpdateDays(days))
                    nav.popSafe()
                },
            )
        }
    }
}

@Composable
fun EditTimeSlotWeekDayScreen(viewModel: EditTimeSlotViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    HandleEditUiState(uiState) { timeSlot ->
        DynamicSubTheme(seedColor = timeSlot.color) {
            DayOfWeekSelectionScreen(
                initialDay = timeSlot.dayOfWeek ?: Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date.dayOfWeek,
                onDaySelected = { day ->
                    viewModel.onAction(EditTimeSlotAction.UpdateDayOfWeek(day))
                    nav.popSafe()
                },
            )
        }
    }
}

@Composable
fun EditTimeSlotRecurrenceScreen(viewModel: EditTimeSlotViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    HandleEditUiState(uiState) { timeSlot ->
        DynamicSubTheme(seedColor = timeSlot.color) {
            RecurrenceSelectionScreen(
                initialPattern = timeSlot.recurrence,
                onPatternSelected = { pattern ->
                    viewModel.onAction(EditTimeSlotAction.UpdateRecurrence(pattern))
                    nav.popSafe()
                },
            )
        }
    }
}

@Composable
fun EditTimeSlotRemarkScreen(viewModel: EditTimeSlotViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    HandleEditUiState(uiState) { timeSlot ->
        DynamicSubTheme(seedColor = timeSlot.color) {
            TextEditScreen(
                label = stringResource(R.string.edit_timeslot_remark_hint),
                initialText = timeSlot.remark ?: "",
            ) { newRemark ->
                viewModel.onAction(EditTimeSlotAction.UpdateRemark(newRemark.ifBlank { null }))
                nav.popSafe()
            }
        }
    }
}

@Composable
fun EditTimeSlotDeleteConfirmScreen(viewModel: EditTimeSlotViewModel) {
    TimeSlotCompletionEffect(viewModel)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    HandleEditUiState(uiState) { timeSlot ->
        DynamicSubTheme(seedColor = timeSlot.color) {
            DeleteConfirmScreen(
                detail = stringResource(
                    R.string.edit_timeslot_display_name,
                    timeSlot.startTime ?: stringResource(R.string.unknown),
                    timeSlot.endTime ?: stringResource(R.string.unknown),
                ),
                onConfirm = { viewModel.onAction(EditTimeSlotAction.Delete) },
                onCancel = { nav.popSafe() },
            )
        }
    }
}
