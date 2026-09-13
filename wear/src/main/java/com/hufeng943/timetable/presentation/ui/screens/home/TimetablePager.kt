package com.hufeng943.timetable.presentation.ui.screens.home

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
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
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.UiState
import com.hufeng943.timetable.presentation.viewmodel.home.TimetableViewModel
import com.hufeng943.timetable.shared.model.AcademicEvent
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

import androidx.lifecycle.compose.collectAsStateWithLifecycle
@Composable
fun TimetablePager(
    viewModel: TimetableViewModel = hiltViewModel(),
    onOpenStateChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.dateCoursesUi.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedWeekNumber by viewModel.selectedWeekNumber.collectAsStateWithLifecycle()
    val selectedDateEvents by viewModel.selectedDateEvents.collectAsStateWithLifecycle()
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
                onDateSelected = handleDateSelected,
                events = selectedDateEvents,
                is24HourFormat = config.is24HourFormat,
            )
        } else {
            // Re-evaluate current/next-course state periodically.  This state must
            // live in the same lexical scope as the item-content lambda below;
            // keeping it inside CourseListPager made `minuteTick` inaccessible
            // here and caused the release Kotlin compilation to fail.
            var minuteTick by remember { mutableLongStateOf(0L) }
            LaunchedEffect(coursesUi, selectedDate) {
                while (true) {
                    val waitMillis = nextCourseStatusWakeMillis(coursesUi, selectedDate)
                    if (waitMillis == null) awaitCancellation()
                    delay(waitMillis)
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
                weekNumber = selectedWeekNumber,
                showTopTime = config.isShowTopTime,
                events = selectedDateEvents,
                is24HourFormat = config.is24HourFormat,
                statusSummary = statusSummary,
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
    onDateSelected: (LocalDate) -> Unit,
    events: List<AcademicEvent> = emptyList(),
    is24HourFormat: Boolean = true,
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
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OneUiCapsuleSurface(
                        title = stringResource(R.string.home_empty_course_hint),
                        subtitle = if (events.isEmpty()) null else stringResource(R.string.home_academic_events_count, events.size),
                        icon = Icons.Rounded.EventAvailable,
                        emphasize = true,
                    )
                    events.take(1).forEach { event ->
                        androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                        OneUiCapsuleSurface(
                            title = event.title,
                            subtitle = eventSubtitle(event, is24HourFormat),
                            icon = Icons.Rounded.EventAvailable,
                        )
                    }
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
    weekNumber: Int? = null,
    showTopTime: Boolean = false,
    events: List<AcademicEvent> = emptyList(),
    is24HourFormat: Boolean = true,
    statusSummary: CourseStatusSummary = CourseStatusSummary(),
    modifier: Modifier = Modifier,
    itemContent: @Composable TransformingLazyColumnItemScope.(CourseUi, TransformationSpec) -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState(initialAnchorItemIndex = 0)
    val transformationSpec = rememberTransformationSpec()
    val isTouching = remember { AtomicBoolean(false) }
    val focusRequester = remember { FocusRequester() }
    val zone = TimeZone.currentSystemDefault()
    val isToday = selectedDate == Clock.System.todayIn(zone)
    val daySummary = wearDaySummary(coursesUi, is24HourFormat)

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
                    .onPreRotaryScrollEvent { state.dragOffset > 0 }
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
                if (isToday) {
                    item {
                        val currentCourse = statusSummary.currentId?.let { id ->
                            coursesUi.firstOrNull { it.timeSlot.id == id }
                        }
                        val nextCourse = statusSummary.nextId?.let { id ->
                            coursesUi.firstOrNull { it.timeSlot.id == id }
                        }
                        val title: String
                        val subtitle: String
                        when {
                            currentCourse != null -> {
                                title = currentCourse.displayName
                                subtitle = stringResource(
                                    R.string.home_summary_in_class,
                                    statusSummary.minutesLeft ?: 1,
                                )
                            }
                            nextCourse != null && statusSummary.minutesUntilNext != null -> {
                                title = untilClassText(statusSummary.minutesUntilNext)
                                subtitle = buildString {
                                    append(nextCourse.displayName)
                                    nextCourse.timeSlot.startTime?.let {
                                        append(" · ").append(it.toDisplayString(is24HourFormat))
                                    }
                                }
                            }
                            else -> {
                                title = stringResource(R.string.home_day_finished_title)
                                subtitle = dayFinishedMessage(selectedDate) + "\n" + stringResource(R.string.home_day_finished_scroll_hint)
                            }
                        }
                        OneUiCapsuleSurface(
                            title = title,
                            subtitle = subtitle,
                            icon = Icons.Rounded.EventAvailable,
                            emphasize = currentCourse != null,
                            titleMaxLines = Int.MAX_VALUE,
                            subtitleMaxLines = Int.MAX_VALUE,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (showTopTime) 30.dp else 10.dp)
                                .transformedHeight(this, transformationSpec)
                                .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding),
                        )
                    }
                }

                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (!isToday && showTopTime) 30.dp else if (!isToday) 10.dp else 2.dp)
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = weekNumber?.let {
                                    stringResource(R.string.home_week_title, stringResource(R.string.home_title), it)
                                } ?: stringResource(R.string.home_title),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = daySummary,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Clip,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = coursesUi, key = { _, item -> itemKey(item) }) { _, item ->
                    this.itemContent(item, transformationSpec)
                }
                itemsIndexed(
                    items = events,
                    key = { _, event -> "event-${event.id}" },
                ) { _, event ->
                    OneUiCapsuleSurface(
                        title = event.title,
                        subtitle = eventSubtitle(event, is24HourFormat),
                        icon = Icons.Rounded.EventAvailable,
                        titleMaxLines = 2,
                        subtitleMaxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun eventSubtitle(event: AcademicEvent, is24HourFormat: Boolean): String {
    val typeLabel = when (event.type) {
        com.hufeng943.timetable.shared.model.AcademicEventType.ASSIGNMENT -> stringResource(R.string.event_type_assignment)
        com.hufeng943.timetable.shared.model.AcademicEventType.EXAM -> stringResource(R.string.event_type_exam)
        com.hufeng943.timetable.shared.model.AcademicEventType.LAB -> stringResource(R.string.event_type_lab)
        com.hufeng943.timetable.shared.model.AcademicEventType.OTHER -> stringResource(R.string.event_type_other)
    }
    return buildString {
        append(typeLabel)
        event.time?.let { append(" · ").append(it.toDisplayString(is24HourFormat)) }
        event.courseName?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
    }
}

@Composable
private fun wearDaySummary(courses: List<CourseUi>, is24HourFormat: Boolean): String {
    val ranges = courses.mapNotNull { course ->
        val start = course.timeSlot.startTime ?: return@mapNotNull null
        val end = course.timeSlot.endTime ?: return@mapNotNull null
        if (end <= start) return@mapNotNull null
        start to end
    }.sortedBy { it.first }
    if (ranges.isEmpty()) return stringResource(R.string.home_summary_empty)
    return stringResource(
        R.string.home_summary_classes,
        ranges.size,
        ranges.first().first.toDisplayString(is24HourFormat),
        ranges.maxBy { it.second }.second.toDisplayString(is24HourFormat),
    )
}

@Composable
private fun untilClassText(totalMinutes: Int): String {
    val safeMinutes = totalMinutes.coerceAtLeast(1)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60
    return stringResource(R.string.home_until_hours_minutes, hours, minutes)
}

@Composable
private fun dayFinishedMessage(date: LocalDate): String {
    val selector = (date.toString().hashCode() and Int.MAX_VALUE) % 4
    return when (selector) {
        0 -> stringResource(R.string.home_day_finished_message_1)
        1 -> stringResource(R.string.home_day_finished_message_2)
        2 -> stringResource(R.string.home_day_finished_message_3)
        else -> stringResource(R.string.home_day_finished_message_4)
    }
}

private data class CourseStatusSummary(
    val currentId: Long? = null,
    val nextId: Long? = null,
    val minutesLeft: Int? = null,
    val minutesUntilNext: Int? = null,
    val dayFinished: Boolean = false,
)

private fun nextCourseStatusWakeMillis(courses: List<CourseUi>, selectedDate: LocalDate): Long? {
    val zone = TimeZone.currentSystemDefault()
    if (selectedDate != Clock.System.todayIn(zone)) return null
    val now = Clock.System.now().toLocalDateTime(zone)
    val nowSec = now.time.hour * 3600 + now.time.minute * 60 + now.time.second
    var nextStartSec: Int? = null
    for (course in courses) {
        val start = course.timeSlot.startTime?.let { it.hour * 3600 + it.minute * 60 + it.second } ?: continue
        val end = course.timeSlot.endTime?.let { it.hour * 3600 + it.minute * 60 + it.second } ?: continue
        if (nowSec in start until end) {
            val nowMillis = System.currentTimeMillis()
            return (60_000L - (nowMillis % 60_000L)).coerceAtLeast(1_000L)
        }
        if (start > nowSec && (nextStartSec == null || start < nextStartSec!!)) nextStartSec = start
    }
    return nextStartSec?.let {
        val untilStart = ((it - nowSec) * 1000L).coerceAtLeast(1_000L)
        val nowMillis = System.currentTimeMillis()
        val nextMinute = (60_000L - (nowMillis % 60_000L)).coerceAtLeast(1_000L)
        minOf(untilStart, nextMinute)
    }
}

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
    var latestEnd = -1

    for (course in courses) {
        val start = course.timeSlot.startTime?.let { it.hour * 60 + it.minute } ?: continue
        val end = course.timeSlot.endTime?.let { it.hour * 60 + it.minute } ?: continue
        if (end <= start) continue
        if (end > latestEnd) latestEnd = end
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

    val untilNext = if (nextId == null || smallestUntilStart == Int.MAX_VALUE) null else smallestUntilStart
    val dayFinished = currentId == null && nextId == null && latestEnd >= 0 && nowMin >= latestEnd
    return CourseStatusSummary(currentId, nextId, currentMinutesLeft, untilNext, dayFinished)
}

private fun isWithinSlot(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean =
    if (startMinutes <= endMinutes) {
        nowMinutes in startMinutes until endMinutes
    } else {
        nowMinutes >= startMinutes || nowMinutes < endMinutes
    }

private fun minutesUntil(nowMinutes: Int, targetMinutes: Int): Int =
    if (targetMinutes >= nowMinutes) targetMinutes - nowMinutes else (24 * 60 - nowMinutes) + targetMinutes
