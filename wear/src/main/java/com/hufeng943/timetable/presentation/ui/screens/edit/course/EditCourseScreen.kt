package com.hufeng943.timetable.presentation.ui.screens.edit.course

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.common.DynamicSubTheme
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.HandleEditUiState
import com.hufeng943.timetable.presentation.ui.screens.common.ColorSelectionScreen
import com.hufeng943.timetable.presentation.ui.screens.common.DeleteConfirmScreen
import com.hufeng943.timetable.presentation.ui.screens.common.TextEditScreen
import com.hufeng943.timetable.presentation.viewmodel.edit.course.EditCourseAction
import com.hufeng943.timetable.presentation.viewmodel.edit.course.EditCourseViewModel

@Composable
fun EditCourseScreen(
    viewModel: EditCourseViewModel = hiltViewModel()
) {
    EditCourseMainScreen(viewModel)
}

@Composable
fun EditCourseMainScreen(viewModel: EditCourseViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = LocalNavController.current

    HandleEditUiState(uiState) { course ->
        DynamicSubTheme(seedColor = course.color) {
            EditCourseMainPager(
                course = course,
                onSave = {
                    viewModel.onAction(EditCourseAction.Upsert)
                    navController.popSafe()
                },
                onNameClick = {
                    navController.navigateSingle(NavRoutes.EDIT_COURSE_NAME)
                },
                onLocationClick = {
                    navController.navigateSingle(NavRoutes.EDIT_COURSE_LOCATION)
                },
                onLocationLongClick = {
                    viewModel.onAction(EditCourseAction.UpdateLocation())
                },
                onTeacherClick = {
                    navController.navigateSingle(NavRoutes.EDIT_COURSE_TEACHER)
                },
                onTeacherLongClick = {
                    viewModel.onAction(EditCourseAction.UpdateTeacher())
                },
                onColorClick = {
                    navController.navigateSingle(NavRoutes.EDIT_COURSE_COLOR)
                },
                onColorLongClick = {
                    viewModel.onAction(EditCourseAction.UpdateColor())
                },
                onDelete = {
                    navController.navigateSingle(NavRoutes.EDIT_COURSE_DELETE_CONFIRM)
                }
            )
        }
    }
}

@Composable
fun EditCourseNameScreen(viewModel: EditCourseViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = LocalNavController.current

    HandleEditUiState(uiState) { course ->
        DynamicSubTheme(seedColor = course.color) {
            TextEditScreen(
                label = stringResource(R.string.edit_course_name_hint),
                initialText = course.name
            ) {
                viewModel.onAction(EditCourseAction.UpdateName(it))
                navController.popSafe()
            }
        }
    }
}

@Composable
fun EditCourseLocationScreen(viewModel: EditCourseViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = LocalNavController.current

    HandleEditUiState(uiState) { course ->
        DynamicSubTheme(seedColor = course.color) {
            TextEditScreen(
                label = stringResource(R.string.edit_course_location_hint),
                initialText = course.location ?: ""
            ) {
                viewModel.onAction(
                    EditCourseAction.UpdateLocation(it.ifBlank { null })
                )
                navController.popSafe()
            }
        }
    }
}

@Composable
fun EditCourseTeacherScreen(viewModel: EditCourseViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = LocalNavController.current

    HandleEditUiState(uiState) { course ->
        DynamicSubTheme(seedColor = course.color) {
            TextEditScreen(
                label = stringResource(R.string.edit_course_teacher_hint),
                initialText = course.teacher ?: ""
            ) {
                viewModel.onAction(
                    EditCourseAction.UpdateTeacher(it.ifBlank { null })
                )
                navController.popSafe()
            }
        }
    }
}

@Composable
fun EditCourseColorScreen(viewModel: EditCourseViewModel) {
    val navController = LocalNavController.current

    ColorSelectionScreen { color ->
        viewModel.onAction(EditCourseAction.UpdateColor(color))
        navController.popSafe()
    }
}

@Composable
fun EditCourseDeleteConfirmScreen(viewModel: EditCourseViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = LocalNavController.current

    HandleEditUiState(uiState) { course ->
        DynamicSubTheme(seedColor = course.color) {
            DeleteConfirmScreen(
                detail = stringResource(
                    R.string.edit_course_display_name,
                    course.displayName
                ),
                onConfirm = {
                    viewModel.onAction(EditCourseAction.Delete)
                    navController.popBackStack()
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popSafe()
                }
            )
        }
    }
}
