package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.WearDatePickerPage
import com.hufeng943.timetable.presentation.ui.components.WearWheelPickerPage
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleAdjustmentState
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleAdjustmentViewModel
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.format.TextStyle

@Composable
private fun DayArrangementCompletionEffect(viewModel: ScheduleAdjustmentViewModel) {
    val nav = LocalNavController.current
    LaunchedEffect(viewModel) {
        viewModel.completed.collect {
            nav.popBackStack(NavRoutes.MORE_DAY_ARRANGEMENT, inclusive = true)
        }
    }
}

@Composable
fun DayArrangementScreen(viewModel: ScheduleAdjustmentViewModel) {
    DayArrangementCompletionEffect(viewModel)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editor by viewModel.dayEditor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current

    when (val current = state) {
        ScheduleAdjustmentState.Loading -> ScreenScaffold(timeText = {}) {}
        is ScheduleAdjustmentState.Error -> SimpleMessageScreen("调休", current.message)
        is ScheduleAdjustmentState.Ready -> {
            val scroll = rememberTransformingLazyColumnState()
            val transform = rememberTransformationSpec()
            ScreenScaffold(
                scrollState = scroll,
                timeText = {},
                edgeButton = {
                    EdgeButton(
                        enabled = editor.sourceDay != editor.date.dayOfWeek,
                        onClick = { viewModel.applyDayArrangement(null, editor.date, editor.sourceDay) },
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
                        ) { Text("调休") }
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
                            title = "日期：${editor.date.toDisplayString()}",
                            subtitle = "${editor.date.dayOfWeek.toDisplayString(TextStyle.FULL)} · 点按选日期",
                            icon = Icons.Rounded.DateRange,
                            onClick = { nav.navigateSingle(NavRoutes.MORE_DAY_ARRANGEMENT_DATE) },
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "改上：${editor.sourceDay.toDisplayString(TextStyle.FULL)}的课",
                            subtitle = "选择来源星期",
                            icon = Icons.Rounded.SwapHoriz,
                            onClick = { nav.navigateSingle(NavRoutes.MORE_DAY_ARRANGEMENT_SOURCE) },
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "恢复调休/换课",
                            subtitle = "恢复调休与课程调节",
                            icon = Icons.Rounded.Restore,
                            onClick = { viewModel.restoreAdjustments() },
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayArrangementDateScreen(viewModel: ScheduleAdjustmentViewModel) {
    val editor by viewModel.dayEditor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    WearDatePickerPage(
        initialDate = editor.date.toJavaLocalDate(),
        onDatePicked = {
            viewModel.updateDayArrangementDate(it.toKotlinLocalDate())
            nav.popSafe()
        },
    )
}

@Composable
fun DayArrangementSourceScreen(viewModel: ScheduleAdjustmentViewModel) {
    val editor by viewModel.dayEditor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    DayOfWeekPicker(editor.sourceDay, editor.date.dayOfWeek) {
        viewModel.updateDayArrangementSource(it)
        nav.popSafe()
    }
}

@Composable
private fun DayOfWeekPicker(
    initial: DayOfWeek,
    excluded: DayOfWeek,
    onSelect: (DayOfWeek) -> Unit,
) {
    val values = remember(excluded) { DayOfWeek.entries.filter { it != excluded } }
    WearWheelPickerPage(
        title = "选择来源星期",
        values = values,
        initial = initial.takeIf { it in values } ?: values.first(),
        label = { it.toDisplayString(TextStyle.FULL) },
        onConfirm = onSelect,
    )
}
