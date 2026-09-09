package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import com.hufeng943.timetable.presentation.ui.common.ui.CourseUi
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
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val courseColor = course.displayColor
    val slot = course.timeSlot
    val order = course.dailyOrder?.toString() ?: "•"
    val pulse = if (isNext) {
        val transition = rememberInfiniteTransition(label = "nextCoursePulse")
        transition.animateFloat(0.82f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha").value
    } else 1f

    Card(
        onClick = onClick,
        modifier = modifier.alpha(pulse),
        transformation = transformation,
        shape = CourseCapsuleShape,
        colors = CardDefaults.cardColors(
            containerColor = courseColor.copy(alpha = if (isCurrent) 0.28f else 0.14f).compositeOver(colors.surfaceContainer),
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
                strength = 0.55f,
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
                        .background(galaxyAiAccentBrush(courseColor, colors.primary, colors.secondary))
                )

                Spacer(Modifier.width(9.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = course.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = buildString {
                            slot.startTime?.let { append("%02d:%02d".format(it.hour, it.minute)) }
                            if (slot.startTime != null && slot.endTime != null) append(" – ")
                            slot.endTime?.let { append("%02d:%02d".format(it.hour, it.minute)) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )

                    if (isCurrent && minutesLeft != null) {
                        Text(
                            text = "上课中 · 距离下课还有 ${minutesLeft} 分钟",
                            style = MaterialTheme.typography.labelSmall,
                            color = courseColor,
                            maxLines = 1,
                        )
                    } else if (isNext) {
                        Text(
                            text = "下一节课程",
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
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = order,
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
