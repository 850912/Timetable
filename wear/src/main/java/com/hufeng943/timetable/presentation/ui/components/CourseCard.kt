package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.ui.CourseUi
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
import com.hufeng943.timetable.presentation.ui.theme.GalaxyAiAmbientLayer
import com.hufeng943.timetable.presentation.ui.theme.galaxyAiAccentBrush

private val CourseCapsuleShape = RoundedCornerShape(28.dp)

@Composable
fun CourseCard(
    course: CourseUi,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
    isCurrent: Boolean = false,
    isNext: Boolean = false,
    minutesLeft: Int? = null,
    nextCourseName: String? = null,
    minutesUntilNext: Int? = null,
    is24HourFormat: Boolean = true,
    liquidGlassBackdrop: Backdrop? = null,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val glassConfig = LocalAppConfig.current
    val courseColor = course.displayColor
    val slot = course.timeSlot
    val order = course.dailyOrder?.toString() ?: "•"
    val classProgress = if (isCurrent && minutesLeft != null && slot.startTime != null && slot.endTime != null) {
        val startMinutes = slot.startTime.hour * 60 + slot.startTime.minute
        val endMinutes = slot.endTime.hour * 60 + slot.endTime.minute
        val total = (endMinutes - startMinutes).coerceAtLeast(1)
        ((total - minutesLeft).toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } else null
    val topHighlightBrush = remember(courseColor, isCurrent) {
        Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                courseColor.copy(alpha = if (isCurrent) 0.90f else 0.45f),
                Color.White.copy(alpha = 0.24f),
                Color.Transparent,
            )
        )
    }
    val accentBrush = remember(courseColor, colors.primary, colors.secondary) {
        galaxyAiAccentBrush(courseColor, colors.primary, colors.secondary)
    }
    val progressBrush = remember(courseColor) {
        Brush.horizontalGradient(listOf(courseColor.copy(alpha = 0.72f), courseColor))
    }
    val baseCardModifier = when {
        isCurrent -> modifier.border(2.dp, courseColor.copy(alpha = 0.98f), CourseCapsuleShape)
        isNext -> modifier.border(1.dp, courseColor.copy(alpha = 0.46f), CourseCapsuleShape)
        else -> modifier.border(0.8.dp, Color.White.copy(alpha = 0.16f), CourseCapsuleShape)
    }
    val cardModifier = if (liquidGlassBackdrop != null) {
        baseCardModifier.drawBackdrop(
            backdrop = liquidGlassBackdrop,
            shape = { CourseCapsuleShape },
            effects = {
                // Expensive effects remain limited to current/next cards. Each pass is independently
                // configurable so users can trade appearance for battery life without multiplying
                // shader work across the whole list.
                if (glassConfig.glassHighSaturation) vibrancy()
                if (glassConfig.glassBlurEnabled && glassConfig.glassBlurRadius > 0f) {
                    blur(glassConfig.glassBlurRadius.dp.toPx())
                }
                if (glassConfig.glassLensDistortion > 0f) {
                    val amount = glassConfig.glassLensDistortion
                    val inner = (if (isCurrent) 16f else 12f) * amount
                    val outer = (if (isCurrent) 24f else 18f) * amount
                    lens(
                        inner.dp.toPx(),
                        outer.dp.toPx(),
                        chromaticAberration = glassConfig.glassChromaticAberration,
                    )
                }
            },
            highlight = { Highlight.Ambient.copy(alpha = if (isCurrent) 0.80f else 0.58f) },
            shadow = { Shadow(radius = 3.dp, color = Color.Black.copy(alpha = 0.18f)) },
            innerShadow = { InnerShadow(radius = 4.dp, alpha = 0.18f) },
            onDrawSurface = {
                drawRect(courseColor.copy(alpha = glassConfig.glassOpacity * if (isCurrent) 0.34f else 0.20f))
                drawRect(Color.White.copy(alpha = 0.025f))
            }
        )
    } else baseCardModifier

    Card(
        onClick = onClick,
        modifier = cardModifier,
        transformation = transformation,
        shape = CourseCapsuleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (liquidGlassBackdrop != null) {
                Color.Transparent
            } else {
                // Keep the normal mode genuinely translucent instead of pre-compositing it
                // into an opaque surface. This preserves the glass look without a shader.
                courseColor.copy(alpha = if (glassConfig.isLiquidGlassEnabled) glassConfig.glassOpacity * (if (isCurrent) 0.40f else if (isNext) 0.28f else 0.20f) else if (isCurrent) 0.20f else if (isNext) 0.115f else 0.07f)
            },
            contentColor = colors.textPrimary,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clip(CourseCapsuleShape)
        ) {
            GalaxyAiAmbientLayer(
                shape = CourseCapsuleShape,
                strength = if (liquidGlassBackdrop != null) 0.16f else if (isCurrent) 0.58f else if (isNext) 0.36f else 0.22f,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(topHighlightBrush)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentBrush)
                )

                Spacer(Modifier.width(9.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = course.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip,
                    )

                    Text(
                        text = buildString {
                            slot.startTime?.let { append(it.toDisplayString(is24HourFormat)) }
                            if (slot.startTime != null && slot.endTime != null) append(" – ")
                            slot.endTime?.let { append(it.toDisplayString(is24HourFormat)) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = Int.MAX_VALUE,
                    )

                    if (isCurrent && minutesLeft != null) {
                        Text(
                            text = stringResource(R.string.course_in_progress, minutesLeft),
                            style = MaterialTheme.typography.labelMedium,
                            color = courseColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        classProgress?.let { progress ->
                            Spacer(Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(progressBrush)
                                )
                            }
                        }
                        if (!nextCourseName.isNullOrBlank() && minutesUntilNext != null) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = stringResource(R.string.course_class_mode_next, nextCourseName, minutesUntilNext),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                maxLines = Int.MAX_VALUE,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    } else if (isNext) {
                        Text(
                            text = stringResource(R.string.course_next),
                            style = MaterialTheme.typography.labelSmall,
                            color = courseColor,
                            maxLines = 1,
                        )
                    }

                    val detail = listOfNotNull(
                        course.location?.takeIf { it.isNotBlank() },
                        course.teacher?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")
                    if (detail.isNotEmpty()) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary.copy(alpha = 0.82f),
                            maxLines = Int.MAX_VALUE,
                            overflow = TextOverflow.Clip,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(courseColor.copy(alpha = if (isCurrent) 0.26f else 0.16f))
                        .border(0.8.dp, courseColor.copy(alpha = 0.42f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = order,
                        color = if (isCurrent) Color.White else colors.textPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}


@Composable
fun DayFinishedCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
) {
    val colors = AppTheme.colors
    val accent = colors.primary

    Card(
        onClick = {},
        modifier = modifier,
        transformation = transformation,
        shape = CourseCapsuleShape,
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.20f).compositeOver(colors.surfaceContainer),
            contentColor = colors.textPrimary,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clip(CourseCapsuleShape)
        ) {
            GalaxyAiAmbientLayer(
                shape = CourseCapsuleShape,
                strength = 0.40f,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, accent.copy(alpha = 0.55f), Color.White.copy(alpha = 0.20f), Color.Transparent)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(galaxyAiAccentBrush(accent, colors.secondary, accent))
                )

                Spacer(Modifier.width(9.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip,
                    )
                }

                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f))
                        .border(0.8.dp, accent.copy(alpha = 0.32f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
