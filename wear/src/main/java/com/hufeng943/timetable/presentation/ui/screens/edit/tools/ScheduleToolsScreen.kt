package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.WearInternalNavHost
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.ui.components.WearDatePickerPage
import com.hufeng943.timetable.presentation.ui.components.WearTimePickerPage
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.*
import kotlinx.datetime.*
import kotlin.time.Clock

private object QuickRoutes { const val MAIN="main"; const val START="start"; const val END="end"; const val WS="ws"; const val WE="we"; const val DAYS="days"; const val OFFSET="offset" }

@Composable
fun ScheduleToolsScreen(
    fixedTimetableId: Long? = null,
    onCompleted: (() -> Unit)? = null,
    viewModel: ScheduleToolsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val outerNav = LocalNavController.current
    val config = LocalAppConfig.current
    LaunchedEffect(viewModel, onCompleted) {
        viewModel.completed.collect { onCompleted?.invoke() ?: outerNav.popSafe() }
    }
    when (val current = state) {
        ScheduleToolsState.Loading -> ScreenScaffold(timeText = {}) {}
        is ScheduleToolsState.Error -> SimpleMessageScreen("批量日程工具", current.message)
        is ScheduleToolsState.Ready -> {
            val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
            var start by remember { mutableStateOf(today) }
            var end by remember { mutableStateOf<LocalDate?>(null) }
            var offset by remember { mutableIntStateOf(10) }
            var useWindow by remember { mutableStateOf(false) }
            var ws by remember { mutableStateOf(LocalTime(8, 0)) }
            var we by remember { mutableStateOf(LocalTime(18, 0)) }
            var action by remember { mutableStateOf(BatchAction.SHIFT) }
            val nav = rememberSwipeDismissableNavController()

            WearInternalNavHost(navController = nav, startDestination = QuickRoutes.MAIN) {
                composable(QuickRoutes.MAIN) {
                    val valid = (end?.let { it >= start } ?: true) &&
                        (!useWindow || we > ws) &&
                        (action != BatchAction.SHIFT || offset != 0)
                    val scroll = rememberTransformingLazyColumnState()
                    val transform = rememberTransformationSpec()
                    ScreenScaffold(
                        scrollState = scroll,
                        timeText = {},
                        edgeButton = {
                            EdgeButton(
                                enabled = valid,
                                onClick = {
                                    viewModel.apply(
                                        action = action,
                                        startDate = start,
                                        endDate = end,
                                        offsetMinutes = offset,
                                        timetableId = null,
                                        courseId = null,
                                        timeWindowStart = if (useWindow) ws else null,
                                        timeWindowEnd = if (useWindow) we else null,
                                    )
                                },
                            ) { Icon(Icons.Rounded.Check, "确认") }
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
                                    subtitle = "从 ${start.toDisplayString()} 起 · 点按选择天数",
                                    icon = Icons.Rounded.EventBusy,
                                    emphasize = true,
                                    onClick = { nav.navigateSingle(QuickRoutes.DAYS) },
                                    modifier = Modifier.fillMaxWidth()
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = when (action) {
                                        BatchAction.SHIFT -> if (offset < 0) "统一提前 ${-offset} 分钟" else "统一延时 $offset 分钟"
                                        BatchAction.CANCEL -> "停课"
                                        BatchAction.RESTORE -> "恢复正常"
                                    },
                                    subtitle = "点按切换操作；长按调时",
                                    icon = when (action) {
                                        BatchAction.SHIFT -> Icons.Rounded.Schedule
                                        BatchAction.CANCEL -> Icons.Rounded.EventBusy
                                        BatchAction.RESTORE -> Icons.Rounded.Restore
                                    },
                                    onClick = { action = BatchAction.entries[(action.ordinal + 1) % BatchAction.entries.size] },
                                    onLongClick = { nav.navigateSingle(QuickRoutes.OFFSET) },
                                    modifier = Modifier.fillMaxWidth()
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            if (action == BatchAction.SHIFT) item {
                                OneUiCapsuleSurface(
                                    title = "提前 / 延时",
                                    subtitle = if (offset < 0) "提前 ${-offset} 分钟" else "延时 $offset 分钟",
                                    icon = Icons.Rounded.Tune,
                                    onClick = { nav.navigateSingle(QuickRoutes.OFFSET) },
                                    modifier = Modifier.fillMaxWidth()
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = "开始：${start.toDisplayString()}",
                                    subtitle = "点按选择生效日期",
                                    onClick = { nav.navigateSingle(QuickRoutes.START) },
                                    modifier = Modifier.fillMaxWidth()
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = "结束：${end?.toDisplayString() ?: "永不结束"}",
                                    subtitle = "点按选择；长按永久",
                                    onClick = { nav.navigateSingle(QuickRoutes.END) },
                                    onLongClick = { end = null },
                                    modifier = Modifier.fillMaxWidth()
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            item {
                                OneUiCapsuleSurface(
                                    title = if (useWindow) "仅处理 ${ws.toDisplayString(config.is24HourFormat)}–${we.toDisplayString(config.is24HourFormat)}" else "全部时段",
                                    subtitle = "点按开关；长按设开始",
                                    selected = useWindow,
                                    onClick = { useWindow = !useWindow },
                                    onLongClick = { useWindow = true; nav.navigateSingle(QuickRoutes.WS) },
                                    modifier = Modifier.fillMaxWidth()
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                            if (useWindow) item {
                                OneUiCapsuleSurface(
                                    title = "时间窗结束：${we.toDisplayString(config.is24HourFormat)}",
                                    subtitle = "点按修改",
                                    onClick = { nav.navigateSingle(QuickRoutes.WE) },
                                    modifier = Modifier.fillMaxWidth()
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                        }
                    }
                }
                composable(QuickRoutes.START) { DatePage(start) { selected -> start = selected; if (end?.let { it < selected } == true) end = selected; nav.popSafe() } }
                composable(QuickRoutes.END) { DatePage(end ?: start) { end = it; nav.popSafe() } }
                composable(QuickRoutes.WS) { TimePage(ws, config.is24HourFormat) { ws = it; nav.popSafe() } }
                composable(QuickRoutes.WE) { TimePage(we, config.is24HourFormat) { we = it; nav.popSafe() } }
                composable(QuickRoutes.DAYS) {
                    ValuePickerPage("临时放假", (1..30).toList(), 1, { "$it 天" }) { days ->
                        viewModel.apply(
                            BatchAction.CANCEL,
                            start,
                            start.plus(days - 1, DateTimeUnit.DAY),
                            0,
                            null,
                            null,
                            null,
                            null,
                        )
                    }
                }
                composable(QuickRoutes.OFFSET) {
                    ValuePickerPage(
                        "统一提前 / 延时",
                        (-30..30).filter { it != 0 },
                        offset.coerceIn(-30, 30),
                        { if (it < 0) "提前 ${-it} 分钟" else "延时 $it 分钟" },
                    ) { offset = it; nav.popSafe() }
                }
            }
        }
    }
}

@Composable private fun ValuePickerPage(title:String,values:List<Int>,initial:Int,label:(Int)->String,onConfirm:(Int)->Unit){
    var selected by remember(initial){mutableIntStateOf(initial)};val state=rememberTransformingLazyColumnState(initialAnchorItemIndex=(values.indexOf(initial).coerceAtLeast(0)+1));val transform=rememberTransformationSpec()
    ScreenScaffold(scrollState=state,timeText={},edgeButton={EdgeButton(onClick={onConfirm(selected)}){Icon(Icons.Rounded.Check,"确认")}}){padding->TransformingLazyColumn(state=state,contentPadding=padding,modifier=Modifier.fillMaxSize()){
        item{ListHeader(modifier=Modifier.fillMaxWidth().minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(title)}}
        values.forEach{v->item(key=v){OneUiCapsuleSurface(title=label(v),selected=v==selected,onClick={selected=v},modifier=Modifier.fillMaxWidth().minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}}
    }}
}
@Composable private fun DatePage(initial:LocalDate,onPicked:(LocalDate)->Unit){WearDatePickerPage(initialDate=initial.toJavaLocalDate(),onDatePicked={onPicked(it.toKotlinLocalDate())})}
@Composable private fun TimePage(initial:LocalTime,is24:Boolean,onPicked:(LocalTime)->Unit){WearTimePickerPage(initialTime=initial.toJavaLocalTime(),onTimePicked={onPicked(it.toKotlinLocalTime())},timePickerType=if(is24)TimePickerType.HoursMinutes24H else TimePickerType.HoursMinutesAmPm12H)}
