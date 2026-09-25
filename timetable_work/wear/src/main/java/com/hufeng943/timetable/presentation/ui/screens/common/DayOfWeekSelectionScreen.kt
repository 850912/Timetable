package com.hufeng943.timetable.presentation.ui.screens.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.components.WearWheelPickerPage
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import kotlinx.datetime.DayOfWeek
import java.time.format.TextStyle

@Composable
fun DayOfWeekSelectionScreen(initialDay: DayOfWeek?, onDaySelected: (DayOfWeek) -> Unit) {
    val firstDay = LocalAppConfig.current.effectiveFirstDayOfTheWeek
    val days = remember(firstDay) {
        DayOfWeek.entries.sortedBy { (it.ordinal - firstDay.ordinal + 7) % 7 }
    }
    WearWheelPickerPage(
        title = stringResource(R.string.selection_week),
        values = days,
        initial = initialDay?.takeIf { it in days } ?: days.first(),
        label = { it.toDisplayString(TextStyle.FULL_STANDALONE) },
        onConfirm = onDaySelected,
    )
}
