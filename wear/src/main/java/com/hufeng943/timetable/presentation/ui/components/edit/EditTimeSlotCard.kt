package com.hufeng943.timetable.presentation.ui.components.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.SurfaceTransformation
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.ui.TimeSlotUi
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import java.time.format.TextStyle

@Composable
fun EditTimeSlotCard(
    timeSlot: TimeSlotUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null
) {
    val locale = LocalLocale.current.platformLocale
    val dayText = remember(timeSlot.dayOfWeek, locale) {
        timeSlot.dayOfWeek?.toDisplayString(TextStyle.SHORT_STANDALONE)
    } ?: stringResource(R.string.unknown)

    val time = listOfNotNull(timeSlot.startTime, timeSlot.endTime).joinToString(" - ")
    val recurrence = listOf(timeSlot.recurrence.toDisplayString(), dayText)
        .joinToString(stringResource(R.string.info_separator))
    val subtitle = listOf(recurrence, timeSlot.remark).filter { !it.isNullOrBlank() }.joinToString(" · ")

    OneUiCapsuleSurface(
        title = if (time.isBlank()) recurrence else time,
        subtitle = subtitle,
        accentColor = timeSlot.color,
        onClick = onClick,
        modifier = modifier,
    )
}
