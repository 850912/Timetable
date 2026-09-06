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
import java.time.format.TextStyle

@Composable
fun EditTimeSlotMainPager(
    timeSlot: TimeSlotUi,
    onSave: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onDayOfWeekClick: () -> Unit,
    onRecurrenceClick: () -> Unit,
    onRemarkClick: () -> Unit,
    onRemarkLongClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val canSave = timeSlot.startTime != null && timeSlot.endTime != null && timeSlot.dayOfWeek != null

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(onClick = onSave, enabled = canSave) {
                Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.check))
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(state = scrollState, contentPadding = contentPadding) {
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

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.edit_timeslot_start),
                    subtitle = timeSlot.startTime?.toString() ?: stringResource(R.string.not_set),
                    icon = Icons.Rounded.AccessTime,
                    emphasize = timeSlot.startTime == null,
                    onClick = onStartTimeClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.edit_timeslot_end),
                    subtitle = timeSlot.endTime?.toString() ?: stringResource(R.string.not_set),
                    icon = Icons.Rounded.AccessTime,
                    emphasize = timeSlot.endTime == null,
                    onClick = onEndTimeClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.edit_timeslot_week),
                    subtitle = timeSlot.dayOfWeek?.toDisplayString(TextStyle.FULL_STANDALONE) ?: stringResource(R.string.not_set),
                    icon = Icons.Rounded.DateRange,
                    emphasize = timeSlot.dayOfWeek == null,
                    onClick = onDayOfWeekClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.edit_timeslot_repeat),
                    subtitle = timeSlot.recurrence.toDisplayString(),
                    icon = Icons.Rounded.Refresh,
                    onClick = onRecurrenceClick,
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
