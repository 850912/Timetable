package com.hufeng943.timetable.presentation.ui.screens.home

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.NavRoutes.courseDetail
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.common.ui.CourseUi
import com.hufeng943.timetable.presentation.ui.components.CourseCard
import com.hufeng943.timetable.presentation.ui.components.DayFinishedCard
import com.hufeng943.timetable.presentation.ui.components.HandleEditUiState
import com.hufeng943.timetable.presentation.ui.components.PullToDatePicker
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.PullToDatePickerState
import com.hufeng943.timetable.presentation.ui.components.pullToDatePickerDrag
import com.hufeng943.timetable.presentation.ui.components.rememberPullToDatePickerState
import com.hufeng943.timetable.presentation.ui.components.rememberPullToRefreshConnection
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
import com.hufeng943.timetable.presentation.ui.common.TimetableBackgroundMode
import com.hufeng943.timetable.presentation.ui.theme.GalaxyAiAmbientLayer
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
@Composable
fun TimetablePager(
    viewModel: TimetableViewModel = hiltViewModel(),
    onOpenStateChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.dateCoursesUi.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedWeekNumber by viewModel.selectedWeekNumber.collectAsStateWithLifecycle()
    val selectedSemesterWeekCount by viewModel.selectedSemesterWeekCount.collectAsStateWithLifecycle()
    val selectedDateEvents by viewModel.selectedDateEvents.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    val config = LocalAppConfig.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var freshnessTick by remember { mutableLongStateOf(0L) }
    var lastCalendarDate by remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault())) }

    // Re-evaluate time-sensitive course state immediately when the app returns to foreground,
    // or when Android reports a clock/date/time-zone change. This avoids a stale "下课"/
    // "即将上课" card after Wear OS has suspended the process in power saver.
    DisposableEffect(lifecycleOwner, context, selectedDate) {
        fun refreshForWallClockChange() {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            if (today != lastCalendarDate && selectedDate == lastCalendarDate) {
                viewModel.updateSelectedDate(today)
            }
            lastCalendarDate = today
            freshnessTick++
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshForWallClockChange()
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) = refreshForWallClockChange()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState !is UiState.Success) {
            onOpenStateChanged(false)
        }
    }

    Box(Modifier.fillMaxSize()) {
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

        val isDatePickerOpen = pullToDatePickerState.dragOffset > 0f
        LaunchedEffect(isDatePickerOpen) {
            onOpenStateChanged(isDatePickerOpen)
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
            LaunchedEffect(coursesUi, selectedDate, freshnessTick) {
                while (true) {
                    val waitMillis = nextCourseStatusWakeMillis(coursesUi, selectedDate)
                    if (waitMillis == null) awaitCancellation()
                    delay(waitMillis)
                    minuteTick++
                }
            }
            val statusSummary = remember(coursesUi, selectedDate, minuteTick, freshnessTick) {
                calculateCourseStatusSummary(coursesUi, selectedDate)
            }

            CourseListPager(
                coursesUi = coursesUi,
                state = pullToDatePickerState,
                itemKey = { courseUi -> courseUi.timeSlot.id },
                selectedDate = selectedDate,
                onDateSelected = handleDateSelected,
                weekNumber = selectedWeekNumber,
                totalWeekCount = selectedSemesterWeekCount,
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
                    nextCourseName = if (statusSummary.currentId == courseId) {
                        coursesUi.firstOrNull { it.timeSlot.id == statusSummary.nextId }?.displayName
                    } else null,
                    minutesUntilNext = if (statusSummary.currentId == courseId) statusSummary.minutesUntilNext else null,
                    is24HourFormat = config.is24HourFormat,
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    navController.navigateSingle(courseDetail(courseUi.timeSlot.id))
                }
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

    val isDatePickerOpen = state.dragOffset > 0f
    LaunchedEffect(isDatePickerOpen) {
        if (!isDatePickerOpen) focusRequester.requestFocus()
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
                        title = selectedDateEmptyTitle(selectedDate),
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
    totalWeekCount: Int? = null,
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
    val currentCourse = statusSummary.currentId?.let { id ->
        coursesUi.firstOrNull { it.timeSlot.id == id }
    }
    val nextCourse = statusSummary.nextId?.let { id ->
        coursesUi.firstOrNull { it.timeSlot.id == id }
    }
    var showFinishedTimetable by remember(selectedDate, statusSummary.dayFinished) { mutableStateOf(false) }
    val shouldShowCourseList = !isToday || !statusSummary.dayFinished || showFinishedTimetable

    val nestedScrollConnection = rememberPullToRefreshConnection(
        scrollState = scrollState, state = state, isTouching = { isTouching.get() })

    // Opening the app during class should land on the actual highlighted course card,
    // not on a duplicated summary card. Header occupies index 0.
    LaunchedEffect(selectedDate, statusSummary.currentId) {
        val currentId = statusSummary.currentId ?: return@LaunchedEffect
        if (!isToday || statusSummary.dayFinished) return@LaunchedEffect
        val courseIndex = coursesUi.indexOfFirst { it.timeSlot.id == currentId }
        if (courseIndex >= 0) {
            scrollState.scrollToItem(courseIndex + 1)
        }
    }

    val isDatePickerOpen = state.dragOffset > 0f
    LaunchedEffect(isDatePickerOpen) {
        if (isDatePickerOpen) {
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
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollState, hapticFeedbackEnabled = false),
                contentPadding = contentPadding
            ) {
                if (isToday && statusSummary.dayFinished) {
                    item {
                        DayFinishedCard(
                            title = stringResource(R.string.home_day_finished_free_title),
                            subtitle = stringResource(R.string.home_day_finished_title),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (showTopTime) 30.dp else 10.dp)
                                
                                .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding),
                            transformation = SurfaceTransformation(transformationSpec),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = stringResource(
                                if (showFinishedTimetable) R.string.home_hide_today_timetable
                                else R.string.home_view_today_timetable
                            ),
                            icon = Icons.Rounded.EventAvailable,
                            onClick = { showFinishedTimetable = !showFinishedTimetable },
                            modifier = Modifier
                                .fillMaxWidth()
                                
                                .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding),
                        )
                    }
                }

                if (shouldShowCourseList) {
                    item {
                        ListHeader(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = if (!isToday && showTopTime) 30.dp
                                    else if (!isToday) 10.dp
                                    else if (statusSummary.dayFinished) 2.dp
                                    else if (showTopTime) 30.dp
                                    else 10.dp
                                )
                                
                                .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                            transformation = SurfaceTransformation(transformationSpec)
                        ) {
                            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val dateTitle = selectedDateCourseTitle(selectedDate)
                                Text(
                                    text = when {
                                        weekNumber != null && totalWeekCount != null && totalWeekCount >= weekNumber ->
                                            stringResource(R.string.home_week_progress_title, dateTitle, weekNumber, totalWeekCount)
                                        weekNumber != null -> stringResource(R.string.home_week_title, dateTitle, weekNumber)
                                        else -> dateTitle
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = Int.MAX_VALUE,
                                    overflow = TextOverflow.Clip,
                                    textAlign = TextAlign.Center,
                                )
                                val statusLine = if (
                                    isToday &&
                                    currentCourse == null &&
                                    nextCourse != null &&
                                    statusSummary.minutesUntilNext != null
                                ) {
                                    untilClassText(statusSummary.minutesUntilNext)
                                } else null
                                Text(
                                    text = if (statusLine == null) daySummary else "$daySummary\n$statusLine",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = Int.MAX_VALUE,
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
                }
                itemsIndexed(
                    items = events,
                    key = { _, event -> "event-${event.id}" },
                ) { _, event ->
                    OneUiCapsuleSurface(
                        title = event.title,
                        subtitle = eventSubtitle(event, is24HourFormat),
                        icon = Icons.Rounded.EventAvailable,
                        modifier = Modifier
                            .fillMaxWidth()
                            
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
private fun selectedDateEmptyTitle(selectedDate: LocalDate): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val javaDate = java.time.LocalDate.parse(selectedDate.toString())
    val javaToday = java.time.LocalDate.parse(today.toString())
    return when {
        javaDate == javaToday -> stringResource(R.string.home_empty_course_hint)
        javaDate == javaToday.plusDays(1) -> stringResource(R.string.home_tomorrow_empty_course_hint)
        else -> stringResource(R.string.home_date_empty_course_hint, javaDate.monthValue, javaDate.dayOfMonth)
    }
}

@Composable
private fun selectedDateCourseTitle(selectedDate: LocalDate): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    if (selectedDate == today) return stringResource(R.string.home_title)
    val javaDate = java.time.LocalDate.parse(selectedDate.toString())
    if (javaDate == java.time.LocalDate.parse(today.toString()).plusDays(1)) {
        return stringResource(R.string.home_tomorrow_title)
    }
    return stringResource(R.string.home_date_title, javaDate.monthValue, javaDate.dayOfMonth)
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
        if (start > nowSec && (nextStartSec?.let { start < it } ?: true)) nextStartSec = start
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
