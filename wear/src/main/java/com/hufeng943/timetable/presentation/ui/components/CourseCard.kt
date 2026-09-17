package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
                Color.White.copy(alpha = if (isCurrent) 0.34f else 0.20f),
                courseColor.copy(alpha = if (isCurrent) 0.42f else 0.22f),
                Color.Transparent,
            )
        )
    }
    // Course colour is an accent reflected by the glass, not the material itself.
    // Keeping the body optically neutral is what prevents the old coloured-plastic look.
    val edgeReflectionBrush = remember(courseColor, isCurrent) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (isCurrent) 0.62f else 0.34f),
                courseColor.copy(alpha = if (isCurrent) 0.90f else 0.66f),
                courseColor.copy(alpha = 0.20f),
            )
        )
    }
    val progressBrush = remember(courseColor) {
        Brush.horizontalGradient(listOf(courseColor.copy(alpha = 0.72f), courseColor))
    }
    val glassActive = glassConfig.isLiquidGlassEnabled
    val baseCardModifier = if (glassActive) {
        modifier
    } else {
        when {
            isCurrent -> modifier.border(0.8.dp, Color.White.copy(alpha = 0.30f), CourseCapsuleShape)
            isNext -> modifier.border(0.65.dp, Color.White.copy(alpha = 0.20f), CourseCapsuleShape)
            else -> modifier.border(0.5.dp, Color.White.copy(alpha = 0.11f), CourseCapsuleShape)
        }
    }
    val cardModifier = if (glassActive) {
        // Kyant already renders the optical edge/highlight. Avoid a second full-card border pass.
        baseCardModifier.globalLiquidGlass(CourseCapsuleShape, Color.Transparent)
    } else baseCardModifier

    Card(
        onClick = onClick,
        modifier = cardModifier,
        transformation = transformation,
        shape = CourseCapsuleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (glassActive) {
                Color.Transparent
            } else {
                // Keep the normal mode genuinely translucent instead of pre-compositing it
                // into an opaque surface. This preserves the glass look without a shader.
                colors.surfaceContainer.copy(alpha = if (isCurrent) 0.78f else if (isNext) 0.66f else 0.56f)
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
            if (!glassActive) {
                GalaxyAiAmbientLayer(
                    shape = CourseCapsuleShape,
                    strength = if (isCurrent) 0.36f else if (isNext) 0.24f else 0.14f,
                )
            }
            // No full-card colour wash in glass mode: it turns refraction into coloured plastic.
            if (!glassActive) {
                // Non-glass fallback keeps a cheap highlight. In glass mode Kyant already draws
                // the optical highlight, so drawing this again only adds overdraw.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(topHighlightBrush)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(if (isCurrent) 3.dp else 2.dp)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(edgeReflectionBrush)
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = buildString {
                            slot.startTime?.let { append(it.toDisplayString(is24HourFormat)) }
                            if (slot.startTime != null && slot.endTime != null) append(" – ")
                            slot.endTime?.let { append(it.toDisplayString(is24HourFormat)) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(0.65.dp, Color.White.copy(alpha = if (isCurrent) 0.30f else 0.16f), CircleShape),
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
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
