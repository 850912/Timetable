package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.common.AppPowerSaveMode
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.R

@Composable
fun PowerSaveModeSelectPager(config: AppConfig, onSelect: (AppPowerSaveMode) -> Unit) {
    val values = listOf(
        AppPowerSaveMode.FOLLOW_SYSTEM to (stringResource(R.string.settings_power_follow_system) to stringResource(R.string.settings_power_follow_summary)),
        AppPowerSaveMode.ALWAYS_ON to (stringResource(R.string.settings_power_always_on) to stringResource(R.string.settings_power_on_summary)),
        AppPowerSaveMode.ALWAYS_OFF to (stringResource(R.string.settings_power_always_off) to stringResource(R.string.settings_power_off_summary)),
    )
    val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = values.indexOfFirst { it.first == config.powerSaveMode }.coerceAtLeast(0) + 1)
    val transform = rememberTransformationSpec()
    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = padding, rotaryScrollableBehavior = androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults.behavior(state, hapticFeedbackEnabled = false)) {
            item { ListHeader(modifier = Modifier.fillMaxWidth().minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding), transformation = SurfaceTransformation(transform)) { Text(stringResource(R.string.settings_power_save)) } }
            items(values, key = { it.first.name }) { (mode, labels) ->
                OneUiCapsuleSurface(title = labels.first, subtitle = labels.second, icon = Icons.Rounded.BatterySaver, selected = mode == config.powerSaveMode, onClick = { onSelect(mode) }, modifier = Modifier.fillMaxWidth().minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))
            }
        }
    }
}
