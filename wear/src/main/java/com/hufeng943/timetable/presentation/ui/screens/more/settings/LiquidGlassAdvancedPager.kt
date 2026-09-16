package com.hufeng943.timetable.presentation.ui.screens.more.settings

import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.ColorLens
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
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

@Composable
fun LiquidGlassAdvancedPager(
    config: AppConfig,
    onEnabledChange: (Boolean) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onEffectChange: (LiquidGlassEffect) -> Unit,
    onBrightnessChange: (Float) -> Unit,
) {
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val opacitySteps = listOf(0.28f, 0.42f, 0.56f, 0.70f)
    val brightnessSteps = listOf(0.38f, 0.52f, 0.66f, 0.82f, 1f)
    fun next(values: List<Float>, current: Float): Float {
        val i = values.indices.minByOrNull { kotlin.math.abs(values[it] - current) } ?: 0
        return values[(i + 1) % values.size]
    }
    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(state = state, contentPadding = padding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform)
                ) { Text(stringResource(R.string.settings_liquid_glass_advanced)) }
            }
            item {
                SwitchButton(
                    checked = config.isLiquidGlassEnabled && supported,
                    onCheckedChange = { if (supported) onEnabledChange(it) },
                    enabled = supported,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transform),
                    icon = { androidx.wear.compose.material3.Icon(Icons.Rounded.BlurOn, null) },
                    label = { Text(stringResource(R.string.settings_liquid_glass)) },
                    secondaryLabel = { Text(stringResource(if (supported) R.string.settings_liquid_glass_summary else R.string.settings_liquid_glass_unsupported)) }
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.settings_glass_opacity),
                    subtitle = stringResource(R.string.settings_glass_opacity_value, (config.glassOpacity * 100).toInt()),
                    icon = Icons.Rounded.BlurOn,
                    onClick = { onOpacityChange(next(opacitySteps, config.glassOpacity)) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                )
            }
            item {
                val nextEffect = when (config.liquidGlassEffect) {
                    LiquidGlassEffect.SOFT -> LiquidGlassEffect.BALANCED
                    LiquidGlassEffect.BALANCED -> LiquidGlassEffect.FLUID
                    LiquidGlassEffect.FLUID -> LiquidGlassEffect.SOFT
                }
                OneUiCapsuleSurface(
                    title = stringResource(R.string.settings_liquid_effect),
                    subtitle = stringResource(when (config.liquidGlassEffect) {
                        LiquidGlassEffect.SOFT -> R.string.settings_liquid_effect_soft
                        LiquidGlassEffect.BALANCED -> R.string.settings_liquid_effect_balanced
                        LiquidGlassEffect.FLUID -> R.string.settings_liquid_effect_fluid
                    }),
                    icon = Icons.Rounded.ColorLens,
                    onClick = { onEffectChange(nextEffect) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.settings_background_brightness),
                    subtitle = stringResource(R.string.settings_background_brightness_value, (config.backgroundBrightness * 100).toInt()),
                    icon = Icons.Rounded.Wallpaper,
                    onClick = { onBrightnessChange(next(brightnessSteps, config.backgroundBrightness)) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.settings_glass_readability),
                    subtitle = stringResource(R.string.settings_glass_readability_summary),
                    icon = Icons.Rounded.Wallpaper,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                )
            }
        }
    }
}
