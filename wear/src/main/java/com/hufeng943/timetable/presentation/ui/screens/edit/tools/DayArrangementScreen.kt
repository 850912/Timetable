package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.*
import kotlinx.datetime.*
import java.time.format.TextStyle
import kotlin.time.Clock

enum class DayArrangementPage { MAIN, TABLE, DATE, SOURCE_DAY }

@Composable
fun DayArrangementScreen(viewModel: ScheduleAdjustmentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    LaunchedEffect(viewModel) { viewModel.completed.collect { nav.popSafe() } }
    when (val current = state) {
        ScheduleAdjustmentState.Loading -> ScreenScaffold(timeText = {}) { }
        is ScheduleAdjustmentState.Error -> SimpleMessageScreen("调休", current.message)
        is ScheduleAdjustmentState.Ready -> {
            val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
            var tableId by remember { mutableLongStateOf(current.timetables.first().timetableId) }
            var date by remember { mutableStateOf(today) }
            var sourceDay by remember { mutableStateOf(nextDifferentDay(today.dayOfWeek)) }
            var page by remember { mutableStateOf(DayArrangementPage.MAIN) }
            BackHandler(enabled = page != DayArrangementPage.MAIN) { page = DayArrangementPage.MAIN }
            val table = current.timetables.firstOrNull { it.timetableId == tableId } ?: current.timetables.first()

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (targetState == DayArrangementPage.MAIN) {
                        slideInHorizontally { -it / 3 } togetherWith slideOutHorizontally { it }
                    } else {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
                    }
                },
                label = "DayArrangementPageTransition",
            ) { visiblePage ->
                when (visiblePage) {
                DayArrangementPage.TABLE -> TimetablePickerPage(current.timetables, tableId) { tableId = it; page = DayArrangementPage.MAIN }
                DayArrangementPage.DATE -> ScreenScaffold(timeText = {}) {
                    DatePicker(initialDate = date.toJavaLocalDate(), onDatePicked = {
                        date = it.toKotlinLocalDate(); if (sourceDay == date.dayOfWeek) sourceDay = nextDifferentDay(sourceDay); page = DayArrangementPage.MAIN
                    })
                }
                DayArrangementPage.SOURCE_DAY -> DayOfWeekPicker(sourceDay, date.dayOfWeek) { sourceDay = it; page = DayArrangementPage.MAIN }
                DayArrangementPage.MAIN -> {
                    val scroll = rememberTransformingLazyColumnState(); val transform = rememberTransformationSpec()
                    ScreenScaffold(
                        scrollState = scroll,
                        timeText = {},
                        edgeButton = { EdgeButton(enabled = sourceDay != date.dayOfWeek, onClick = { viewModel.applyDayArrangement(tableId, date, sourceDay) }) { Icon(Icons.Rounded.Check, "确认") } }
                    ) { padding ->
                        TransformingLazyColumn(state = scroll, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                            item { ListHeader(modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding), transformation = SurfaceTransformation(transform)) { Text("调休") } }
                            item { OneUiCapsuleSurface(title = "课表：${table.semesterName}", subtitle = "点按进入课表/课程/课时选择", onClick = { page = DayArrangementPage.TABLE }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) }
                            item { OneUiCapsuleSurface(title = "日期：${date.toDisplayString()}", subtitle = "${date.dayOfWeek.toDisplayString(TextStyle.FULL)} · 点按打开日期选择", icon = Icons.Rounded.DateRange, onClick = { page = DayArrangementPage.DATE }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) }
                            item { OneUiCapsuleSurface(title = "改上：${sourceDay.toDisplayString(TextStyle.FULL)}的课", subtitle = "点按选择来源星期", icon = Icons.Rounded.SwapHoriz, onClick = { page = DayArrangementPage.SOURCE_DAY }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) }
                            item { OneUiCapsuleSurface(title = "恢复调休/换课", subtitle = "恢复此课表由调休、课程调节产生的修改", icon = Icons.Rounded.Restore, onClick = { viewModel.restoreAdjustments(tableId) }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun DayOfWeekPicker(initial: DayOfWeek, excluded: DayOfWeek, onSelect: (DayOfWeek) -> Unit) {
    val state = rememberTransformingLazyColumnState(); val transform = rememberTransformationSpec()
    ScreenScaffold(scrollState = state, timeText = {}) { padding ->
        TransformingLazyColumn(state = state, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding), transformation = SurfaceTransformation(transform)) { Text("选择来源星期") } }
            DayOfWeek.entries.filter { it != excluded }.forEach { day -> item { OneUiCapsuleSurface(title = day.toDisplayString(TextStyle.FULL), selected = day == initial, onClick = { onSelect(day) }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) } }
        }
    }
}
private fun nextDifferentDay(day: DayOfWeek): DayOfWeek = DayOfWeek.entries[(day.ordinal + 1) % 7]
