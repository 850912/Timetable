package com.hufeng943.timetable.presentation.ui.screens.edit.timetable

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.hufeng943.timetable.presentation.ui.common.ui.TimetableUi
import com.hufeng943.timetable.presentation.ui.components.ColorPickerCard
import com.hufeng943.timetable.presentation.ui.components.DeleteButton
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString

@Composable
fun EditTimetableMainPager(
    timetable: TimetableUi,
    onSave: () -> Unit,
    onNameClick: () -> Unit,
    onStartDateClick: () -> Unit,
    onStartDateLongClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onEndDateLongClick: () -> Unit,
    onColorClick: () -> Unit,
    onColorLongClick: () -> Unit,
    onDelete: () -> Unit,
    startDateIsToday: Boolean
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(onClick = onSave) {
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
                    AnimatedContent(targetState = timetable.timetableId == 0L, label = "header_text") { isAdd ->
                        Text(if (isAdd) stringResource(R.string.edit_timetable_add) else stringResource(R.string.edit_timetable_edit))
                    }
                }
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.edit_timetable_name),
                    subtitle = timetable.displayName,
                    icon = Icons.Rounded.CollectionsBookmark,
                    onClick = onNameClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.edit_timetable_start),
                    subtitle = timetable.semesterStart.toDisplayString() + if (!startDateIsToday) " · ${stringResource(R.string.set_current_date_long_press)}" else "",
                    icon = Icons.Rounded.CalendarToday,
                    onClick = onStartDateClick,
                    onLongClick = onStartDateLongClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.edit_timetable_end),
                    subtitle = (timetable.semesterEnd?.toDisplayString() ?: stringResource(R.string.never_ends)) +
                        if (timetable.semesterEnd != null) " · ${stringResource(R.string.set_never_ends_long_press)}" else "",
                    icon = Icons.Rounded.CalendarMonth,
                    onClick = onEndDateClick,
                    onLongClick = onEndDateLongClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                ColorPickerCard(
                    label = stringResource(R.string.edit_timetable_color),
                    color = timetable.displayColor,
                    onClick = onColorClick,
                    onLongClick = onColorLongClick,
                    isNull = timetable.color == Color.Unspecified,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            if (timetable.timetableId != 0L) {
                item {
                    DeleteButton(
                        label = stringResource(R.string.edit_timetable_delete),
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
