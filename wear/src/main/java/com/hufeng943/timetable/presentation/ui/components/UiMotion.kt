package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import androidx.wear.compose.material3.MaterialTheme

/** Elastic, layer-only press motion. No relayout and no competing tween on release. */
@Composable
fun rememberPressMotion(): Pair<MutableInteractionSource, Modifier> {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val enabled = LocalAppConfig.current.uiAnimationsEnabled
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.965f else 1f,
        animationSpec = if (enabled) {
            MaterialTheme.motionScheme.fastSpatialSpec()
        } else {
            snap()
        },
        label = "glassPressScale",
    )
    return interactionSource to Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
