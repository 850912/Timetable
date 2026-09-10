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
import androidx.compose.runtime.mutableLongStateOf
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
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
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
    val config = LocalAppConfig.current

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
            // Re-evaluate current/next-course state periodically.  This state must
            // live in the same lexical scope as the item-content lambda below;
            // keeping it inside CourseListPager made `minuteTick` inaccessible
            // here and caused the release Kotlin compilation to fail.
            var minuteTick by remember { mutableLongStateOf(0L) }
            LaunchedEffect(Unit) {
                while (true) {
                    val now = System.currentTimeMillis()
                    delay(60_000L - (now % 60_000L))
                    minuteTick++
                }
            }
            val statusSummary = remember(coursesUi, selectedDate, minuteTick) {
                calculateCourseStatusSummary(coursesUi, selectedDate)
            }

            CourseListPager(
                coursesUi = coursesUi,
                state = pullToDatePickerState,
                itemKey = { courseUi -> courseUi.timeSlot.id },
                selectedDate = selectedDate,
                onDateSelected = handleDateSelected,
                showTopTime = config.isShowTopTime
            ) { courseUi, transformationSpec ->
                val courseId = courseUi.timeSlot.id
                CourseCard(
                    course = courseUi,
                    isCurrent = statusSummary.currentId == courseId,
                    isNext = statusSummary.nextId == courseId,
                    minutesLeft = if (statusSummary.currentId == courseId) statusSummary.minutesLeft else null,
                    is24HourFormat = config.is24HourFormat,
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
    showTopTime: Boolean = false,
    modifier: Modifier = Modifier,
    itemContent: @Composable TransformingLazyColumnItemScope.(CourseUi, TransformationSpec) -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState(initialAnchorItemIndex = 0)
    val transformationSpec = rememberTransformationSpec()
    val isTouching = remember { AtomicBoolean(false) }
    val focusRequester = remember { FocusRequester() }

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
                            .padding(top = if (showTopTime) 30.dp else 10.dp)
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

private data class CourseStatusSummary(
    val currentId: Long? = null,
    val nextId: Long? = null,
    val minutesLeft: Int? = null,
)

private fun calculateCourseStatusSummary(
    courses: List<CourseUi>,
    selectedDate: LocalDate,
): CourseStatusSummary {
    val zone = TimeZone.currentSystemDefault()
    val now = Clock.System.now().toLocalDateTime(zone)
    if (selectedDate != Clock.System.todayIn(zone)) return CourseStatusSummary()

    val nowMin = now.time.hour * 60 + now.time.minute
    var currentId: Long? = null
    var currentMinutesLeft: Int? = null
    var nextId: Long? = null
    var smallestUntilStart = Int.MAX_VALUE

    for (course in courses) {
        val start = course.timeSlot.startTime?.let { it.hour * 60 + it.minute } ?: continue
        val end = course.timeSlot.endTime?.let { it.hour * 60 + it.minute } ?: continue
        if (isWithinSlot(nowMin, start, end)) {
            if (currentId == null) {
                currentId = course.timeSlot.id
                currentMinutesLeft = minutesUntil(nowMin, end).coerceAtLeast(1)
            }
        } else {
            val untilStart = start - nowMin
            if (untilStart > 0 && untilStart < smallestUntilStart) {
                smallestUntilStart = untilStart
                nextId = course.timeSlot.id
            }
        }
    }

    return CourseStatusSummary(currentId, nextId, currentMinutesLeft)
}

private fun isWithinSlot(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean =
    if (startMinutes <= endMinutes) {
        nowMinutes in startMinutes until endMinutes
    } else {
        nowMinutes >= startMinutes || nowMinutes < endMinutes
    }

private fun minutesUntil(nowMinutes: Int, targetMinutes: Int): Int =
    if (targetMinutes >= nowMinutes) targetMinutes - nowMinutes else (24 * 60 - nowMinutes) + targetMinutes
