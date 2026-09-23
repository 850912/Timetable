package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.OneUiSwitchCapsule
import com.hufeng943.timetable.presentation.ui.components.rememberWearHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * Main Liquid Glass page only. Numeric adjustment pages are app-level destinations so swipe-back
 * is rendered by the same Wear Navigation 3 back stack as the rest of the app.
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
    val initialIndex = remember(title, value, values) {
        values.indexOf(value.coerceIn(range)).coerceAtLeast(0)
    }
    val state = rememberPickerState(
        initialNumberOfOptions = values.size,
        initiallySelectedIndex = initialIndex,
        shouldRepeatOptions = false,
    )
    val haptics = rememberWearHaptics()
    val selectedIndex by remember(state) {
        derivedStateOf { state.selectedOptionIndex.coerceIn(values.indices) }
    }
    val selected = values[selectedIndex]

    LaunchedEffect(state) {
        snapshotFlow { state.selectedOptionIndex }
            .distinctUntilChanged()
            .drop(1)
            .collect { haptics.tick() }
    }

    // Picker pages deliberately suppress the global TimeText. The title therefore occupies the
    // top safe area without colliding with the system time on small round displays.
    ScreenScaffold(timeText = {}) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Picker(
                state = state,
                contentDescription = { "$selected$suffix" },
                gradientColor = Color.Unspecified,
                modifier = Modifier.size(width = 118.dp, height = 118.dp),
            ) { index ->
                val item = values[index]
                val isSelected = index == state.selectedOptionIndex
                Text(
                    text = "$item$suffix",
                    textAlign = TextAlign.Center,
                    style = if (isSelected) MaterialTheme.typography.displayMedium else MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isSelected) 1f else 0.48f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = title,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
            )

            EdgeButton(
                onClick = {
                    haptics.confirm()
                    onApply(selected)
                    onClose()
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.check))
            }
        }
    }
}
