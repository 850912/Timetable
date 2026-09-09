package com.hufeng943.timetable.presentation.ui.screens.home

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.NavRoutes.courseDetail
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.common.ui.CourseUi
import com.hufeng943.timetable.presentation.ui.components.CourseCard
import com.hufeng943.timetable.presentation.ui.components.HandleEditUiState
import com.hufeng943.timetable.presentation.ui.components.PullToDatePicker
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.PullToDatePickerState
import com.hufeng943.timetable.presentation.ui.components.pullToDatePickerDrag
import com.hufeng943.timetable.presentation.ui.components.rememberPullToDatePickerState
import com.hufeng943.timetable.presentation.ui.components.rememberPullToRefreshConnection
import com.hufeng943.timetable.presentation.viewmodel.UiState
import com.hufeng943.timetable.presentation.viewmodel.home.TimetableViewModel
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Composable
fun TimetablePager(
    viewModel: TimetableViewModel = hiltViewModel(),
    onOpenStateChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.dateCoursesUi.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val navController = LocalNavController.current

    LaunchedEffect(uiState) {
        if (uiState !is UiState.Success) {
            onOpenStateChanged(false)
        }
    }

    HandleEditUiState(
        uiState = uiState,
        emptyContent = {
            EmptyPager(
                onAddClick = {
                    navController.navigateSingle(NavRoutes.LIST_TIMETABLE)
                }
            )
        }
    ) { coursesUi ->
        val pullToDatePickerState = rememberPullToDatePickerState()
        val handleDateSelected: (LocalDate) -> Unit = remember(viewModel) {
            { date -> viewModel.updateSelectedDate(date) }
        }

        LaunchedEffect(pullToDatePickerState.dragOffset) {
            onOpenStateChanged(pullToDatePickerState.dragOffset > 0)
        }

        if (coursesUi.isEmpty()) {
            EmptyCoursePager(
                state = pullToDatePickerState,
                selectedDate = selectedDate,
                onDateSelected = handleDateSelected
            )
        } else {
            CourseListPager(
                coursesUi = coursesUi,
                state = pullToDatePickerState,
                itemKey = { courseUi -> courseUi.timeSlot.id },
                selectedDate = selectedDate,
                onDateSelected = handleDateSelected
            ) { courseUi, transformationSpec ->
                minuteTick
                val status = courseStatus(courseUi, selectedDate, coursesUi)
                CourseCard(
                    course = courseUi,
                    isCurrent = status.first,
                    isNext = status.second,
                    minutesLeft = status.third,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    navController.navigateSingle(courseDetail(courseUi.timeSlot.id))
                }
            }
        }
    }
}

/**
 * 空状态页面组件
 */
@Composable
private fun EmptyCoursePager(
    state: PullToDatePickerState,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.dragOffset) {
        if (state.dragOffset == 0f) {
            focusRequester.requestFocus()
        }
    }

    ScreenScaffold {
        PullToDatePicker(
            dragOffset = state.dragOffset,
            refreshThreshold = state.refreshThreshold,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreRotaryScrollEvent {
                        state.dragOffset > 0
                    }
                    .pullToDatePickerDrag(state),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.padding(top = 28.dp)
                ) {
                    OneUiCapsuleSurface(
                        title = stringResource(R.string.home_empty_course_hint),
                        subtitle = stringResource(R.string.home_empty_course_hint),
                        icon = Icons.Rounded.EventAvailable,
                        emphasize = true,
                    )
                }
            }
        }
    }
}

/**
 * 课程列表页面组件
 */
@Composable
private fun CourseListPager(
    coursesUi: List<CourseUi>,
    state: PullToDatePickerState,
    itemKey: (CourseUi) -> Any,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable TransformingLazyColumnItemScope.(CourseUi, TransformationSpec) -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState(initialAnchorItemIndex = 0)
    val transformationSpec = rememberTransformationSpec()
    val isTouching = remember { AtomicBoolean(false) }
    val focusRequester = remember { FocusRequester() }
    var minuteTick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            minuteTick++
        }
    }

    val nestedScrollConnection = rememberPullToRefreshConnection(
        scrollState = scrollState, state = state, isTouching = { isTouching.get() })

    LaunchedEffect(state.dragOffset) {
        if (state.dragOffset > 0) {
            scrollState.scrollToItem(0)
        } else {
            focusRequester.requestFocus()
        }
    }

    ScreenScaffold(
        scrollState = scrollState,
        scrollIndicator = {
            if (state.dragOffset == 0f) {
                ScrollIndicator(state = scrollState)
            }
        }
    ) { contentPadding ->
        PullToDatePicker(
            dragOffset = state.dragOffset,
            refreshThreshold = state.refreshThreshold,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected
        ) {
            TransformingLazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .onPreRotaryScrollEvent {
                        state.dragOffset > 0
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                isTouching.set(event.changes.any { it.pressed })
                            }
                        }
                    }
                    .nestedScroll(nestedScrollConnection),
                state = scrollState,
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(scrollState),
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollState),
                contentPadding = contentPadding
            ) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                itemsIndexed(
                    items = coursesUi, key = { _, item -> itemKey(item) }) { _, item ->
                    this.itemContent(item, transformationSpec)
                }
            }
        }
    }
}

private fun courseStatus(course: CourseUi, selectedDate: LocalDate, all: List<CourseUi>): Triple<Boolean, Boolean, Int?> {
    val zone = TimeZone.currentSystemDefault()
    val now = Clock.System.now().toLocalDateTime(zone)
    if (selectedDate != Clock.System.todayIn(zone)) return Triple(false, false, null)
    val nowMin = now.hour * 60 + now.minute
    val start = course.timeSlot.startTime?.let { it.hour * 60 + it.minute } ?: return Triple(false, false, null)
    val end = course.timeSlot.endTime?.let { it.hour * 60 + it.minute } ?: return Triple(false, false, null)
    val current = nowMin in start until end
    val nextId = all.firstOrNull { c -> c.timeSlot.startTime?.let { it.hour * 60 + it.minute > nowMin } == true }?.timeSlot?.id
    return Triple(current, !current && nextId == course.timeSlot.id, if (current) (end - nowMin).coerceAtLeast(0) else null)
}
