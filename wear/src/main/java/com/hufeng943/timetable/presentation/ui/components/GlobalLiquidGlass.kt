package com.hufeng943.timetable.presentation.ui.components

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalLiquidGlassBackdrop
import com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * WYS App Market-inspired liquid glass.
 *
 * Uses Kyant Backdrop on Android 13+ and a cheap translucent fallback elsewhere.
 * The optical profiles share one renderer, so switching between soft, balanced and fluid changes
 * refraction/highlight character without replacing the material or rebuilding the UI hierarchy.
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
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || backdrop == null) {
        val alpha = config.glassOpacity.coerceIn(0.18f, 0.52f)
        return drawWithCache {
            val material = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.11f),
                0.24f to surfaceColor.copy(alpha = alpha),
                1f to surfaceColor.copy(alpha = (alpha * 0.72f).coerceAtLeast(0.12f)),
            )
            onDrawBehind { drawRect(material) }
        }
    }

    val liquid = config.isLiquidGlassEnabled
    val profile = when (config.liquidGlassEffect) {
        LiquidGlassEffect.SOFT -> Triple(0.78f, 0.72f, 0.84f)
        LiquidGlassEffect.BALANCED -> Triple(1f, 1f, 1f)
        LiquidGlassEffect.FLUID -> Triple(1.18f, 1.34f, 1.18f)
    }
    // Every profile remains visibly liquid. Profiles tune the optical character instead of
    // replacing glass with a flat surface; only low-RAM devices skip the blur pass itself.
    val requestedBlurDp = config.glassBlurRadius.coerceIn(0.35f, 3.5f) * profile.first
    val blurDp = if (config.glassBlurEnabled && !lowRamDevice) requestedBlurDp else 0f
    val requestedLens = (config.glassLensDistortion.coerceIn(0.08f, 0.75f) * profile.second)
        .coerceIn(0.08f, 0.82f)
    val lensAmount = if (lowRamDevice) requestedLens.coerceAtMost(0.26f) else requestedLens
    val clarity = config.glassClarity.coerceIn(0f, 1f)
    // Tint density and optical clarity are independent. Higher clarity preserves the low-opacity
    // liquid look without forcing the tint control to an extreme.
    val surfaceAlpha = (config.glassOpacity.coerceIn(0.05f, 0.52f) * (1f - clarity * 0.55f)).coerceIn(0.04f, 0.52f)

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (blurDp > 0f) blur(blurDp.dp.toPx())
            if (liquid) {
                // Micro-refraction + vibrancy are what make the reference look like optical glass
                // rather than a plain frosted panel.
                vibrancy()
                if (lensAmount > 0.02f) {
                    val height = (3.8f + 7.5f * lensAmount).dp.toPx()
                    val radius = (7.5f + 13f * lensAmount).dp.toPx()
                    lens(
                        refractionHeight = height,
                        refractionAmount = radius,
                        depthEffect = !lowRamDevice,
                        chromaticAberration = config.glassChromaticAberration && !lowRamDevice,
                    )
                }
            }
        },
        highlight = {
            Highlight.Ambient.copy(alpha = if (liquid) 0.34f * profile.third else 0.16f)
        },
        shadow = {
            Shadow(radius = 1.dp, color = Color.Black.copy(alpha = 0.14f))
        },
        innerShadow = {
            InnerShadow(radius = 0.8.dp, alpha = if (liquid) 0.12f * profile.third else 0.07f)
        },
        onDrawSurface = {
            drawRect(surfaceColor.copy(alpha = surfaceAlpha))
            drawRect(Color.White.copy(alpha = if (liquid) 0.035f else 0.018f))
        },
    )
}
