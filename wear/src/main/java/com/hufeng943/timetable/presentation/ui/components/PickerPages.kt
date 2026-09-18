package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimePickerType
import java.time.LocalDate
import java.time.LocalTime

/**
 * Hosts Wear Material 3 full-screen pickers on the exact color they use for their edge gradients.
 *
 * TimePicker/DatePicker are already full-screen Wear components. Wrapping them in ScreenScaffold
 * both shrinks their measured viewport and lets the app's custom background show through while the
 * picker gradients are rendered with MaterialTheme.colorScheme.background. On image/glass
 * backgrounds that mismatch becomes visible as dark rectangular bands around unselected values.
 */
@Composable
fun WearTimePickerPage(
    initialTime: LocalTime,
    onTimePicked: (LocalTime) -> Unit,
    timePickerType: TimePickerType,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TimePicker(
            initialTime = initialTime,
            onTimePicked = onTimePicked,
            timePickerType = timePickerType,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun WearDatePickerPage(
    initialDate: LocalDate,
    onDatePicked: (LocalDate) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DatePicker(
            initialDate = initialDate,
            onDatePicked = onDatePicked,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
