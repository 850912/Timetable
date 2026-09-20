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
import androidx.wear.compose.foundation.LocalScreenIsActive
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
 * Shared liquid-glass renderer for watch surfaces.
 *
 * The full Backdrop path is intentionally conservative on Wear OS. The previous high-cost renderer
 * allowed up to 8dp of live blur on every visible card; a list can contain several cards at once,
 * so that multiplied the most expensive pass and caused severe jank. The UI setting still spans
 * 0..8, but it is mapped to a sub-dp optical blur and combined with refraction/highlight changes.
 * This keeps every control visible while restoring the cost profile of 3.4.x.
 */
@Composable
fun Modifier.globalLiquidGlass(shape: Shape, surfaceColor: Color): Modifier {
    val config = LocalAppConfig.current
    if (!config.isLiquidGlassEnabled) return this

    val context = LocalContext.current
    val lowRamDevice = remember(context) {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
    }
    val screenIsActive = LocalScreenIsActive.current
    val backdrop = LocalLiquidGlassBackdrop.current

    fun lightweightFallback(): Modifier {
        val alpha = config.glassOpacity.coerceIn(0.18f, 0.52f)
        val clarity = config.glassClarity.coerceIn(0f, 1f)
        return drawWithCache {
            val material = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.10f + clarity * 0.025f),
                0.24f to surfaceColor.copy(alpha = alpha),
                1f to surfaceColor.copy(alpha = (alpha * 0.72f).coerceAtLeast(0.12f)),
            )
            onDrawBehind { drawRect(material) }
        }
    }

    // Inactive SwipeDismissableNavHost pages can remain composed during the gesture. Rendering
    // AGSL/backdrop effects on both pages at once wastes frame budget exactly when animation needs it.
    if (
        !screenIsActive || lowRamDevice ||
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || backdrop == null
    ) {
        return lightweightFallback()
    }

    val profile = config.liquidGlassEffect
    val profileBlurMaxDp = when (profile) {
        LiquidGlassEffect.SOFT -> 0.34f
        LiquidGlassEffect.BALANCED -> 0.52f
        LiquidGlassEffect.FLUID -> 0.72f
    }
    // Map the user-facing 0..8 strength to a Wear-safe optical radius. This remains monotonic and
    // visible without doing an 8dp blur pass for every card.
    val blurDp = if (!lowRamDevice && config.glassBlurEnabled) {
        val level = config.glassBlurRadius.coerceIn(0f, 8f)
        if (level <= 0f) 0f else {
            val normalized = kotlin.math.sqrt(level / 8f)
            0.16f + (profileBlurMaxDp - 0.16f) * normalized
        }
    } else 0f

    val lensMultiplier = when (profile) {
        LiquidGlassEffect.SOFT -> 0.55f
        LiquidGlassEffect.BALANCED -> 0.78f
        LiquidGlassEffect.FLUID -> 1.0f
    }
    val lensAmount = config.glassLensDistortion.coerceIn(0f, 0.60f) * lensMultiplier
    val clarity = config.glassClarity.coerceIn(0f, 1f)
    val surfaceAlpha = (
        config.glassOpacity.coerceIn(0.05f, 0.52f) * (1f - clarity * 0.55f)
        ).coerceIn(0.04f, 0.52f)

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (blurDp > 0.01f) blur(blurDp.dp.toPx())
            vibrancy()
            if (lensAmount > 0.02f) {
                val effectiveLens = if (lowRamDevice) lensAmount.coerceAtMost(0.22f) else lensAmount
                val height = (3.2f + 5.2f * effectiveLens).dp.toPx()
                val radius = (6.5f + 9.5f * effectiveLens).dp.toPx()
                lens(
                    refractionHeight = height,
                    refractionAmount = radius,
                    // Depth is one of the expensive extras. Keep it for the strongest profile only.
                    depthEffect = !lowRamDevice && profile == LiquidGlassEffect.FLUID && lensAmount > 0.08f,
                    // Dispersion is opt-in. Unlike 3.4.x it is not silently ignored outside FLUID.
                    chromaticAberration = !lowRamDevice && config.glassChromaticAberration,
                )
            }
        },
        highlight = {
            Highlight.Ambient.copy(
                alpha = when (profile) {
                    LiquidGlassEffect.SOFT -> 0.20f
                    LiquidGlassEffect.BALANCED -> 0.29f
                    LiquidGlassEffect.FLUID -> 0.37f
                }
            )
        },
        shadow = {
            Shadow(radius = 1.dp, color = Color.Black.copy(alpha = 0.13f))
        },
        innerShadow = {
            InnerShadow(
                radius = 0.7.dp,
                alpha = when (profile) {
                    LiquidGlassEffect.SOFT -> 0.07f
                    LiquidGlassEffect.BALANCED -> 0.10f
                    LiquidGlassEffect.FLUID -> 0.12f
                }
            )
        },
        onDrawSurface = {
            drawRect(surfaceColor.copy(alpha = surfaceAlpha))
            drawRect(Color.White.copy(alpha = 0.03f + clarity * 0.012f))
        },
    )
}
