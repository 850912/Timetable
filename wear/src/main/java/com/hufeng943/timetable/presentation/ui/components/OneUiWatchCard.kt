package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalLiquidGlassBackdrop
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
import kotlinx.datetime.LocalTime

private val WatchCourseCardShape = RoundedCornerShape(22.dp)

data class WearCourseCardData(
    val courseName: String,
    val teacher: String?,
    val location: String?,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val countdownState: LiveCountdownState,
)

/**
 * Shared custom course card surface for Wear OS. 3.5.3 moves presentation inputs into one immutable
 * model so list callers can create/stabilize card data independently of Compose and so every card
 * uses the same glass, press and text layout path.
 */
@Composable
fun WearCourseCard(
    data: WearCourseCardData,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val glassConfig = LocalAppConfig.current
    val cardBackground = when (data.countdownState.status) {
        CourseStatus.IN_PROGRESS -> colors.courseCurrent
        CourseStatus.NOT_STARTED -> colors.surfaceContainer
        CourseStatus.FINISHED -> colors.surface
    }
    val statusColor = when (data.countdownState.status) {
        CourseStatus.IN_PROGRESS -> colors.badgeActive
        CourseStatus.NOT_STARTED -> colors.primary
        CourseStatus.FINISHED -> colors.textSecondary
    }
    val timeRangeText = remember(data.startTime, data.endTime) {
        "%02d:%02d - %02d:%02d".format(
            data.startTime.hour, data.startTime.minute, data.endTime.hour, data.endTime.minute
        )
    }
    val (interactionSource, pressMotion) = rememberPressMotion()

    Box(
        modifier = modifier
            .then(pressMotion)
            .fillMaxWidth()
            .clip(WatchCourseCardShape)
            .globalLiquidGlass(WatchCourseCardShape, cardBackground)
            .background(
                cardBackground.copy(
                    alpha = if (LocalLiquidGlassBackdrop.current != null && glassConfig.isLiquidGlassEnabled) 0f else 1f
                )
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(statusColor, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = data.countdownState.label,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = data.countdownState.countdownText,
                    color = if (data.countdownState.status == CourseStatus.IN_PROGRESS) colors.badgeActive else colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = data.courseName,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text(
                    text = timeRangeText,
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                )
            }

            val location = data.location?.takeIf { it.isNotBlank() }
            val teacher = data.teacher?.takeIf { it.isNotBlank() }
            if (location != null || teacher != null) {
                Spacer(Modifier.height(2.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (location != null) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = location,
                            color = colors.textSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (teacher != null) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(text = teacher, color = colors.textSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** Compatibility wrapper for existing callers and previews. */
@Composable
fun OneUiWatchCard(
    courseName: String,
    teacher: String?,
    location: String?,
    startTime: LocalTime,
    endTime: LocalTime,
    countdownState: LiveCountdownState,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) = WearCourseCard(
    data = WearCourseCardData(courseName, teacher, location, startTime, endTime, countdownState),
    onClick = onClick,
    modifier = modifier,
)
