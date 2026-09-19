package com.hufeng943.timetable.presentation.ui.screens.edit.timetable

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.ui.TimetableUi
import com.hufeng943.timetable.presentation.ui.components.edit.EditTimetableCard
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

@Composable
fun TimetableListPager(
    timetables: List<TimetableUi>,
    onAddTimetable: () -> Unit,
    onTimetableClick: (Long) -> Unit,
    onTimetableLongClick: (Long) -> Unit,
    onScheduleToolsClick: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(onClick = onAddTimetable) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.edit_timetable_add)
                )
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(scrollState),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollState, hapticFeedbackEnabled = false),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(stringResource(R.string.edit_timetable_title))
                }
            }
            if (timetables.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.edit_timetable_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            
                            .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(timetables, key = { it.timetableId }) { timetable ->
                    EditTimetableCard(
                        onTimetableClick = onTimetableClick,
                        timetable = timetable,
                        onTimetableLongClick = onTimetableLongClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }
                item(key = "schedule_tools") {
                    OneUiCapsuleSurface(
                        title = "批量日程工具",
                        subtitle = "批量调时、停课、恢复日程",
                        icon = Icons.Rounded.Build,
                        onClick = onScheduleToolsClick,
                        modifier = Modifier.fillMaxWidth()
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                    )
                }
            }
        }
    }
}
