package com.hufeng943.timetable.presentation.ui.screens.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.School
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.UiState
import com.hufeng943.timetable.presentation.viewmodel.home.TimetableViewModel

/**
 * Read-only, one-glance view of today's complete schedule.
 *
 * It intentionally has no course-edit gestures: the main timetable remains the interaction-heavy
 * surface while this page prioritizes large text, dense information and low interaction cost.
 */
@Composable
fun QuickViewPager(viewModel: TimetableViewModel = hiltViewModel()) {
    val uiState by viewModel.todayCoursesUi.collectAsStateWithLifecycle()
    val nextState by viewModel.nextCourseState.collectAsStateWithLifecycle()
    val config = LocalAppConfig.current
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()

    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text(stringResource(R.string.quick_view_title)) }
            }

            when (val current = uiState) {
                is UiState.Success -> {
                    val courses = current.data
                    if (courses.isEmpty()) {
                        item {
                            OneUiCapsuleSurface(
                                title = stringResource(R.string.home_no_course_today),
                                subtitle = nextState.next?.let { occurrence ->
                                    stringResource(
                                        R.string.quick_view_next_summary,
                                        occurrence.courseName,
                                        occurrence.startTime.toDisplayString(config.is24HourFormat),
                                    )
                                },
                                icon = Icons.Rounded.EventAvailable,
                                modifier = Modifier.fillMaxWidth()
                                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                            )
                        }
                    } else {
                        item {
                            OneUiCapsuleSurface(
                                title = stringResource(R.string.quick_view_count, courses.size),
                                subtitle = stringResource(R.string.quick_view_hint),
                                icon = Icons.Rounded.EventAvailable,
                                modifier = Modifier.fillMaxWidth()
                                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                            )
                        }
                        items(courses, key = { it.timeSlot.id }) { course ->
                            val slot = course.timeSlot
                            val time = listOfNotNull(
                                slot.startTime?.toDisplayString(config.is24HourFormat),
                                slot.endTime?.toDisplayString(config.is24HourFormat),
                            ).joinToString("–")
                            val place = course.location.orEmpty().trim()
                            val subtitle = if (place.isBlank()) time else "$time · $place"
                            val isCurrent = nextState.current?.timeSlotId == slot.id
                            val isNext = nextState.next?.takeIf { it.date == nextState.today }?.timeSlotId == slot.id
                            OneUiCapsuleSurface(
                                title = course.name.ifBlank { stringResource(R.string.default_course_name) },
                                subtitle = subtitle,
                                icon = Icons.Rounded.School,
                                selected = isNext,
                                emphasize = isCurrent,
                                accentColor = course.displayColor,
                                modifier = Modifier.fillMaxWidth()
                                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                            )
                        }
                    }
                }
                is UiState.Empty -> item {
                    OneUiCapsuleSurface(
                        title = stringResource(R.string.home_no_course_today),
                        icon = Icons.Rounded.EventAvailable,
                        modifier = Modifier.fillMaxWidth()
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    )
                }
                is UiState.Error -> item {
                    OneUiCapsuleSurface(
                        title = stringResource(R.string.quick_view_unavailable),
                        icon = Icons.Rounded.EventAvailable,
                        modifier = Modifier.fillMaxWidth()
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    )
                }
                is UiState.Loading -> Unit
            }
        }
    }
}
