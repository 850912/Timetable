package com.hufeng943.timetable.presentation.ui.screens.edit.timeslot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
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
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.time.format.TextStyle

/**
 * Multi-select date picker embedded into normal time-slot creation.
 * The anchor only controls the visible 84-day window; selected dates are retained
 * when moving the window.
 */
@Composable
fun MultiDateSelectionScreen(
    anchorDate: LocalDate,
    selectedDates: Set<LocalDate>,
    onAnchorClick: () -> Unit,
    onToggleDate: (LocalDate) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val candidates = remember(anchorDate) {
        val start = anchorDate.minus(14, DateTimeUnit.DAY)
        (0 until 84).map { start.plus(it, DateTimeUnit.DAY) }
    }

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(onClick = onDone) {
                Icon(Icons.Rounded.Check, contentDescription = "完成")
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                ) { Text("选择日期 · 可多选") }
            }
            item {
                OneUiCapsuleSurface(
                    title = "日期窗口：${anchorDate.toDisplayString()}",
                    subtitle = "点按跳转到其他日期，已选日期不会丢失",
                    icon = Icons.Rounded.DateRange,
                    onClick = onAnchorClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            if (selectedDates.isNotEmpty()) {
                item {
                    OneUiCapsuleSurface(
                        title = "已选 ${selectedDates.size} 天",
                        subtitle = "长按清空全部已选日期",
                        icon = Icons.Rounded.DeleteSweep,
                        selected = true,
                        onClick = {},
                        onLongClick = onClear,
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    )
                }
            }
            items(candidates, key = { it.toEpochDays() }) { date ->
                val selected = date in selectedDates
                OneUiCapsuleSurface(
                    title = date.toDisplayString(),
                    subtitle = date.dayOfWeek.toDisplayString(TextStyle.FULL),
                    selected = selected,
                    onClick = { onToggleDate(date) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
