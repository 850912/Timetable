package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.common.AppPowerSaveMode
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

@Composable
fun PowerSaveModeSelectPager(config: AppConfig, onSelect: (AppPowerSaveMode) -> Unit) {
    val values = listOf(
        AppPowerSaveMode.FOLLOW_SYSTEM to ("跟随系统" to "跟随手表省电模式"),
        AppPowerSaveMode.ALWAYS_ON to ("始终开启" to "降低动效与 GPU 特效，优先续航"),
        AppPowerSaveMode.ALWAYS_OFF to ("始终关闭" to "不主动降级界面效果")
    )
    val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = values.indexOfFirst { it.first == config.powerSaveMode }.coerceAtLeast(0) + 1)
    val transform = rememberTransformationSpec()
    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            item { ListHeader(modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding), transformation = SurfaceTransformation(transform)) { Text("省电模式") } }
            items(values, key = { it.first.name }) { (mode, labels) ->
                OneUiCapsuleSurface(title = labels.first, subtitle = labels.second, icon = Icons.Rounded.BatterySaver, selected = mode == config.powerSaveMode, onClick = { onSelect(mode) }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))
            }
        }
    }
}
