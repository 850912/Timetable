package com.hufeng943.timetable.presentation.ui.screens.edit.timeslot

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
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
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.ui.TimeSlotUi
import com.hufeng943.timetable.presentation.ui.components.DeleteButton
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.shared.model.ScheduleOverrideType
import com.hufeng943.timetable.shared.model.WeekPattern
import java.time.format.TextStyle

@Composable
fun EditTimeSlotMainPager(
    timeSlot: TimeSlotUi,
    is24HourFormat: Boolean,
    onSave: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onDatesClick: () -> Unit,
    onDayOfWeekClick: () -> Unit,
    onRecurrenceClick: () -> Unit,
    onRemarkClick: () -> Unit,
    onRemarkLongClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val hasDate = timeSlot.selectedDates.isNotEmpty() || timeSlot.dayOfWeek != null
    val canSave = timeSlot.startTime != null && timeSlot.endTime != null && hasDate
    val temporaryTimeOverrides = timeSlot.overrides.filter { override ->
        override.type != ScheduleOverrideType.CANCELLED &&
            override.startTime != null && override.endTime != null &&
            (override.startTime != timeSlot.startTime || override.endTime != timeSlot.endTime)
    }
    val temporaryTimePairs = temporaryTimeOverrides
        .mapNotNull { override ->
            val start = override.startTime ?: return@mapNotNull null
            val end = override.endTime ?: return@mapNotNull null
            start to end
        }
        .distinct()
    val hasTemporaryTime = temporaryTimePairs.isNotEmpty()

    val dateSubtitle = if (timeSlot.selectedDates.isNotEmpty()) {
        val ordered = timeSlot.selectedDates.sorted()
        when {
            ordered.size == 1 -> ordered.first().toDisplayString()
            ordered.size == 2 -> "${ordered[0].toDisplayString()} · ${ordered[1].toDisplayString()}"
            else -> "${ordered[0].toDisplayString()} · ${ordered[1].toDisplayString()} · 共 ${ordered.size} 天"
        }
    } else {
        timeSlot.dayOfWeek?.toDisplayString(TextStyle.FULL_STANDALONE) ?: stringResource(R.string.not_set)
    }

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(onClick = onSave, enabled = canSave) {
                Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.check))
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(state = scrollState,
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(scrollState),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollState), contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    AnimatedContent(targetState = timeSlot.id == 0L, label = "header_text") { isAdd ->
                        Text(if (isAdd) stringResource(R.string.edit_timeslot_add) else stringResource(R.string.edit_timeslot_edit))
                    }
                }
            }

            if (hasTemporaryTime) {
                item {
                    val first = temporaryTimePairs.first()
                    val timeSummary = if (temporaryTimePairs.size == 1) {
                        "${first.first.toDisplayString(is24HourFormat)}–${first.second.toDisplayString(is24HourFormat)}"
                    } else {
                        "${temporaryTimePairs.size} 组临时时间"
                    }
                    val firstRule = temporaryTimeOverrides.first()
                    val rangeSummary = firstRule.endDate?.let { end ->
                        if (end.year >= 9999) "${firstRule.date.toDisplayString()} 起"
                        else "${firstRule.date.toDisplayString()}–${end.toDisplayString()}"
                    } ?: if (temporaryTimeOverrides.size == 1) {
                        firstRule.date.toDisplayString()
                    } else {
                        "${temporaryTimeOverrides.size} 个日期规则"
                    }
                    OneUiCapsuleSurface(
                        title = "临时调时：$timeSummary",
                        subtitle = "$rangeSummary · 由快捷修改/日期例外生效",
                        icon = Icons.Rounded.AccessTime,
                        selected = true,
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                    )
                }
            }

            item {
                OneUiCapsuleSurface(
                    title = if (hasTemporaryTime) "基础开始时间" else stringResource(R.string.edit_timeslot_start),
                    subtitle = timeSlot.startTime?.toDisplayString(is24HourFormat) ?: stringResource(R.string.not_set),
                    icon = Icons.Rounded.AccessTime,
                    emphasize = timeSlot.startTime == null,
                    onClick = onStartTimeClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = if (hasTemporaryTime) "基础结束时间" else stringResource(R.string.edit_timeslot_end),
                    subtitle = timeSlot.endTime?.toDisplayString(is24HourFormat) ?: stringResource(R.string.not_set),
                    icon = Icons.Rounded.AccessTime,
                    emphasize = timeSlot.endTime == null,
                    onClick = onEndTimeClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = "日期",
                    subtitle = if (timeSlot.selectedDates.isNotEmpty()) {
                        "$dateSubtitle · 点按多选"
                    } else {
                        "$dateSubtitle · 点按具体日期（可多选） · 长按按星期"
                    },
                    icon = Icons.Rounded.DateRange,
                    emphasize = !hasDate,
                    onClick = onDatesClick,
                    onLongClick = onDayOfWeekClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.edit_timeslot_repeat),
                    subtitle = if (timeSlot.recurrence == WeekPattern.DATE_ONLY) "仅所选日期" else timeSlot.recurrence.toDisplayString(),
                    icon = Icons.Rounded.Refresh,
                    onClick = if (timeSlot.recurrence == WeekPattern.DATE_ONLY) ({}) else onRecurrenceClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.edit_timeslot_remark),
                    subtitle = timeSlot.displayRemark + if (timeSlot.remark != null) " · ${stringResource(R.string.clear_long_press)}" else "",
                    icon = Icons.AutoMirrored.Rounded.Notes,
                    onClick = onRemarkClick,
                    onLongClick = onRemarkLongClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            if (timeSlot.id != 0L) {
                item {
                    DeleteButton(
                        label = stringResource(R.string.edit_timeslot_delete),
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }
            }
        }
    }
}
