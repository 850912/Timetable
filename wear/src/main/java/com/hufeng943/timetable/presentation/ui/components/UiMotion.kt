package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig

/**
 * Cheap, draw-layer-only interaction motion for Wear OS.
 *
 * Scaling/alpha are applied through graphicsLayer so a press does not relayout the list. The
 * global UI animation switch can turn this off completely for battery-sensitive users.
 */
@Composable
fun rememberPressMotion(): Pair<MutableInteractionSource, Modifier> {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val enabled = LocalAppConfig.current.uiAnimationsEnabled
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.975f else 1f,
        animationSpec = tween(durationMillis = if (pressed) 75 else 125),
        label = "wearPressScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = if (pressed) 70 else 120),
        label = "wearPressAlpha",
    )
    return interactionSource to Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}
