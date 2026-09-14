package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimePickerType
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.MultiDateSlotState
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.MultiDateSlotViewModel
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
import java.time.format.TextStyle
import kotlin.time.Clock

enum class MultiDatePage { MAIN, ANCHOR_DATE, START_TIME, END_TIME }

@Composable
fun MultiDateSlotScreen(viewModel: MultiDateSlotViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    val config = LocalAppConfig.current
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.completed.collect { navController.popSafe() }
    }

    when (val current = state) {
        MultiDateSlotState.Loading -> ScreenScaffold { }
        is MultiDateSlotState.Error -> SimpleMessageScreen("创建课时失败", current.message)
        is MultiDateSlotState.Ready -> {
            val data = current.data
            val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
            var anchorDate by remember(data.timetable.semesterStart) {
                mutableStateOf(maxOf(today, data.timetable.semesterStart))
            }
            var startTime by remember { mutableStateOf(LocalTime(8, 0)) }
            var endTime by remember { mutableStateOf(LocalTime(9, 0)) }
            var selectedDates by remember { mutableStateOf(setOf<LocalDate>()) }
            var page by remember { mutableStateOf(MultiDatePage.MAIN) }

            when (page) {
                MultiDatePage.ANCHOR_DATE -> ScreenScaffold(timeText = {}) {
                    DatePicker(
                        initialDate = anchorDate.toJavaLocalDate(),
                        onDatePicked = { anchorDate = it.toKotlinLocalDate(); page = MultiDatePage.MAIN },
                    )
                }
                MultiDatePage.START_TIME -> ScreenScaffold(timeText = {}) {
                    TimePicker(
                        initialTime = startTime.toJavaLocalTime(),
                        onTimePicked = { startTime = it.toKotlinLocalTime(); if (endTime <= startTime) endTime = startTime.plusMinutesSafe(60); page = MultiDatePage.MAIN },
                        timePickerType = if (config.is24HourFormat) TimePickerType.HoursMinutes24H else TimePickerType.HoursMinutesAmPm12H,
                    )
                }
                MultiDatePage.END_TIME -> ScreenScaffold(timeText = {}) {
                    TimePicker(
                        initialTime = endTime.toJavaLocalTime(),
                        onTimePicked = { endTime = it.toKotlinLocalTime(); page = MultiDatePage.MAIN },
                        timePickerType = if (config.is24HourFormat) TimePickerType.HoursMinutes24H else TimePickerType.HoursMinutesAmPm12H,
                    )
                }
                MultiDatePage.MAIN -> {
                    val semesterEnd = data.timetable.semesterEnd
                    val candidates = remember(anchorDate, semesterEnd) {
                        (0 until 31).map { anchorDate.plus(it, DateTimeUnit.DAY) }
                            .filter { semesterEnd == null || it <= semesterEnd }
                    }
                    val scrollState = rememberTransformingLazyColumnState()
                    val transform = rememberTransformationSpec()
                    ScreenScaffold(
                        scrollState = scrollState,
                        edgeButton = {
                            EdgeButton(
                                onClick = {
                                    if (selectedDates.isNotEmpty() && endTime > startTime) {
                                        viewModel.create(selectedDates, startTime, endTime)
                                    }
                                },
                                enabled = selectedDates.isNotEmpty() && endTime > startTime,
                            ) { Icon(Icons.Rounded.Check, contentDescription = "创建") }
                        },
                    ) { padding ->
                        TransformingLazyColumn(
                            state = scrollState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = padding,
                        ) {
                            item {
                                ListHeader(
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                                    transformation = SurfaceTransformation(transform),
                                ) { Text("多日期创建 · ${data.course.name}") }
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = "日期窗口：${anchorDate.toDisplayString()}",
                                    subtitle = "点按可跳到任意日期；下面一次可勾选 31 天",
                                    icon = Icons.Rounded.DateRange,
                                    onClick = { page = MultiDatePage.ANCHOR_DATE },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = "${startTime.toDisplayString(config.is24HourFormat)} – ${endTime.toDisplayString(config.is24HourFormat)}",
                                    subtitle = "点按设置开始时间；长按设置结束时间",
                                    icon = Icons.Rounded.Schedule,
                                    onClick = { page = MultiDatePage.START_TIME },
                                    onLongClick = { page = MultiDatePage.END_TIME },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            items(candidates, key = { it.toEpochDays() }) { date ->
                                val selected = date in selectedDates
                                OneUiCapsuleSurface(
                                    title = date.toDisplayString(),
                                    subtitle = date.dayOfWeek.toDisplayString(TextStyle.FULL),
                                    selected = selected,
                                    onClick = {
                                        selectedDates = if (selected) selectedDates - date else selectedDates + date
                                    },
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
fun SimpleMessageScreen(title: String, message: String) {
    val scroll = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = scroll) { padding ->
        TransformingLazyColumn(state = scroll, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { OneUiCapsuleSurface(title = title, subtitle = message) }
        }
    }
}

private fun LocalTime.plusMinutesSafe(minutes: Int): LocalTime {
    val total = (hour * 60 + minute + minutes).coerceIn(0, 1439)
    return LocalTime(total / 60, total % 60)
}
