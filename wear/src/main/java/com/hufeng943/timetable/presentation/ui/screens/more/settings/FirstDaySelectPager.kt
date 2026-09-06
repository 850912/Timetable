package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.R
import com.hufeng943.timetable.data.FirstDayOfTheWeek
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

@Composable
fun FirstDaySelectPager(config: AppConfig, onFirstDaySelect: (FirstDayOfTheWeek) -> Unit) {
    val values = listOf(
        FirstDayOfTheWeek.SYSTEM to stringResource(R.string.settings_first_day_system),
        FirstDayOfTheWeek.MONDAY to stringResource(R.string.settings_first_day_monday),
        FirstDayOfTheWeek.SUNDAY to stringResource(R.string.settings_first_day_sunday),
        FirstDayOfTheWeek.SATURDAY to stringResource(R.string.settings_first_day_saturday)
    )
    val current = config.firstDayOfTheWeekSetting
    val initialIndex = values.indexOfFirst { it.first == current }.coerceAtLeast(0) + 1
    val scrollState = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text(stringResource(R.string.settings_first_day)) }
            }
            items(values) { (value, label) ->
                OneUiCapsuleSurface(
                    title = label,
                    icon = Icons.Rounded.DateRange,
                    selected = value == current,
                    onClick = { onFirstDaySelect(value) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }
        }
    }
}
