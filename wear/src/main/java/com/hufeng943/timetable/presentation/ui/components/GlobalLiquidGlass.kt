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
 * Blur, lens distortion, aberration, and vibrancy remain independently configurable. Android 13+
 * uses the Backdrop renderer; older or constrained devices keep a lightweight fallback.
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
    val profile = config.liquidGlassEffect
    // Wear optimization: blur is substantially more expensive than tint/refraction. Keep the
    // reference-app optical look, but reserve a tiny blur pass for the Fluid profile only.
    // Low-RAM watches automatically use the cheaper path.
    val blurDp = if (!lowRamDevice && profile == LiquidGlassEffect.FLUID) {
        0.55f.coerceAtMost(config.glassBlurRadius.coerceAtLeast(0f))
    } else 0f
    val lensAmount = config.glassLensDistortion.coerceIn(0f, 0.60f)
    val clarity = config.glassClarity.coerceIn(0f, 1f)
    // Tint density and optical clarity are independent. Higher clarity preserves the low-opacity
    // liquid look without forcing the tint control to an extreme.
    val surfaceAlpha = (config.glassOpacity.coerceIn(0.05f, 0.52f) * (1f - clarity * 0.55f)).coerceIn(0.04f, 0.52f)

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (config.glassBlurEnabled && blurDp > 0f) blur(blurDp.dp.toPx())
            if (liquid) {
                // Micro-refraction + vibrancy are what make the reference look like optical glass
                // rather than a plain frosted panel.
                vibrancy()
                if (lensAmount > 0.02f) {
                    val effectiveLens = if (lowRamDevice) lensAmount.coerceAtMost(0.24f) else lensAmount
                    val height = (3.5f + 6f * effectiveLens).dp.toPx()
                    val radius = (7f + 11f * effectiveLens).dp.toPx()
                    lens(
                        refractionHeight = height,
                        refractionAmount = radius,
                        depthEffect = !lowRamDevice && profile != LiquidGlassEffect.SOFT,
                        chromaticAberration = !lowRamDevice && config.glassChromaticAberration && profile == LiquidGlassEffect.FLUID,
                    )
                }
            }
        },
        highlight = {
            Highlight.Ambient.copy(alpha = if (liquid) 0.34f else 0.16f)
        },
        shadow = {
            Shadow(radius = 1.dp, color = Color.Black.copy(alpha = 0.14f))
        },
        innerShadow = {
            InnerShadow(radius = 0.8.dp, alpha = if (liquid) 0.12f else 0.07f)
        },
        onDrawSurface = {
            drawRect(surfaceColor.copy(alpha = surfaceAlpha))
            drawRect(Color.White.copy(alpha = if (liquid) 0.035f else 0.018f))
        },
    )
}
