package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleAdjustmentState
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleAdjustmentViewModel
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.todayIn
import java.time.format.TextStyle
import kotlin.time.Clock

enum class DayArrangementPage { MAIN, DATE }

@Composable
fun DayArrangementScreen(viewModel: ScheduleAdjustmentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.completed.collect { nav.popSafe() }
    }

    when (val current = state) {
        ScheduleAdjustmentState.Loading -> ScreenScaffold { }
        is ScheduleAdjustmentState.Error -> SimpleMessageScreen("调休", current.message)
        is ScheduleAdjustmentState.Ready -> {
            val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
            var tableIndex by remember { mutableIntStateOf(0) }
            var targetDate by remember { mutableStateOf(today) }
            var sourceDay by remember { mutableStateOf(nextDifferentDay(today.dayOfWeek)) }
            var page by remember { mutableStateOf(DayArrangementPage.MAIN) }
            BackHandler(enabled = page != DayArrangementPage.MAIN) { page = DayArrangementPage.MAIN }

            if (page == DayArrangementPage.DATE) {
                ScreenScaffold(timeText = {}) {
                    DatePicker(
                        initialDate = targetDate.toJavaLocalDate(),
                        onDatePicked = {
                            targetDate = it.toKotlinLocalDate()
                            if (sourceDay == targetDate.dayOfWeek) sourceDay = nextDifferentDay(sourceDay)
                            page = DayArrangementPage.MAIN
                        },
                    )
                }
                return
            }

            val table = current.timetables[tableIndex.coerceIn(0, current.timetables.lastIndex)]
            val scroll = rememberTransformingLazyColumnState()
            val transform = rememberTransformationSpec()
            ScreenScaffold(
                scrollState = scroll,
                edgeButton = {
                    EdgeButton(
                        enabled = sourceDay != targetDate.dayOfWeek,
                        onClick = { viewModel.applyDayArrangement(table.timetableId, targetDate, sourceDay) },
                    ) { Icon(Icons.Rounded.Check, contentDescription = "应用调休") }
                },
            ) { padding ->
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
                        ) { Text("调休") }
                    }
                    if (current.timetables.size > 1) {
                        item {
                            OneUiCapsuleSurface(
                                title = "课表：${table.semesterName}",
                                subtitle = "点按切换要应用调休的课表",
                                onClick = { tableIndex = (tableIndex + 1) % current.timetables.size },
                                modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                            )
                        }
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "目标：${targetDate.toDisplayString()}",
                            subtitle = "${targetDate.dayOfWeek.toDisplayString(TextStyle.FULL)} · 点按选择日期",
                            icon = Icons.Rounded.DateRange,
                            onClick = { page = DayArrangementPage.DATE },
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "上${sourceDay.toDisplayString(TextStyle.FULL)}的课",
                            subtitle = "点按切换星期 · 应用后仅替换目标日期",
                            icon = Icons.Rounded.SwapHoriz,
                            emphasize = true,
                            onClick = {
                                do {
                                    sourceDay = DayOfWeek.entries[(sourceDay.ordinal + 1) % 7]
                                } while (sourceDay == targetDate.dayOfWeek)
                            },
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "效果预览",
                            subtitle = "${targetDate.toDisplayString()}（${targetDate.dayOfWeek.toDisplayString(TextStyle.SHORT)}）上${sourceDay.toDisplayString(TextStyle.SHORT)}的课",
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                }
            }
        }
    }
}

private fun nextDifferentDay(day: DayOfWeek): DayOfWeek = DayOfWeek.entries[(day.ordinal + 1) % 7]
