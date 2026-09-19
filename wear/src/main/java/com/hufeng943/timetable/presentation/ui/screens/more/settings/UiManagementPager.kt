package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.ColorLens
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
    onImageBlurToggle: (Boolean) -> Unit,
    onImageFluidToggle: (Boolean) -> Unit,
) {
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()

    val backgroundLabel = when (config.timetableBackgroundMode) {
        TimetableBackgroundMode.SOLID -> stringResource(R.string.settings_background_solid_short)
        TimetableBackgroundMode.THEME -> stringResource(R.string.settings_background_theme_short)
        TimetableBackgroundMode.IMAGE -> stringResource(R.string.settings_background_image_short)
    }
    val liquidEffectLabel = when (config.liquidGlassEffect) {
        com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect.SOFT -> stringResource(R.string.settings_effect_soft)
        com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect.BALANCED -> stringResource(R.string.settings_effect_balanced)
        com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect.FLUID -> stringResource(R.string.settings_effect_fluid)
    }

    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(state = state, contentPadding = padding, rotaryScrollableBehavior = androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults.behavior(state, hapticFeedbackEnabled = false)) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text(stringResource(R.string.settings_ui_management)) }
            }
            item {
                OneUiInfoCapsule(
                    icon = Icons.Rounded.Tune,
                    text = stringResource(R.string.settings_ui_management_summary),
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    maxLines = 3,
                )
            }
            item {
                OneUiCapsuleButton(
                    icon = Icons.Rounded.Palette,
                    label = stringResource(R.string.settings_theme_style),
                    secondaryLabel = stringResource(currentThemePreset.titleRes),
                    onClick = onThemeSelectClick,
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleButton(
                    icon = Icons.Rounded.Wallpaper,
                    label = stringResource(R.string.settings_timetable_background),
                    secondaryLabel = backgroundLabel,
                    onClick = onBackgroundSelectClick,
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            if (config.timetableBackgroundMode == TimetableBackgroundMode.IMAGE) {
                item {
                    OneUiSwitchCapsule(
                        title = stringResource(R.string.settings_image_background_blur),
                        subtitle = stringResource(R.string.settings_image_background_blur_summary),
                        icon = Icons.Rounded.BlurOn,
                        checked = config.imageBackgroundBlurEnabled,
                        onCheckedChange = onImageBlurToggle,
                        modifier = Modifier.fillMaxWidth()
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    )
                }
                item {
                    OneUiSwitchCapsule(
                        title = stringResource(R.string.settings_image_background_fluid),
                        subtitle = stringResource(R.string.settings_image_background_fluid_summary),
                        icon = Icons.Rounded.Animation,
                        checked = config.imageBackgroundFluidEnabled,
                        onCheckedChange = onImageFluidToggle,
                        modifier = Modifier.fillMaxWidth()
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    )
                }
            }
            item {
                OneUiCapsuleButton(
                    icon = Icons.Rounded.BlurOn,
                    label = stringResource(R.string.settings_liquid_glass),
                    secondaryLabel = if (config.isLiquidGlassEnabled) {
                        stringResource(R.string.settings_liquid_enabled, liquidEffectLabel)
                    } else stringResource(R.string.settings_disabled),
                    onClick = onLiquidGlassAdvancedClick,
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiSwitchCapsule(
                    title = stringResource(R.string.settings_ui_animations),
                    subtitle = stringResource(R.string.settings_ui_animations_summary),
                    icon = Icons.Rounded.Animation,
                    checked = config.uiAnimationsEnabled,
                    onCheckedChange = onUiAnimationsToggle,
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiSwitchCapsule(
                    title = stringResource(R.string.settings_top_time),
                    subtitle = stringResource(R.string.settings_top_time_summary),
                    icon = Icons.Rounded.Schedule,
                    checked = config.isShowTopTime,
                    onCheckedChange = onShowTopTimeToggle,
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
