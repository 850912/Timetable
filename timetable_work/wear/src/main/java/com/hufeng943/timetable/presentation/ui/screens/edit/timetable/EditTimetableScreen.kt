package com.hufeng943.timetable.presentation.ui.screens.edit.timetable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.common.DynamicSubTheme
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.HandleEditUiState
import com.hufeng943.timetable.presentation.ui.components.WearDatePickerPage
import com.hufeng943.timetable.presentation.ui.screens.common.ColorSelectionScreen
import com.hufeng943.timetable.presentation.ui.screens.common.DeleteConfirmScreen
import com.hufeng943.timetable.presentation.ui.screens.common.TextEditScreen
import com.hufeng943.timetable.presentation.viewmodel.edit.timetable.EditTimetableAction
import com.hufeng943.timetable.presentation.viewmodel.edit.timetable.EditTimetableViewModel
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate

/** Main destination of the timetable editor graph. */
@Composable
fun EditTimetableScreen(viewModel: EditTimetableViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavController.current

    HandleEditUiState(uiState) { timetable ->
        DynamicSubTheme(seedColor = timetable.color) {
            EditTimetableMainPager(
                timetable = timetable,
                onSave = {
                    viewModel.onAction(EditTimetableAction.Upsert)
                    nav.popSafe()
                },
                onNameClick = { nav.navigateSingle(NavRoutes.EDIT_TIMETABLE_NAME) },
                onStartDateClick = { nav.navigateSingle(NavRoutes.EDIT_TIMETABLE_START_DATE) },
                onStartDateLongClick = { viewModel.onAction(EditTimetableAction.UpdateStartDate()) },
                onEndDateClick = { nav.navigateSingle(NavRoutes.EDIT_TIMETABLE_END_DATE) },
                onEndDateLongClick = { viewModel.onAction(EditTimetableAction.UpdateEndDate()) },
                onColorClick = { nav.navigateSingle(NavRoutes.EDIT_TIMETABLE_COLOR) },
                onColorLongClick = { viewModel.onAction(EditTimetableAction.UpdateColor()) },
                onDelete = { nav.navigateSingle(NavRoutes.EDIT_TIMETABLE_DELETE_CONFIRM) },
                onQuickModify = { nav.navigateSingle(NavRoutes.SCHEDULE_TOOLS) },
                startDateIsToday = viewModel.toDay == timetable.semesterStart,
            )
        }
    }
}

@Composable
fun EditTimetableStartDateScreen(viewModel: EditTimetableViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    HandleEditUiState(uiState) { timetable ->
        DynamicSubTheme(seedColor = timetable.color) {
            WearDatePickerPage(
                initialDate = timetable.semesterStart.toJavaLocalDate(),
                onDatePicked = { newDate ->
                    viewModel.onAction(EditTimetableAction.UpdateStartDate(newDate.toKotlinLocalDate()))
                    nav.popSafe()
                },
            )
        }
    }
}

@Composable
fun EditTimetableEndDateScreen(viewModel: EditTimetableViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    HandleEditUiState(uiState) { timetable ->
        DynamicSubTheme(seedColor = timetable.color) {
            WearDatePickerPage(
                initialDate = (timetable.semesterEnd ?: timetable.semesterStart).toJavaLocalDate(),
                onDatePicked = { newDate ->
                    viewModel.onAction(EditTimetableAction.UpdateEndDate(newDate.toKotlinLocalDate()))
                    nav.popSafe()
                },
            )
        }
    }
}

@Composable
fun EditTimetableNameScreen(viewModel: EditTimetableViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    HandleEditUiState(uiState) { timetable ->
        DynamicSubTheme(seedColor = timetable.color) {
            TextEditScreen(
                label = stringResource(R.string.edit_timetable_name_hint),
                initialText = timetable.semesterName,
            ) { newValue ->
                viewModel.onAction(EditTimetableAction.UpdateName(newValue))
                nav.popSafe()
            }
        }
    }
}

@Composable
fun EditTimetableColorScreen(viewModel: EditTimetableViewModel) {
    val nav = LocalNavController.current
    ColorSelectionScreen { color ->
        viewModel.onAction(EditTimetableAction.UpdateColor(color))
        nav.popSafe()
    }
}

@Composable
fun EditTimetableDeleteConfirmScreen(viewModel: EditTimetableViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    HandleEditUiState(uiState) { timetable ->
        DynamicSubTheme(seedColor = timetable.color) {
            DeleteConfirmScreen(
                detail = stringResource(R.string.edit_timetable_display_name, timetable.displayName),
                onConfirm = {
                    viewModel.onAction(EditTimetableAction.Delete)
                    nav.popBackStack(NavRoutes.EDIT_TIMETABLE, inclusive = true)
                },
                onCancel = { nav.popSafe() },
            )
        }
    }
}
