package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.DatePickerDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimePickerDefaults
import androidx.wear.compose.material3.TimePickerType
import androidx.wear.compose.material3.rememberPickerState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import java.time.LocalDate
import java.time.LocalTime

/**
 * Shared wheel-picker surface for every app-owned scrolling choice.
 * Keeping one component here avoids the previous mixture of scrolling capsule lists and picker UI.
 */
@Composable
fun <T> WearWheelPickerPage(
    title: String,
    values: List<T>,
    initial: T,
    label: (T) -> String,
    onConfirm: (T) -> Unit,
) {
    require(values.isNotEmpty()) { "WearWheelPickerPage requires at least one option" }
    val initialIndex = remember(values, initial) { values.indexOf(initial).coerceAtLeast(0) }
    val state = rememberPickerState(
        initialNumberOfOptions = values.size,
        initiallySelectedIndex = initialIndex,
        shouldRepeatOptions = false,
    )
    val haptics = rememberWearHaptics()
    val selectedIndex by remember(state, values) {
        derivedStateOf { state.selectedOptionIndex.coerceIn(values.indices) }
    }
    val selected = values[selectedIndex]

    LaunchedEffect(state) {
        snapshotFlow { state.selectedOptionIndex }
            .distinctUntilChanged()
            .drop(1)
            .collect { haptics.tick() }
    }

    ScreenScaffold(
        timeText = {},
        edgeButton = {
            EdgeButton(
                onClick = {
                    haptics.confirm()
                    onConfirm(selected)
                },
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "确认")
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Picker(
                state = state,
                contentDescription = { label(selected) },
                gradientColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(width = 132.dp, height = 124.dp),
            ) { index ->
                val item = values[index]
                val isSelected = index == state.selectedOptionIndex
                Text(
                    text = label(item),
                    textAlign = TextAlign.Center,
                    style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.46f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = title,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Wear Material 3 time picker with the same accent/content palette as app-owned wheel pickers.
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
            colors = TimePickerDefaults.timePickerColors(
                selectedPickerContentColor = MaterialTheme.colorScheme.primary,
                unselectedPickerContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f),
                separatorColor = MaterialTheme.colorScheme.primary,
                pickerLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                confirmButtonContentColor = MaterialTheme.colorScheme.onPrimary,
                confirmButtonContainerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Wear Material 3 date picker, themed to the same selected/unselected treatment. */
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
            colors = DatePickerDefaults.datePickerColors(
                activePickerContentColor = MaterialTheme.colorScheme.primary,
                inactivePickerContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f),
                invalidPickerContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f),
                pickerLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                nextButtonContentColor = MaterialTheme.colorScheme.onPrimary,
                nextButtonContainerColor = MaterialTheme.colorScheme.primary,
                confirmButtonContentColor = MaterialTheme.colorScheme.onPrimary,
                confirmButtonContainerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
