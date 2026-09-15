package com.hufeng943.timetable.presentation.ui.components.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.wear.compose.material3.SurfaceTransformation
import com.hufeng943.timetable.R
import androidx.compose.ui.res.stringResource
import com.hufeng943.timetable.presentation.ui.common.ui.TimeSlotUi
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.shared.model.WeekPattern
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
    val dateText = if (timeSlot.recurrence == WeekPattern.DATE_ONLY && timeSlot.selectedDates.isNotEmpty()) {
        val dates = timeSlot.selectedDates.sorted()
        if (dates.size == 1) dates.first().toDisplayString() else "${dates.first().toDisplayString()} · ${dates.size} 天"
    } else {
        listOf(timeSlot.recurrence.toDisplayString(), dayText).joinToString(stringResource(R.string.info_separator))
    }
    val groupHint = if (!timeSlot.batchGroupId.isNullOrBlank()) " · 多日期组" else ""
    val subtitle = listOf(dateText + groupHint, timeSlot.remark).filter { !it.isNullOrBlank() }.joinToString(" · ")

    OneUiCapsuleSurface(
        title = if (time.isBlank()) dateText else time,
        subtitle = subtitle,
        accentColor = timeSlot.color,
        onClick = onClick,
        modifier = modifier,
    )
}
