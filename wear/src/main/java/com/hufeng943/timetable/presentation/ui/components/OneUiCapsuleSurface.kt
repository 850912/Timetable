package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
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
) {
    val colors = AppTheme.colors
    val resolvedAccent = when {
        destructive -> MaterialTheme.colorScheme.error
        accentColor != Color.Unspecified -> accentColor
        else -> colors.primary
    }
    val backgroundColor = when {
        destructive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
        selected -> colors.primary.copy(alpha = 0.18f)
        else -> colors.surfaceContainer
    }

    var root = modifier
        .fillMaxWidth()
        .clip(OneUiCapsuleShape)
        .background(backgroundColor)
    root = when {
        onClick != null && onLongClick != null -> root.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        )
        onClick != null -> root.clickable(onClick = onClick)
        else -> root
    }

    Box(modifier = root) {
        if (emphasize || selected) {
            GalaxyAiAmbientLayer(
                shape = OneUiCapsuleShape,
                strength = if (emphasize) 0.55f else 0.35f,
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
                    color = if (destructive) MaterialTheme.colorScheme.onErrorContainer else colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (destructive) {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.78f)
                        } else {
                            colors.textSecondary
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (selected) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .clip(CircleShape)
                        .background(resolvedAccent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
