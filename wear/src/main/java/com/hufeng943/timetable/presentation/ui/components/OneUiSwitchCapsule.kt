package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import com.hufeng943.timetable.presentation.ui.theme.AppTheme

/** A glass-compatible switch row used instead of opaque Material SwitchButton surfaces. */
@Composable
fun OneUiSwitchCapsule(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val animationsEnabled = com.hufeng943.timetable.presentation.ui.common.LocalAppConfig.current.uiAnimationsEnabled
    val travelPx = with(LocalDensity.current) { 16.dp.toPx() }
    val knobOffset by animateFloatAsState(
        targetValue = if (checked) travelPx else 0f,
        animationSpec = tween(if (animationsEnabled) 140 else 0),
        label = "switchKnob",
    )
    OneUiCapsuleSurface(
        title = title,
        subtitle = subtitle,
        icon = icon,
        selected = false,
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
        modifier = modifier,
        trailing = {
            val track = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .size(width = 34.dp, height = 22.dp)
                    .clip(track)
                    .background(if (checked) colors.primary.copy(alpha = 0.26f) else colors.surface.copy(alpha = 0.72f))
                    .border(1.dp, if (checked) colors.primary.copy(alpha = 0.66f) else colors.textSecondary.copy(alpha = 0.28f), track),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { translationX = knobOffset }
                        .clip(CircleShape)
                        .background(if (checked) colors.primary else colors.textSecondary.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (checked) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.Black.copy(alpha = 0.74f),
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        },
    )
}
