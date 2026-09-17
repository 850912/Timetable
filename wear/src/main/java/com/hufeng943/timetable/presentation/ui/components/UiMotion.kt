package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig

/** Elastic, layer-only press motion. No relayout and no competing tween on release. */
@Composable
fun rememberPressMotion(): Pair<MutableInteractionSource, Modifier> {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val enabled = LocalAppConfig.current.uiAnimationsEnabled
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = if (pressed) 0.82f else 0.68f,
            stiffness = if (pressed) Spring.StiffnessMedium else Spring.StiffnessMediumLow,
        ),
        label = "glassPressScale",
    )
    return interactionSource to Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
