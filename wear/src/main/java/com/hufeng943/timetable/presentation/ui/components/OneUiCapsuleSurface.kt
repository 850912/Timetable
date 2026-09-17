package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalLiquidGlassBackdrop
import com.hufeng943.timetable.presentation.ui.theme.GalaxyAiAmbientLayer

val OneUiCapsuleShape = RoundedCornerShape(26.dp)

@Composable
fun OneUiCapsuleSurface(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    selected: Boolean = false,
    emphasize: Boolean = false,
    destructive: Boolean = false,
    accentColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    titleMaxLines: Int = 2,
    subtitleMaxLines: Int = 2,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val glassConfig = LocalAppConfig.current
    val vividDeleteRed = Color(0xFFFF453A)
    val resolvedAccent = when {
        destructive -> vividDeleteRed
        accentColor != Color.Unspecified -> accentColor
        else -> colors.primary
    }
    val backgroundColor = when {
        // Keep destructive rows readable on a tiny display. Full red fills were overwhelming and
        // could hide labels near the curved top/bottom of the watch screen.
        destructive -> colors.surfaceContainer
        // Selection should be an accent, not a second foreground layer. A dark container with
        // border/checkmark keeps contrast stable with dynamic colors and custom backgrounds.
        selected -> colors.surfaceContainer
        else -> colors.surfaceContainer
    }

    val (interactionSource, pressMotion) = rememberPressMotion()

    var root = modifier
        .then(pressMotion)
        .fillMaxWidth()
        .clip(OneUiCapsuleShape)
        .globalLiquidGlass(OneUiCapsuleShape, backgroundColor)
        .background(backgroundColor.copy(alpha = if (LocalLiquidGlassBackdrop.current != null && (glassConfig.isLiquidGlassEnabled || glassConfig.isFrostedGlassEnabled || glassConfig.isGlobalGlassMaterialEnabled)) 0f else 1f))
        .then(
            when {
                destructive -> Modifier.border(1.dp, vividDeleteRed.copy(alpha = 0.74f), OneUiCapsuleShape)
                selected -> Modifier.border(1.dp, resolvedAccent.copy(alpha = 0.62f), OneUiCapsuleShape)
                else -> Modifier
            }
        )
    root = when {
        onClick != null && onLongClick != null -> root.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick,
        )
        onClick != null -> root.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
        else -> root
    }

    Box(modifier = root) {
        if (emphasize && !destructive) {
            GalaxyAiAmbientLayer(
                shape = OneUiCapsuleShape,
                strength = 0.28f,
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(resolvedAccent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = resolvedAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(9.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (destructive) vividDeleteRed else colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = titleMaxLines,
                    overflow = if (titleMaxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (destructive) {
                            vividDeleteRed.copy(alpha = 0.82f)
                        } else {
                            colors.textSecondary
                        },
                        maxLines = subtitleMaxLines,
                        overflow = if (subtitleMaxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
                    )
                }
            }

            if (trailing != null) {
                Spacer(Modifier.width(6.dp))
                trailing()
            } else if (selected) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .clip(CircleShape)
                        .background(resolvedAccent.copy(alpha = 0.24f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = resolvedAccent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
