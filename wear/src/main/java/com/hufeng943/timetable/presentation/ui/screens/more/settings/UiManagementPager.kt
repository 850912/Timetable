package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
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
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.common.TimetableBackgroundMode
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleButton
import com.hufeng943.timetable.presentation.ui.components.OneUiInfoCapsule
import com.hufeng943.timetable.presentation.ui.components.OneUiSwitchCapsule
import com.hufeng943.timetable.presentation.ui.theme.ThemePreset

/** Central home for every visual/UI preference. */
@Composable
fun UiManagementPager(
    config: AppConfig,
    currentThemePreset: ThemePreset,
    onThemeSelectClick: () -> Unit,
    onBackgroundSelectClick: () -> Unit,
    onLiquidGlassAdvancedClick: () -> Unit,
    onDynamicColorToggle: (Boolean) -> Unit,
    onShowTopTimeToggle: (Boolean) -> Unit,
    onUiAnimationsToggle: (Boolean) -> Unit,
    onConditionalUiToggle: (Boolean) -> Unit,
) {
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()

    fun backgroundLabel(): String = when (config.timetableBackgroundMode) {
        TimetableBackgroundMode.SOLID -> "纯色"
        TimetableBackgroundMode.THEME -> "主题光晕"
        TimetableBackgroundMode.IMAGE -> "自定义图片"
    }

    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(state = state, contentPadding = padding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text("UI 管理") }
            }
            item {
                OneUiInfoCapsule(
                    icon = Icons.Rounded.Tune,
                    text = "主题、背景、玻璃与动效。",
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    maxLines = 3,
                )
            }
            item {
                OneUiCapsuleButton(
                    icon = Icons.Rounded.Palette,
                    label = "主题风格",
                    secondaryLabel = currentThemePreset.title,
                    onClick = onThemeSelectClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleButton(
                    icon = Icons.Rounded.Wallpaper,
                    label = stringResource(R.string.settings_timetable_background),
                    secondaryLabel = backgroundLabel(),
                    onClick = onBackgroundSelectClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleButton(
                    icon = Icons.Rounded.BlurOn,
                    label = stringResource(R.string.settings_liquid_glass),
                    secondaryLabel = if (config.isLiquidGlassEnabled) "液态 · ${config.liquidGlassEffect.name.lowercase()}" else "已关闭",
                    onClick = onLiquidGlassAdvancedClick,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiSwitchCapsule(
                    title = stringResource(R.string.settings_dynamic_color),
                    subtitle = stringResource(R.string.settings_dynamic_color_summary),
                    icon = Icons.Rounded.ColorLens,
                    checked = config.isDynamicColorEnabled,
                    onCheckedChange = onDynamicColorToggle,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiSwitchCapsule(
                    title = "界面动效",
                    subtitle = "按压与切换动画",
                    icon = Icons.Rounded.Animation,
                    checked = config.uiAnimationsEnabled,
                    onCheckedChange = onUiAnimationsToggle,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiSwitchCapsule(
                    title = "条件 UI",
                    subtitle = "自动隐藏无效选项",
                    icon = Icons.Rounded.AutoAwesome,
                    checked = config.conditionalUiEnabled,
                    onCheckedChange = onConditionalUiToggle,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiSwitchCapsule(
                    title = "顶部时间显示",
                    subtitle = "显示应用内时间",
                    icon = Icons.Rounded.Schedule,
                    checked = config.isShowTopTime,
                    onCheckedChange = onShowTopTimeToggle,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
