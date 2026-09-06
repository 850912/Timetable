package com.hufeng943.timetable.presentation.ui.screens.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import kotlinx.datetime.DayOfWeek
import java.time.format.TextStyle

@Composable
fun DayOfWeekSelectionScreen(initialDay: DayOfWeek?, onDaySelected: (DayOfWeek) -> Unit) {
    val firstDay = LocalAppConfig.current.effectiveFirstDayOfTheWeek
    val days = remember(firstDay) { DayOfWeek.entries.sortedBy { (it.ordinal - firstDay.ordinal + 7) % 7 } }
    val initialIndex = remember(days, initialDay) { days.indexOf(initialDay).coerceAtLeast(0) + 1 }
    val scrollState = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(scrollState),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollState),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text(stringResource(R.string.selection_week)) }
            }
            items(days) { day ->
                OneUiCapsuleSurface(
                    title = day.toDisplayString(TextStyle.FULL_STANDALONE),
                    icon = Icons.Rounded.DateRange,
                    selected = day == initialDay,
                    onClick = { onDaySelected(day) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }
        }
    }
}
