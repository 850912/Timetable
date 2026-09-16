package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalLiquidGlassBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/** Shared glass treatment for every reusable One UI surface, not just timetable cards. */
@Composable
fun Modifier.globalLiquidGlass(shape: Shape, surfaceColor: Color): Modifier {
    val config = LocalAppConfig.current
    val backdrop = LocalLiquidGlassBackdrop.current ?: return this
    if (!config.isLiquidGlassEnabled) return this
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (config.glassHighSaturation) vibrancy()
            if (config.glassBlurEnabled && config.glassBlurRadius > 0f) blur(config.glassBlurRadius.dp.toPx())
            if (config.glassLensDistortion > 0f) {
                val amount = config.glassLensDistortion
                lens(8.dp.toPx() * amount, 14.dp.toPx() * amount, chromaticAberration = config.glassChromaticAberration)
            }
        },
        highlight = { Highlight.Ambient.copy(alpha = 0.46f) },
        shadow = { Shadow(radius = 2.dp, color = Color.Black.copy(alpha = 0.14f)) },
        innerShadow = { InnerShadow(radius = 3.dp, alpha = 0.14f) },
        onDrawSurface = {
            drawRect(surfaceColor.copy(alpha = config.glassOpacity.coerceIn(0.05f, 0.95f)))
            drawRect(Color.White.copy(alpha = 0.018f))
        },
    )
}
