package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
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
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.OneUiSwitchCapsule
import com.hufeng943.timetable.presentation.ui.components.WearWheelPickerPage

/**
 * Main Liquid Glass page only. Numeric adjustment pages are app-level destinations so swipe-back
 * is rendered by the same app-level Navigation Compose graph as the rest of the app.
 */
@Composable
fun LiquidGlassAdvancedPager(
    config: AppConfig,
    onEnabledChange: (Boolean) -> Unit,
    onEffectChange: (LiquidGlassEffect) -> Unit,
    onChromaticAberrationChange: (Boolean) -> Unit,
    onLensDistortionChange: (Float) -> Unit,
    onBlurEnabledChange: (Boolean) -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
    onOpacityClick: () -> Unit,
    onClarityClick: () -> Unit,
    onBrightnessClick: () -> Unit,
) {
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()

    @Composable
    fun itemModifier(scope: androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope): Modifier =
        with(scope) {
            Modifier.fillMaxWidth()
                .transformedHeight(this, transform)
                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
        }

    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(
            state = state,
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text(stringResource(R.string.settings_liquid_glass_advanced)) }
            }
            item {
                OneUiSwitchCapsule(
                    title = stringResource(R.string.settings_liquid_glass),
                    subtitle = "实时折射、模糊与高光",
                    icon = Icons.Rounded.BlurOn,
                    checked = config.isLiquidGlassEnabled,
                    onCheckedChange = onEnabledChange,
                    modifier = itemModifier(this),
                )
            }
            if (config.isLiquidGlassEnabled) {
                item {
                    OneUiCapsuleSurface(
                        title = "玻璃底色浓度",
                        subtitle = "${(config.glassOpacity * 100).toInt()}% · 越低越通透",
                        icon = Icons.Rounded.BlurOn,
                        onClick = onOpacityClick,
                        modifier = itemModifier(this),
                    )
                }
                item {
                    OneUiCapsuleSurface(
                        title = "玻璃清透度",
                        subtitle = "${(config.glassClarity * 100).toInt()}% · 影响玻璃底色与高光层次",
                        icon = Icons.Rounded.Tune,
                        onClick = onClarityClick,
                        modifier = itemModifier(this),
                    )
                }
            }
            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.settings_background_brightness),
                    subtitle = "${(config.backgroundBrightness * 100).toInt()}% · 点按调节",
                    icon = Icons.Rounded.Wallpaper,
                    onClick = onBrightnessClick,
                    modifier = itemModifier(this),
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.settings_glass_readability),
                    subtitle = "Wear 已优化玻璃性能。",
                    icon = Icons.Rounded.Wallpaper,
                    modifier = itemModifier(this),
                )
            }
        }
    }
}

/** Picker page used by the app-level Liquid Glass adjustment routes. */
@Composable
fun LiquidGlassIntegerAdjustPager(
    title: String,
    value: Int,
    range: IntRange,
    suffix: String = "%",
    onApply: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val values = remember(range) { range.toList() }
    WearWheelPickerPage(
        title = title,
        values = values,
        initial = value.coerceIn(range),
        label = { "$it$suffix" },
        onConfirm = {
            onApply(it)
            onClose()
        },
    )
}
