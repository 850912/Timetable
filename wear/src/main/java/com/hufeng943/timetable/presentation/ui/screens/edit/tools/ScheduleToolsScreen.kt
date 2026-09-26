package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePickerType
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.WearDatePickerPage
import com.hufeng943.timetable.presentation.ui.components.WearTimePickerPage
import com.hufeng943.timetable.presentation.ui.components.WearWheelPickerPage
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.BatchAction
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleToolsState
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleToolsViewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalTime

@Composable
private fun ScheduleToolsCompletionEffect(viewModel: ScheduleToolsViewModel) {
    val nav = LocalNavController.current
    LaunchedEffect(viewModel) {
        viewModel.completed.collect {
            nav.popBackStack(NavRoutes.SCHEDULE_TOOLS, inclusive = true)
        }
    }
}

/** Main destination of the batch schedule-tools graph. */
@Composable
fun ScheduleToolsScreen(viewModel: ScheduleToolsViewModel) {
    ScheduleToolsCompletionEffect(viewModel)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    val config = LocalAppConfig.current

    when (val current = state) {
        ScheduleToolsState.Loading -> ScreenScaffold(timeText = {}) {}
        is ScheduleToolsState.Error -> SimpleMessageScreen("批量日程工具", current.message)
        is ScheduleToolsState.Ready -> {
            val valid = (editor.end?.let { it >= editor.start } ?: true) &&
                (!editor.useWindow || editor.windowEnd > editor.windowStart) &&
                (editor.action != BatchAction.SHIFT || editor.offsetMinutes != 0)
            val scroll = rememberTransformingLazyColumnState()
            val transform = rememberTransformationSpec()
            ScreenScaffold(
                scrollState = scroll,
                timeText = {},
                edgeButton = {
                    EdgeButton(enabled = valid, onClick = viewModel::applyEditor) {
                        Icon(Icons.Rounded.Check, "确认")
                    }
                },
            ) { padding ->
                TransformingLazyColumn(
                    state = scroll,
                    contentPadding = padding,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        ListHeader(
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                            transformation = SurfaceTransformation(transform),
                        ) { Text("批量日程工具") }
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "范围：全部课表",
                            subtitle = "${current.timetables.size} 个课表 · 全部课程/课时",
                            icon = Icons.Rounded.SelectAll,
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "临时放假",
                            subtitle = "从 ${editor.start.toDisplayString()} 起 · 点按选择天数",
                            icon = Icons.Rounded.EventBusy,
                            emphasize = true,
                            onClick = { nav.navigateSingle(NavRoutes.SCHEDULE_TOOLS_DAYS) },
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = when (editor.action) {
                                BatchAction.SHIFT -> if (editor.offsetMinutes < 0) {
                                    "统一提前 ${-editor.offsetMinutes} 分钟"
                                } else {
                                    "统一延时 ${editor.offsetMinutes} 分钟"
                                }
                                BatchAction.CANCEL -> "停课"
                                BatchAction.RESTORE -> "恢复正常"
                            },
                            subtitle = "点按切换操作；长按调时",
                            icon = when (editor.action) {
                                BatchAction.SHIFT -> Icons.Rounded.Schedule
                                BatchAction.CANCEL -> Icons.Rounded.EventBusy
                                BatchAction.RESTORE -> Icons.Rounded.Restore
                            },
                            onClick = viewModel::cycleAction,
                            onLongClick = { nav.navigateSingle(NavRoutes.SCHEDULE_TOOLS_OFFSET) },
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    if (editor.action == BatchAction.SHIFT) {
                        item {
                            OneUiCapsuleSurface(
                                title = "提前 / 延时",
                                subtitle = if (editor.offsetMinutes < 0) {
                                    "提前 ${-editor.offsetMinutes} 分钟"
                                } else {
                                    "延时 ${editor.offsetMinutes} 分钟"
                                },
                                icon = Icons.Rounded.Tune,
                                onClick = { nav.navigateSingle(NavRoutes.SCHEDULE_TOOLS_OFFSET) },
                                modifier = Modifier.fillMaxWidth()
                                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                            )
                        }
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "开始：${editor.start.toDisplayString()}",
                            subtitle = "点按选择生效日期",
                            onClick = { nav.navigateSingle(NavRoutes.SCHEDULE_TOOLS_START) },
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "结束：${editor.end?.toDisplayString() ?: "永不结束"}",
                            subtitle = "点按选择；长按永久",
                            onClick = { nav.navigateSingle(NavRoutes.SCHEDULE_TOOLS_END) },
                            onLongClick = { viewModel.updateEnd(null) },
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = if (editor.useWindow) {
                                "仅处理 ${editor.windowStart.toDisplayString(config.is24HourFormat)}–${editor.windowEnd.toDisplayString(config.is24HourFormat)}"
                            } else {
                                "全部时段"
                            },
                            subtitle = "点按开关；长按设开始",
                            selected = editor.useWindow,
                            onClick = viewModel::toggleWindow,
                            onLongClick = {
                                viewModel.enableWindow()
                                nav.navigateSingle(NavRoutes.SCHEDULE_TOOLS_WINDOW_START)
                            },
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    if (editor.useWindow) {
                        item {
                            OneUiCapsuleSurface(
                                title = "时间窗结束：${editor.windowEnd.toDisplayString(config.is24HourFormat)}",
                                subtitle = "点按修改",
                                onClick = { nav.navigateSingle(NavRoutes.SCHEDULE_TOOLS_WINDOW_END) },
                                modifier = Modifier.fillMaxWidth()
                                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleToolsStartDateScreen(viewModel: ScheduleToolsViewModel) {
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    DatePage(editor.start) {
        viewModel.updateStart(it)
        nav.popSafe()
    }
}

@Composable
fun ScheduleToolsEndDateScreen(viewModel: ScheduleToolsViewModel) {
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    DatePage(editor.end ?: editor.start) {
        viewModel.updateEnd(it)
        nav.popSafe()
    }
}

@Composable
fun ScheduleToolsWindowStartScreen(viewModel: ScheduleToolsViewModel) {
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    TimePage(editor.windowStart, LocalAppConfig.current.is24HourFormat) {
        viewModel.updateWindowStart(it)
        nav.popSafe()
    }
}

@Composable
fun ScheduleToolsWindowEndScreen(viewModel: ScheduleToolsViewModel) {
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    TimePage(editor.windowEnd, LocalAppConfig.current.is24HourFormat) {
        viewModel.updateWindowEnd(it)
        nav.popSafe()
    }
}

@Composable
fun ScheduleToolsDaysScreen(viewModel: ScheduleToolsViewModel) {
    ScheduleToolsCompletionEffect(viewModel)
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    ValuePickerPage("临时放假", (1..30).toList(), 1, { "$it 天" }) { days ->
        viewModel.apply(
            action = BatchAction.CANCEL,
            startDate = editor.start,
            endDate = editor.start.plus(days - 1, DateTimeUnit.DAY),
            offsetMinutes = 0,
            timetableId = null,
            courseId = null,
            timeWindowStart = null,
            timeWindowEnd = null,
        )
    }
}

@Composable
fun ScheduleToolsOffsetScreen(viewModel: ScheduleToolsViewModel) {
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    ValuePickerPage(
        title = "统一提前 / 延时",
        values = (-30..30).filter { it != 0 },
        initial = editor.offsetMinutes.coerceIn(-30, 30).let { if (it == 0) 10 else it },
        label = { if (it < 0) "提前 ${-it} 分钟" else "延时 $it 分钟" },
    ) {
        viewModel.updateOffset(it)
        nav.popSafe()
    }
}

@Composable
private fun ValuePickerPage(
    title: String,
    values: List<Int>,
    initial: Int,
    label: (Int) -> String,
    onConfirm: (Int) -> Unit,
) {
    WearWheelPickerPage(
        title = title,
        values = values,
        initial = initial,
        label = label,
        onConfirm = onConfirm,
    )
}

@Composable
private fun DatePage(initial: LocalDate, onPicked: (LocalDate) -> Unit) {
    WearDatePickerPage(
        initialDate = initial.toJavaLocalDate(),
        onDatePicked = { onPicked(it.toKotlinLocalDate()) },
    )
}

@Composable
private fun TimePage(initial: LocalTime, is24: Boolean, onPicked: (LocalTime) -> Unit) {
    WearTimePickerPage(
        initialTime = initial.toJavaLocalTime(),
        onTimePicked = { onPicked(it.toKotlinLocalTime()) },
        timePickerType = if (is24) TimePickerType.HoursMinutes24H else TimePickerType.HoursMinutesAmPm12H,
    )
}
