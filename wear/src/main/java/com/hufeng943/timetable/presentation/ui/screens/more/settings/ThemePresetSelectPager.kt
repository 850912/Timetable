package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.hufeng943.timetable.R
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
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.theme.ThemePreset
import kotlinx.coroutines.launch

@Composable
fun ThemePresetSelectPager(currentPreset: ThemePreset, onPresetSelect: (ThemePreset) -> Unit) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val scope = rememberCoroutineScope()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(onClick = { scope.launch { scrollState.animateScrollToItem(0) } }) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(state = scrollState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text(stringResource(R.string.settings_theme_style)) }
            }
            items(ThemePreset.entries, key = { it.name }) { preset ->
                OneUiCapsuleSurface(
                    title = themePresetLabel(preset),
                    subtitle = stringResource(when (preset) {
                        ThemePreset.AMOLED_BLACK -> R.string.theme_summary_amoled
                        ThemePreset.SYSTEM_DYNAMIC -> R.string.theme_summary_dynamic
                        ThemePreset.GRAPHITE -> R.string.theme_summary_graphite
                        ThemePreset.AURORA -> R.string.theme_summary_aurora
                        else -> R.string.theme_summary_default
                    }),
                    icon = Icons.Rounded.Palette,
                    selected = preset == currentPreset,
                    emphasize = preset == currentPreset,
                    onClick = { onPresetSelect(preset) },
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }
        }
    }
}

@Composable
fun themePresetLabel(preset: ThemePreset): String = stringResource(when (preset) {
    ThemePreset.AMOLED_BLACK -> R.string.theme_amoled_black
    ThemePreset.DEEP_BLUE -> R.string.theme_deep_blue
    ThemePreset.CYAN_TEAL -> R.string.theme_cyan_teal
    ThemePreset.ROYAL_PURPLE -> R.string.theme_royal_purple
    ThemePreset.SUNSET_ORANGE -> R.string.theme_sunset_orange
    ThemePreset.SAKURA_PINK -> R.string.theme_sakura_pink
    ThemePreset.GRAPHITE -> R.string.theme_graphite
    ThemePreset.AURORA -> R.string.theme_aurora
    ThemePreset.SYSTEM_DYNAMIC -> R.string.theme_system_dynamic
})
