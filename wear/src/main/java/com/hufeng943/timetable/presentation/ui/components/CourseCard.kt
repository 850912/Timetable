package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.ui.CourseUi
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
import com.hufeng943.timetable.presentation.ui.theme.GalaxyAiAmbientLayer
import com.hufeng943.timetable.presentation.ui.theme.galaxyAiAccentBrush

private val CourseCapsuleShape = RoundedCornerShape(30.dp)
private val DetailPillShape = RoundedCornerShape(50)

/**
 * Round-screen course card inspired by Samsung's media cards: the information layer stays
 * inside the circular safe area while a soft "artwork" block lives on the right. Keeping the
 * card content height deterministic also avoids the old clipped-card issue on Galaxy Watch.
 */
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
    val courseColor = course.displayColor
    val slot = course.timeSlot
    val order = course.dailyOrder?.toString() ?: "•"
    val cardModifier = when {
        isCurrent -> modifier.border(1.6.dp, courseColor.copy(alpha = 0.95f), CourseCapsuleShape)
        isNext -> modifier.border(1.dp, courseColor.copy(alpha = 0.48f), CourseCapsuleShape)
        else -> modifier.border(0.6.dp, Color.White.copy(alpha = 0.07f), CourseCapsuleShape)
    }

    val timeText = buildString {
        slot.startTime?.let { append(it.toDisplayString(is24HourFormat)) }
        if (slot.startTime != null && slot.endTime != null) append(" – ")
        slot.endTime?.let { append(it.toDisplayString(is24HourFormat)) }
    }
    val detail = listOfNotNull(
        course.location?.takeIf { it.isNotBlank() },
        course.teacher?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    val status = when {
        isCurrent && minutesLeft != null -> stringResource(R.string.course_in_progress, minutesLeft)
        isNext -> stringResource(R.string.course_next)
        else -> null
    }

    Card(
        onClick = onClick,
        modifier = cardModifier,
        transformation = transformation,
        shape = CourseCapsuleShape,
        colors = CardDefaults.cardColors(
            containerColor = courseColor.copy(alpha = if (isCurrent) 0.22f else if (isNext) 0.13f else 0.08f)
                .compositeOver(colors.surfaceContainer),
            contentColor = colors.textPrimary,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .clip(CourseCapsuleShape)
        ) {
            GalaxyAiAmbientLayer(
                shape = CourseCapsuleShape,
                strength = if (isCurrent) 0.52f else if (isNext) 0.32f else 0.18f,
            )

            // Samsung media-card-like artwork field. It is decorative and intentionally static
            // (no shader/animation) to keep GPU and battery cost low.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(86.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                courseColor.copy(alpha = 0.42f),
                                colors.primary.copy(alpha = 0.12f),
                                Color.Transparent,
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 13.dp, end = 12.dp, top = 11.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = course.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (timeText.isNotBlank()) {
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrent || isNext) courseColor.copy(alpha = 0.88f)
                                else Color.White.copy(alpha = 0.11f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = order,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCurrent || isNext) Color.Black else colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (!status.isNullOrBlank() || detail.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(DetailPillShape)
                            .background(Color.Black.copy(alpha = 0.34f))
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = status ?: detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (status != null) courseColor else colors.textPrimary,
                            fontWeight = if (status != null) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (isCurrent && !nextCourseName.isNullOrBlank() && minutesUntilNext != null) {
                    Text(
                        text = stringResource(R.string.course_class_mode_next, nextCourseName, minutesUntilNext),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 2,
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
