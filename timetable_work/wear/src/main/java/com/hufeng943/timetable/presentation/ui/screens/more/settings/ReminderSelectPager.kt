package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

@Composable
fun ReminderSelectPager(config: AppConfig, onSelect: (Int?) -> Unit) {
    val values = listOf<Int?>(null, 30, 15, 5)
    val selectedIndex = values.indexOf(config.courseReminderMinutes).coerceAtLeast(0)
    val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = selectedIndex + 1)
    val transform = rememberTransformationSpec()

    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text(stringResource(R.string.settings_course_reminder)) }
            }
            items(values, key = { it ?: 0 }) { minutes ->
                val title = if (minutes == null) {
                    stringResource(R.string.settings_course_reminder_off)
                } else {
                    stringResource(R.string.settings_course_reminder_minutes, minutes)
                }
                val subtitle = if (minutes == null) {
                    stringResource(R.string.settings_course_reminder_off_summary)
                } else {
                    stringResource(R.string.settings_course_reminder_minutes_summary, minutes)
                }
                OneUiCapsuleSurface(
                    title = title,
                    subtitle = subtitle,
                    icon = Icons.Rounded.NotificationsActive,
                    selected = minutes == config.courseReminderMinutes,
                    onClick = { onSelect(minutes) },
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
