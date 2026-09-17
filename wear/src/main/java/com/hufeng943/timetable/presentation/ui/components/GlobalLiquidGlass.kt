package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalHazeState
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

/**
 * Single Haze-backed material entry point for the whole app.
 *
 * There is no custom shader/backdrop implementation here anymore. Liquid glass uses Haze Glass,
 * frosted glass uses Haze Blur, and Wear always asks Haze for its Performance profile.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun Modifier.globalLiquidGlass(shape: RoundedCornerShape, surfaceColor: Color): Modifier {
    val config = LocalAppConfig.current
    val hazeState = LocalHazeState.current ?: return this
    val anyGlass = config.isGlobalGlassMaterialEnabled || config.isLiquidGlassEnabled || config.isFrostedGlassEnabled
    if (!anyGlass) return this

    val input = HazeInput.Backdrop(hazeState)
    val clipped = this.clip(shape)

    // Frosted mode is deliberately optics-free. Haze handles the blur backend and downsampling.
    if (config.isFrostedGlassEnabled && !config.isLiquidGlassEnabled && !config.isGlobalGlassMaterialEnabled) {
        val blurRadius = if (config.glassBlurEnabled) {
            (config.glassBlurRadius.coerceIn(0.5f, 2.0f) * 6f).dp
        } else 0.dp
        return clipped.hazeBlur(
            input = input,
            style = HazeBlurStyle {
                blurRadius(blurRadius)
                colorEffects(listOf(
                    HazeColorEffect.tint(surfaceColor.copy(alpha = config.glassOpacity.coerceIn(0.16f, 0.58f)))
                ))
            },
            performanceMode = HazePerformanceMode.Performance,
            expandLayerBounds = false,
        )
    }

    val profile = config.liquidGlassEffect
    val blur = when (profile) {
        LiquidGlassEffect.SOFT -> 3.dp
        LiquidGlassEffect.BALANCED -> 5.dp
        LiquidGlassEffect.FLUID -> 7.dp
    }
    val refraction = when (profile) {
        LiquidGlassEffect.SOFT -> 0.18f
        LiquidGlassEffect.BALANCED -> 0.32f
        LiquidGlassEffect.FLUID -> 0.46f
    } * config.glassLensDistortion.coerceIn(0f, 1f)
    val tintAlpha = config.glassOpacity.coerceIn(0.12f, 0.52f)

    val style = GlassStyle.regular.then {
        backgroundColor(surfaceColor.copy(alpha = 0.10f))
        tint(surfaceColor.copy(alpha = tintAlpha))
        shape(shape)
        optics(
            blurRadius = if (config.glassBlurEnabled) blur else 0.dp,
            refractionStrength = refraction,
            refractionHeightFraction = 0.22f,
            depth = if (profile == LiquidGlassEffect.SOFT) 0.18f else 0.28f,
        )
        specularIntensity(if (profile == LiquidGlassEffect.SOFT) 0.22f else 0.34f)
        ambientResponse(0.30f)
        edgeSoftness(2.dp)
        chromaticAberrationStrength(
            if (config.glassChromaticAberration && profile == LiquidGlassEffect.FLUID) 0.10f else 0f
        )
    }

    return clipped.hazeGlass(
        input = input,
        style = style,
        performanceMode = HazePerformanceMode.Performance,
        expandLayerBounds = false,
    )
}
