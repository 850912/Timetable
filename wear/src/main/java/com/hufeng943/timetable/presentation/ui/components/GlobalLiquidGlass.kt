package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
 * Shared glass treatment for reusable surfaces across the app.
 *
 * Wear OS has a much tighter GPU budget than phones. Liquid Glass therefore uses deliberately
 * small blur/refraction values and never animates its shader parameters while scrolling. Frosted
 * glass is the cheaper mode: blur + translucent tint only, with no lens/refraction pass.
 */
@Composable
fun Modifier.globalLiquidGlass(shape: Shape, surfaceColor: Color): Modifier {
    val config = LocalAppConfig.current
    val backdrop = LocalLiquidGlassBackdrop.current ?: return this
    if (!config.isLiquidGlassEnabled && !config.isFrostedGlassEnabled) return this

    val liquid = config.isLiquidGlassEnabled
    val profile = config.liquidGlassEffect
    val blurCap = when (profile) {
        LiquidGlassEffect.SOFT -> 0.75f
        LiquidGlassEffect.BALANCED -> 1.25f
        LiquidGlassEffect.FLUID -> 1.75f
    }
    val requestedBlur = config.glassBlurRadius.coerceAtMost(blurCap)
    val frostedBlur = 2.25f

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (liquid) {
                // Keep vibrancy optional and avoid stacking it with the cheapest profile.
                if (config.glassHighSaturation && profile != LiquidGlassEffect.SOFT) vibrancy()
                if (config.glassBlurEnabled && requestedBlur > 0f) blur(requestedBlur.dp.toPx())
                if (config.glassLensDistortion > 0f && profile != LiquidGlassEffect.SOFT) {
                    val profileScale = when (profile) {
                        LiquidGlassEffect.SOFT -> 0f
                        LiquidGlassEffect.BALANCED -> 0.30f
                        LiquidGlassEffect.FLUID -> 0.45f
                    }
                    val amount = config.glassLensDistortion.coerceIn(0f, 1f) * profileScale
                    lens(
                        3.dp.toPx() * amount,
                        5.dp.toPx() * amount,
                        // Chromatic aberration is the most expensive/visually noisy option; honor
                        // it only in the strongest profile.
                        chromaticAberration = config.glassChromaticAberration && profile == LiquidGlassEffect.FLUID,
                    )
                }
            } else {
                blur(frostedBlur.dp.toPx())
            }
        },
        highlight = {
            if (liquid) Highlight.Ambient.copy(alpha = 0.22f)
            else Highlight.Ambient.copy(alpha = 0.10f)
        },
        shadow = { Shadow(radius = 1.dp, color = Color.Black.copy(alpha = 0.10f)) },
        innerShadow = { InnerShadow(radius = 1.dp, alpha = if (liquid) 0.08f else 0.04f) },
        onDrawSurface = {
            val alpha = if (liquid) {
                config.glassOpacity.coerceIn(0.18f, 0.72f)
            } else {
                0.56f
            }
            // Layered tint reads more like glass than a flat translucent rectangle while keeping
            // the expensive work in the single backdrop pass above.
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = if (liquid) 0.055f else 0.038f),
                    0.38f to surfaceColor.copy(alpha = alpha),
                    1.0f to surfaceColor.copy(alpha = (alpha * 0.82f).coerceAtLeast(0.12f)),
                )
            )
            drawRect(Color.White.copy(alpha = if (liquid) 0.012f else 0.010f))
        },
    )
}
