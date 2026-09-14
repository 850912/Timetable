package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
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
import com.hufeng943.timetable.presentation.ui.screens.common.TextEditScreen
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.BatchAction
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleToolsState
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleToolsViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

enum class ScheduleToolPage { MAIN, START_DATE, END_DATE, WINDOW_START, WINDOW_END, OFFSET }

@Composable
fun ScheduleToolsScreen(viewModel: ScheduleToolsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    val config = LocalAppConfig.current
    var applySuccess by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.completed.collect { applySuccess = true }
    }
    when (val current = state) {
        ScheduleToolsState.Loading -> ScreenScaffold { }
        is ScheduleToolsState.Error -> SimpleMessageScreen("批量日程工具", current.message)
        is ScheduleToolsState.Ready -> {
            val timetable = current.timetable
            val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
            var action by remember { mutableStateOf(BatchAction.SHIFT) }
            var startDate by remember { mutableStateOf(maxOf(today, timetable.semesterStart)) }
            var endDate by remember { mutableStateOf<LocalDate?>(timetable.semesterEnd?.let { maxOf(it, maxOf(today, timetable.semesterStart)) }) }
            var courseIndex by remember { mutableStateOf(0) }
            var offset by remember { mutableStateOf(10) }
            var useWindow by remember { mutableStateOf(false) }
            var windowStart by remember { mutableStateOf(LocalTime(8, 0)) }
            var windowEnd by remember { mutableStateOf(LocalTime(18, 0)) }
            var page by remember { mutableStateOf(ScheduleToolPage.MAIN) }

            BackHandler(enabled = page != ScheduleToolPage.MAIN) { page = ScheduleToolPage.MAIN }

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (targetState == ScheduleToolPage.MAIN) {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    } else {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    }
                },
                label = "schedule_tool_page",
            ) { activePage ->
            when (activePage) {
                ScheduleToolPage.START_DATE -> DatePage(startDate) { picked ->
                    startDate = picked
                    if (endDate != null && endDate!! < picked) endDate = picked
                    page = ScheduleToolPage.MAIN
                }
                ScheduleToolPage.END_DATE -> DatePage(endDate ?: maxOf(today, timetable.semesterStart)) { endDate = it; page = ScheduleToolPage.MAIN }
                ScheduleToolPage.WINDOW_START -> TimePage(windowStart, config.is24HourFormat) { windowStart = it; page = ScheduleToolPage.MAIN }
                ScheduleToolPage.WINDOW_END -> TimePage(windowEnd, config.is24HourFormat) { windowEnd = it; page = ScheduleToolPage.MAIN }
                ScheduleToolPage.OFFSET -> TextEditScreen("移动分钟（负数=前移）", offset.toString()) {
                    offset = it.toIntOrNull()?.coerceIn(-720, 720) ?: offset
                    page = ScheduleToolPage.MAIN
                }
                ScheduleToolPage.MAIN -> {
                    val selectedCourse = timetable.allCourses.getOrNull(courseIndex - 1)
                    val scopeTitle = selectedCourse?.name ?: "全部课程"
                    val valid = (endDate == null || endDate!! >= startDate) && (!useWindow || windowEnd > windowStart) && (action != BatchAction.SHIFT || offset != 0)
                    val scroll = rememberTransformingLazyColumnState()
                    val transform = rememberTransformationSpec()
                    ScreenScaffold(
                        scrollState = scroll,
                        edgeButton = {
                            EdgeButton(
                                enabled = valid,
                                onClick = {
                                    applySuccess = false
                                    viewModel.apply(
                                        action, startDate, endDate, offset, selectedCourse?.id,
                                        if (useWindow) windowStart else null,
                                        if (useWindow) windowEnd else null,
                                    )
                                },
                            ) { Icon(Icons.Rounded.Check, contentDescription = "应用") }
                        },
                    ) { padding ->
                        TransformingLazyColumn(state = scroll, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                            item {
                                ListHeader(
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                                    transformation = SurfaceTransformation(transform),
                                ) { Text("批量调时 / 停课") }
                            }
                            if (applySuccess) item {
                                OneUiCapsuleSurface(
                                    title = "已应用",
                                    subtitle = "更改已写入课表，可返回首页查看实际时间",
                                    icon = Icons.Rounded.Check,
                                    selected = true,
                                    onClick = { applySuccess = false },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = "今天全部停课",
                                    subtitle = "临时放假快捷操作，仅影响今天",
                                    icon = Icons.Rounded.EventBusy,
                                    emphasize = true,
                                    onClick = {
                                        applySuccess = false
                                        viewModel.apply(BatchAction.CANCEL, today, today, 0, null, null, null)
                                    },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = when (action) {
                                        BatchAction.SHIFT -> "统一移动 ${if (offset > 0) "+" else ""}$offset 分钟"
                                        BatchAction.CANCEL -> "停课"
                                        BatchAction.RESTORE -> "恢复正常"
                                    },
                                    subtitle = "点按切换：移动 → 停课 → 恢复",
                                    icon = when (action) {
                                        BatchAction.SHIFT -> Icons.Rounded.Schedule
                                        BatchAction.CANCEL -> Icons.Rounded.EventBusy
                                        BatchAction.RESTORE -> Icons.Rounded.Restore
                                    },
                                    onClick = { action = BatchAction.entries[(action.ordinal + 1) % BatchAction.entries.size] },
                                    onLongClick = { if (action == BatchAction.SHIFT) page = ScheduleToolPage.OFFSET },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            if (action == BatchAction.SHIFT) item {
                                OneUiCapsuleSurface(
                                    title = "移动分钟：${if (offset > 0) "+" else ""}$offset",
                                    subtitle = "点按输入；负数前移，正数后移",
                                    icon = Icons.Rounded.Tune,
                                    onClick = { page = ScheduleToolPage.OFFSET },
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = "范围：$scopeTitle",
                                    subtitle = "点按循环选择全部/单门课程",
                                    onClick = { courseIndex = (courseIndex + 1) % (timetable.allCourses.size + 1) },
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
                                    subtitle = if (useWindow) "点按关闭；长按设置开始时间" else "点按限制到某一时间段",
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
