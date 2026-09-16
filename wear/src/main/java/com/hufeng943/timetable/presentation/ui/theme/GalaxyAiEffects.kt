package com.hufeng943.timetable.presentation.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Lightweight Galaxy AI inspired ambient light treatment.
 *
 * This intentionally avoids runtime blur/shaders so the effect stays cheap on Galaxy Watch7.
 * It is used as a subtle highlight layer, not as a full-screen animated background.
 */
@Composable
fun GalaxyAiAmbientLayer(
    shape: Shape,
    modifier: Modifier = Modifier,
    strength: Float = 1f,
) {
    val primary = AppTheme.colors.primary
    val secondary = AppTheme.colors.secondary
    val preset = LocalThemePreset.current
    val effectAlpha = strength.coerceIn(0f, 1f)
    // System dynamic theme must remain visually dynamic all the way through custom
    // One UI surfaces. Older code mixed in fixed purple/cyan glows, making changes
    // to the watch-face palette barely visible on Samsung watches.
    val midGlow = if (preset == ThemePreset.AMOLED_BLACK) {
        Color(0xFF8FA9C7)
    } else {
        primary.copy(alpha = 1f)
    }
    val endGlow = if (preset == ThemePreset.AMOLED_BLACK) {
        Color(0xFF6E879F)
    } else {
        secondary.copy(alpha = 1f)
    }

    val ambientBrush = remember(primary, secondary, midGlow, endGlow, effectAlpha) {
        Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to primary.copy(alpha = 0.42f * effectAlpha),
                0.34f to midGlow.copy(alpha = 0.26f * effectAlpha),
                0.72f to endGlow.copy(alpha = 0.18f * effectAlpha),
                1.00f to secondary.copy(alpha = 0.10f * effectAlpha),
            ),
            start = Offset.Zero,
            end = Offset(420f, 260f),
        )
    }
    val glowA = remember(primary, effectAlpha) {
        Brush.radialGradient(listOf(primary.copy(alpha = 0.34f * effectAlpha), Color.Transparent), center = Offset(70f, 35f), radius = 190f)
    }
    val glowB = remember(secondary, effectAlpha) {
        Brush.radialGradient(listOf(secondary.copy(alpha = 0.28f * effectAlpha), Color.Transparent), center = Offset(320f, 330f), radius = 230f)
    }

    Box(modifier = modifier.fillMaxSize().clip(shape).background(ambientBrush)) {
        Box(Modifier.fillMaxSize().background(glowA))
        Box(Modifier.fillMaxSize().background(glowB))
    }
}

fun galaxyAiAccentBrush(
    courseColor: Color,
    themePrimary: Color,
    themeSecondary: Color,
): Brush =
    Brush.verticalGradient(
        listOf(
            themePrimary,
            themeSecondary,
            if (courseColor == Color.Unspecified) themeSecondary else courseColor,
        )
    )
