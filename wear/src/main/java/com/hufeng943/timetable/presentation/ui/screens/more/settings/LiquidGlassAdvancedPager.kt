package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect
import com.hufeng943.timetable.presentation.ui.components.WearInternalNavHost
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.OneUiSwitchCapsule
import com.hufeng943.timetable.presentation.ui.components.rememberWearHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

private data class AdjustTarget(val title: String, val value: Int, val range: IntRange, val suffix: String, val apply: (Int) -> Unit)

@Composable
fun LiquidGlassAdvancedPager(
    config: AppConfig,
    onEnabledChange: (Boolean) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onClarityChange: (Float) -> Unit,
    onEffectChange: (com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onChromaticAberrationChange: (Boolean) -> Unit,
    onLensDistortionChange: (Float) -> Unit,
    onBlurEnabledChange: (Boolean) -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
) {
    val nav = rememberNavController()
    var adjust by remember { mutableStateOf<AdjustTarget?>(null) }

    fun openAdjust(target: AdjustTarget) {
        adjust = target
        nav.navigateSingle("adjust")
    }

    WearInternalNavHost(navController = nav, startDestination = "main") {
        composable("main") {
            val state = rememberTransformingLazyColumnState()
            val transform = rememberTransformationSpec()
            @Composable
            fun itemModifier(scope: androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope): Modifier = with(scope) {
                Modifier.fillMaxWidth().transformedHeight(this, transform)
                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
            }
            ScreenScaffold(scrollState = state) { padding ->
                TransformingLazyColumn(state = state, rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state), modifier = Modifier.fillMaxSize(), contentPadding = padding) {
                    item { ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(stringResource(R.string.settings_liquid_glass_advanced))} }
                    item { OneUiSwitchCapsule(title=stringResource(R.string.settings_liquid_glass),subtitle="实时折射、模糊与高光",icon=Icons.Rounded.BlurOn,checked=config.isLiquidGlassEnabled,onCheckedChange=onEnabledChange,modifier=itemModifier(this)) }
                    // Wear OS: hide controls whose visual delta is too small or whose GPU cost is
                    // disproportionate on round watches. Keep only parameters that are clearly
                    // visible and stable across Wear OS 6 / China-ROM devices.
                    if (config.isLiquidGlassEnabled) item { OneUiCapsuleSurface(title="玻璃底色浓度",subtitle="${(config.glassOpacity*100).toInt()}% · 越低越通透",icon=Icons.Rounded.BlurOn,onClick={openAdjust(AdjustTarget("玻璃底色浓度",(config.glassOpacity*100).toInt(),5..95,"%") { onOpacityChange(it/100f) })},modifier=itemModifier(this)) }
                    if (config.isLiquidGlassEnabled) item { OneUiCapsuleSurface(title="玻璃清透度",subtitle="${(config.glassClarity*100).toInt()}% · 影响玻璃底色与高光层次",icon=Icons.Rounded.Tune,onClick={openAdjust(AdjustTarget("玻璃清透度",(config.glassClarity*100).toInt(),0..100,"%") { onClarityChange(it/100f) })},modifier=itemModifier(this)) }
                    item { OneUiCapsuleSurface(title=stringResource(R.string.settings_background_brightness),subtitle="${(config.backgroundBrightness*100).toInt()}% · 点按调节",icon=Icons.Rounded.Wallpaper,onClick={openAdjust(AdjustTarget("背景亮度",(config.backgroundBrightness*100).toInt(),10..100,"%") { onBrightnessChange(it/100f) })},modifier=itemModifier(this)) }
                    item { OneUiCapsuleSurface(title=stringResource(R.string.settings_glass_readability),subtitle="Wear 已优化玻璃性能。",icon=Icons.Rounded.Wallpaper,modifier=itemModifier(this)) }
                }
            }
        }
        composable("adjust") {
            val target = adjust
            if (target != null) IntegerAdjustPager(target) { nav.popBackStack() }
        }
    }
}

@Composable
private fun IntegerAdjustPager(target: AdjustTarget, onClose: () -> Unit) {
    val values = remember(target.range) { target.range.toList() }
    val initialIndex = remember(target.title, target.value, values) {
        values.indexOf(target.value.coerceIn(target.range)).coerceAtLeast(0)
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

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Picker(
            state = state,
            contentDescription = { "$selected${target.suffix}" },
            // The app can render an image/gradient behind this page. Per Picker docs,
            // Color.Unspecified prevents opaque gradient bands on custom backgrounds.
            gradientColor = androidx.compose.ui.graphics.Color.Unspecified,
            modifier = Modifier.size(width = 118.dp, height = 118.dp),
        ) { index ->
            val value = values[index]
            val isSelected = index == state.selectedOptionIndex
            Text(
                text = "$value${target.suffix}",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = if (isSelected) MaterialTheme.typography.displayMedium else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isSelected) 1f else 0.48f),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            text = target.title,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopCenter)
                .padding(top = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
        )

        EdgeButton(
            onClick = {
                haptics.confirm()
                target.apply(selected)
                onClose()
            },
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.check))
        }
    }
}
