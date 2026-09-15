package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimePickerType
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.rememberPickerState
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.BatchAction
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleToolsState
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleToolsViewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

enum class ScheduleToolPage { MAIN, START_DATE, END_DATE, WINDOW_START, WINDOW_END, HOLIDAY_DAYS, OFFSET }

@Composable
fun ScheduleToolsScreen(viewModel: ScheduleToolsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    val config = LocalAppConfig.current
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.completed.collect { nav.popSafe() }
    }

    when (val current = state) {
        ScheduleToolsState.Loading -> ScreenScaffold { }
        is ScheduleToolsState.Error -> SimpleMessageScreen("批量日程工具", current.message)
        is ScheduleToolsState.Ready -> {
            val timetables = current.timetables
            val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
            val defaultStart = remember(timetables, today) {
                timetables.minOfOrNull { maxOf(today, it.semesterStart) } ?: today
            }
            var startDate by remember { mutableStateOf(defaultStart) }
            var endDate by remember { mutableStateOf<LocalDate?>(null) }
            var timetableIndex by remember { mutableStateOf(0) } // 0 = all
            var courseIndex by remember { mutableStateOf(0) } // 0 = all
            var offset by remember { mutableStateOf(10) }
            var useWindow by remember { mutableStateOf(false) }
            var windowStart by remember { mutableStateOf(LocalTime(8, 0)) }
            var windowEnd by remember { mutableStateOf(LocalTime(18, 0)) }
            var page by remember { mutableStateOf(ScheduleToolPage.MAIN) }

            BackHandler(enabled = page != ScheduleToolPage.MAIN) { page = ScheduleToolPage.MAIN }

            val selectedTimetable = timetables.getOrNull(timetableIndex - 1)
            val courses = selectedTimetable?.allCourses.orEmpty()
            val selectedCourse = courses.getOrNull(courseIndex - 1)

            when (page) {
                ScheduleToolPage.START_DATE -> DatePage(startDate) { picked ->
                    startDate = picked
                    if (endDate != null && endDate!! < picked) endDate = picked
                    page = ScheduleToolPage.MAIN
                }
                ScheduleToolPage.END_DATE -> DatePage(endDate ?: startDate) {
                    endDate = it
                    page = ScheduleToolPage.MAIN
                }
                ScheduleToolPage.WINDOW_START -> TimePage(windowStart, config.is24HourFormat) {
                    windowStart = it
                    page = ScheduleToolPage.MAIN
                }
                ScheduleToolPage.WINDOW_END -> TimePage(windowEnd, config.is24HourFormat) {
                    windowEnd = it
                    page = ScheduleToolPage.MAIN
                }
                ScheduleToolPage.HOLIDAY_DAYS -> PickerChoiceScreen(
                    title = "临时放假",
                    values = (1..30).toList(),
                    initialValue = 1,
                    label = { "$it 天" },
                    onConfirm = { days ->
                        val holidayEnd = startDate.plus((days - 1), DateTimeUnit.DAY)
                        viewModel.apply(
                            action = BatchAction.CANCEL,
                            startDate = startDate,
                            endDate = holidayEnd,
                            offsetMinutes = 0,
                            timetableId = selectedTimetable?.id,
                            courseId = null,
                            timeWindowStart = null,
                            timeWindowEnd = null,
                        )
                    },
                )
                ScheduleToolPage.OFFSET -> PickerChoiceScreen(
                    title = "统一提前 / 延时",
                    values = (-120..120 step 5).filter { it != 0 },
                    initialValue = offset.takeIf { it != 0 } ?: 10,
                    label = { value -> if (value < 0) "提前 ${-value} 分钟" else "延时 $value 分钟" },
                    onConfirm = { value ->
                        offset = value
                        viewModel.apply(
                            action = BatchAction.SHIFT,
                            startDate = startDate,
                            endDate = endDate,
                            offsetMinutes = value,
                            timetableId = selectedTimetable?.id,
                            courseId = selectedCourse?.id,
                            timeWindowStart = if (useWindow) windowStart else null,
                            timeWindowEnd = if (useWindow) windowEnd else null,
                        )
                    },
                )
                ScheduleToolPage.MAIN -> {
                    val tableScope = selectedTimetable?.semesterName ?: "全部课表"
                    val courseScope = selectedCourse?.name ?: "全部课程"
                    val scopeTitle = if (selectedTimetable == null) tableScope else "$tableScope · $courseScope"
                    val validRange = endDate == null || endDate!! >= startDate
                    val validWindow = !useWindow || windowEnd > windowStart
                    val scroll = rememberTransformingLazyColumnState()
                    val transform = rememberTransformationSpec()
                    ScreenScaffold(scrollState = scroll) { padding ->
                        TransformingLazyColumn(
                    state = scroll,
                    flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(scroll),
                    rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scroll),
                    contentPadding = padding,
                    modifier = Modifier.fillMaxSize(),
                ) {
                            item {
                                ListHeader(
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                                    transformation = SurfaceTransformation(transform),
                                ) { Text("批量日程工具") }
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = "临时放假",
                                    subtitle = "从 ${startDate.toDisplayString()} 起 · 滑动选择放假天数",
                                    icon = Icons.Rounded.EventBusy,
                                    emphasize = true,
                                    onClick = { if (validRange) page = ScheduleToolPage.HOLIDAY_DAYS },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = "统一提前 / 延时",
                                    subtitle = "当前 ${if (offset < 0) "提前 ${-offset}" else "延时 $offset"} 分钟 · 点按滑动选择",
                                    icon = Icons.Rounded.Schedule,
                                    onClick = { if (validRange && validWindow) page = ScheduleToolPage.OFFSET },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = "恢复临时调整",
                                    subtitle = "清除所选日期范围内的停课 / 临时调时",
                                    icon = Icons.Rounded.Restore,
                                    onClick = {
                                        if (validRange && validWindow) {
                                            viewModel.apply(
                                                BatchAction.RESTORE,
                                                startDate,
                                                endDate,
                                                0,
                                                selectedTimetable?.id,
                                                selectedCourse?.id,
                                                if (useWindow) windowStart else null,
                                                if (useWindow) windowEnd else null,
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = "范围：$scopeTitle",
                                    subtitle = if (selectedTimetable == null) "点按选择课表" else "点按切换课表 · 长按切换课程",
                                    icon = Icons.Rounded.Tune,
                                    onClick = {
                                        timetableIndex = (timetableIndex + 1) % (timetables.size + 1)
                                        courseIndex = 0
                                    },
                                    onLongClick = {
                                        if (selectedTimetable != null && courses.isNotEmpty()) {
                                            courseIndex = (courseIndex + 1) % (courses.size + 1)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = "开始：${startDate.toDisplayString()}",
                                    subtitle = "点按选择生效开始日期",
                                    onClick = { page = ScheduleToolPage.START_DATE },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = "结束：${endDate?.toDisplayString() ?: "永不结束"}",
                                    subtitle = if (endDate == null) "点按选择结束日期" else "点按选择日期 · 长按设为永不结束",
                                    onClick = { page = ScheduleToolPage.END_DATE },
                                    onLongClick = { endDate = null },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = if (useWindow) "仅处理 ${windowStart.toDisplayString(config.is24HourFormat)}–${windowEnd.toDisplayString(config.is24HourFormat)}" else "全部时段",
                                    subtitle = if (useWindow) "点按关闭 · 长按设置开始时间" else "点按限制到某一时间段",
                                    selected = useWindow,
                                    onClick = { useWindow = !useWindow },
                                    onLongClick = { useWindow = true; page = ScheduleToolPage.WINDOW_START },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            if (useWindow) item {
                                OneUiCapsuleSurface(
                                    title = "时间窗结束：${windowEnd.toDisplayString(config.is24HourFormat)}",
                                    subtitle = "点按修改结束时间",
                                    onClick = { page = ScheduleToolPage.WINDOW_END },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerChoiceScreen(
    title: String,
    values: List<Int>,
    initialValue: Int,
    label: (Int) -> String,
    onConfirm: (Int) -> Unit,
) {
    val initialIndex = values.indexOf(initialValue).coerceAtLeast(0)
    val pickerState = rememberPickerState(
        initialNumberOfOptions = values.size,
        initiallySelectedIndex = initialIndex,
        shouldRepeatOptions = false,
    )
    ScreenScaffold(
        timeText = {},
        edgeButton = {
            EdgeButton(onClick = { onConfirm(values[pickerState.selectedOptionIndex]) }) {
                Icon(Icons.Rounded.Check, contentDescription = "确认")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
                .scrollable(state = pickerState, orientation = Orientation.Vertical, reverseDirection = true),
            contentAlignment = Alignment.Center,
        ) {
            Text(title, modifier = Modifier.align(Alignment.TopCenter))
            Picker(
                state = pickerState,
                modifier = Modifier.size(150.dp, 120.dp),
                contentDescription = { label(values[pickerState.selectedOptionIndex]) },
            ) { index ->
                Text(label(values[index]))
            }
        }
    }
}

@Composable
private fun DatePage(initial: LocalDate, onPicked: (LocalDate) -> Unit) {
    ScreenScaffold(timeText = {}) {
        DatePicker(initialDate = initial.toJavaLocalDate(), onDatePicked = { onPicked(it.toKotlinLocalDate()) })
    }
}

@Composable
private fun TimePage(initial: LocalTime, is24Hour: Boolean, onPicked: (LocalTime) -> Unit) {
    ScreenScaffold(timeText = {}) {
        TimePicker(
            initialTime = initial.toJavaLocalTime(),
            onTimePicked = { onPicked(it.toKotlinLocalTime()) },
            timePickerType = if (is24Hour) TimePickerType.HoursMinutes24H else TimePickerType.HoursMinutesAmPm12H,
        )
    }
}
