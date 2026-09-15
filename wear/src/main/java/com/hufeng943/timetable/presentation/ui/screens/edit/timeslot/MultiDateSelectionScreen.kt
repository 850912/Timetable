package com.hufeng943.timetable.presentation.ui.screens.edit.timeslot

import androidx.activity.compose.BackHandler
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
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
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
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.todayIn
import java.time.format.TextStyle
import kotlin.time.Clock

enum class DateSelectionPage { LIST, ANCHOR }

@Composable
fun MultiDateSelectionScreen(
    initialDates: Set<LocalDate>,
    semesterStart: LocalDate? = null,
    semesterEnd: LocalDate? = null,
    onConfirm: (Set<LocalDate>) -> Unit,
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var selectedDates by remember(initialDates) { mutableStateOf(initialDates) }
    var anchorDate by remember(initialDates, semesterStart) {
        mutableStateOf(initialDates.minOrNull() ?: maxOf(today, semesterStart ?: today))
    }
    var page by remember { mutableStateOf(DateSelectionPage.LIST) }

    BackHandler(enabled = page != DateSelectionPage.LIST) { page = DateSelectionPage.LIST }

    if (page == DateSelectionPage.ANCHOR) {
        ScreenScaffold(timeText = {}) {
            DatePicker(
                initialDate = anchorDate.toJavaLocalDate(),
                onDatePicked = {
                    anchorDate = it.toKotlinLocalDate()
                    page = DateSelectionPage.LIST
                },
            )
        }
        return
    }

    val candidates = remember(anchorDate, semesterStart, semesterEnd) {
        (0 until 31).map { anchorDate.plus(it, DateTimeUnit.DAY) }
            .filter { date -> (semesterStart == null || date >= semesterStart) && (semesterEnd == null || date <= semesterEnd) }
    }
    val scrollState = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = { onConfirm(selectedDates) },
                enabled = selectedDates.isNotEmpty(),
            ) { Icon(Icons.Rounded.Check, contentDescription = "确认日期") }
        },
    ) { padding ->
        TransformingLazyColumn(
            state = scrollState,
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(scrollState),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollState),
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text("日期 · 可多选") }
            }
            item {
                OneUiCapsuleSurface(
                    title = "日期窗口：${anchorDate.toDisplayString()}",
                    subtitle = "点按跳到其他日期 · 下方可连续勾选 31 天",
                    icon = Icons.Rounded.DateRange,
                    onClick = { page = DateSelectionPage.ANCHOR },
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
