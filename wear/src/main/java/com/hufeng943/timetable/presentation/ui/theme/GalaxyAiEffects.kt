package com.hufeng943.timetable.presentation.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Lightweight Galaxy AI inspired ambient light treatment.
 *
 * The glow is static and size-relative: it works for both full-screen watch backgrounds and
 * small capsules without depending on one hard-coded pixel resolution.
 */
@Composable
fun GalaxyAiAmbientLayer(
    shape: Shape,
    modifier: Modifier = Modifier,
    strength: Float = 1f,
    primaryOverride: Color? = null,
    secondaryOverride: Color? = null,
) {
    val primary = primaryOverride ?: AppTheme.colors.primary
    val secondary = secondaryOverride ?: AppTheme.colors.secondary
    val preset = LocalThemePreset.current
    val effectAlpha = strength.coerceIn(0f, 1f)
    val midGlow = if (preset == ThemePreset.AMOLED_BLACK) Color(0xFF8FA9C7) else primary
    val endGlow = if (preset == ThemePreset.AMOLED_BLACK) Color(0xFF6E879F) else secondary

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .drawWithCache {
                val width = size.width.coerceAtLeast(1f)
                val height = size.height.coerceAtLeast(1f)
                val ambientBrush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to primary.copy(alpha = 0.42f * effectAlpha),
                        0.34f to midGlow.copy(alpha = 0.26f * effectAlpha),
                        0.72f to endGlow.copy(alpha = 0.18f * effectAlpha),
                        1.00f to secondary.copy(alpha = 0.10f * effectAlpha),
                    ),
                    start = Offset.Zero,
                    end = Offset(width, height * 0.72f),
                )
                val glowA = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.34f * effectAlpha), Color.Transparent),
                    center = Offset(width * 0.18f, height * 0.12f),
                    radius = maxOf(width, height) * 0.58f,
                )
                val glowB = Brush.radialGradient(
                    colors = listOf(secondary.copy(alpha = 0.28f * effectAlpha), Color.Transparent),
                    center = Offset(width * 0.82f, height * 0.84f),
                    radius = maxOf(width, height) * 0.68f,
                )
                onDrawBehind {
                    drawRect(ambientBrush)
                    drawRect(glowA)
                    drawRect(glowB)
                }
            },
    )
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
