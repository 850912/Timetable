package com.hufeng943.timetable.presentation.ui.components

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalLiquidGlassBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * Shared optical-glass material for Wear OS.
 *
 * Every user-facing glass control below maps to a renderer parameter. The profiles only tune the
 * cost/character of the material; they no longer silently bypass opacity, lens or blur controls.
 */
@Composable
fun Modifier.globalLiquidGlass(shape: Shape, surfaceColor: Color): Modifier {
    val config = LocalAppConfig.current
    val context = LocalContext.current
    val lowRamDevice = remember(context) {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
    }
    val enabled = config.isLiquidGlassEnabled
    if (!enabled) return this

    val backdrop = LocalLiquidGlassBackdrop.current
    val requestedAlpha = config.glassOpacity.coerceIn(0.10f, 0.70f)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || backdrop == null) {
        return drawWithCache {
            val neutral = surfaceColor.alpha <= 0.001f
            val material = if (neutral) {
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.12f),
                    0.30f to Color.White.copy(alpha = 0.045f),
                    1f to Color.White.copy(alpha = 0.018f),
                )
            } else {
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.12f),
                    0.28f to surfaceColor.copy(alpha = requestedAlpha),
                    1f to surfaceColor.copy(alpha = (requestedAlpha * 0.70f).coerceAtLeast(0.08f)),
                )
            }
            onDrawBehind { drawRect(material) }
        }
    }

    val liquid = config.isLiquidGlassEnabled
    val profile = config.liquidGlassEffect
    val profileLensMultiplier = when (profile) {
        LiquidGlassEffect.SOFT -> 0.62f
        LiquidGlassEffect.BALANCED -> 1.00f
        LiquidGlassEffect.FLUID -> 1.28f
    }
    val profileBlurMultiplier = when (profile) {
        LiquidGlassEffect.SOFT -> 0.20f
        LiquidGlassEffect.BALANCED -> 0.42f
        LiquidGlassEffect.FLUID -> 0.70f
    }
    val requestedBlur = if (config.glassBlurEnabled) config.glassBlurRadius.coerceIn(0f, 2f) else 0f
    val blurDp = requestedBlur * profileBlurMultiplier * if (lowRamDevice) 0.45f else 1f
    val requestedLens = config.glassLensDistortion.coerceIn(0f, 0.60f)
    // Low-RAM devices scale the full control range instead of clipping it, so every slider step
    // still produces a visible change while keeping the maximum shader cost restrained.
    val effectiveLens = (requestedLens * profileLensMultiplier * if (lowRamDevice) 0.45f else 1f)
        .coerceAtMost(if (lowRamDevice) 0.32f else 0.72f)
    val depthEnabled = liquid && !lowRamDevice && profile != LiquidGlassEffect.SOFT
    val chromaticEnabled = liquid && config.glassChromaticAberration && profile != LiquidGlassEffect.SOFT
    val highlightAlpha = when (profile) {
        LiquidGlassEffect.SOFT -> 0.22f
        LiquidGlassEffect.BALANCED -> 0.31f
        LiquidGlassEffect.FLUID -> 0.39f
    }
    val neutralOpticalSurface = surfaceColor.alpha <= 0.001f
    val surfaceAlpha = if (neutralOpticalSurface) 0f else when (profile) {
        LiquidGlassEffect.SOFT -> requestedAlpha * 0.90f
        LiquidGlassEffect.BALANCED -> requestedAlpha
        LiquidGlassEffect.FLUID -> requestedAlpha * 0.92f
    }.coerceIn(0.08f, 0.70f)

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (blurDp > 0.01f) blur(blurDp.dp.toPx())
            if (liquid) {
                vibrancy()
                if (effectiveLens > 0.01f) {
                    // The full slider range now changes both refraction height and amount.
                    val height = (2.6f + 11.0f * effectiveLens).dp.toPx()
                    val amount = (5.0f + 24.0f * effectiveLens).dp.toPx()
                    lens(
                        refractionHeight = height,
                        refractionAmount = amount,
                        depthEffect = depthEnabled,
                        chromaticAberration = chromaticEnabled,
                    )
                }
            }
        },
        highlight = { Highlight.Ambient.copy(alpha = if (liquid) highlightAlpha else 0.16f) },
        shadow = { Shadow(radius = 1.dp, color = Color.Black.copy(alpha = 0.12f)) },
        innerShadow = {
            InnerShadow(
                radius = if (profile == LiquidGlassEffect.FLUID) 1.0.dp else 0.7.dp,
                alpha = if (liquid) 0.11f else 0.06f,
            )
        },
        onDrawSurface = {
            if (!neutralOpticalSurface) drawRect(surfaceColor.copy(alpha = surfaceAlpha))
            drawRect(Color.White.copy(alpha = when (profile) {
                LiquidGlassEffect.SOFT -> if (neutralOpticalSurface) 0.012f else 0.020f
                LiquidGlassEffect.BALANCED -> if (neutralOpticalSurface) 0.018f else 0.030f
                LiquidGlassEffect.FLUID -> if (neutralOpticalSurface) 0.026f else 0.042f
            }))
        },
    )
}
