package com.hufeng943.timetable.presentation.ui.screens.edit.timeslot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import kotlinx.datetime.DayOfWeek
import java.time.format.TextStyle

/** Multi-selects weekdays. Each selected weekday is saved as an independent TimeSlot row. */
@Composable
fun MultiDateSelectionScreen(initialDays: Set<DayOfWeek>, onConfirm: (Set<DayOfWeek>) -> Unit) {
    val firstDay = LocalAppConfig.current.effectiveFirstDayOfTheWeek
    val days = remember(firstDay) { DayOfWeek.entries.sortedBy { (it.ordinal - firstDay.ordinal + 7) % 7 } }
    var selected by remember(initialDays) { mutableStateOf(initialDays) }
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = state,
        timeText = {},
        edgeButton = {
            EdgeButton(onClick = { onConfirm(selected) }, enabled = selected.isNotEmpty()) {
                Icon(Icons.Rounded.Check, contentDescription = "确认")
            }
        }
    ) { padding ->
        TransformingLazyColumn(state = state, contentPadding = padding, modifier = Modifier.fillMaxSize(), rotaryScrollableBehavior = androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults.behavior(state, hapticFeedbackEnabled = false)) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text("选择星期（可多选）") }
            }
            items(days, key = { it.toString() }) { day ->
                val checked = day in selected
                OneUiCapsuleSurface(
                    title = day.toDisplayString(TextStyle.FULL_STANDALONE),
                    subtitle = if (checked) "已选择" else "点按选择",
                    icon = Icons.Rounded.DateRange,
                    selected = checked,
                    onClick = { selected = if (checked) selected - day else selected + day },
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
