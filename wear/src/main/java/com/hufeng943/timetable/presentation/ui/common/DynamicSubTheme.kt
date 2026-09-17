package com.hufeng943.timetable.presentation.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Course/timetable accent colors are data, not an app theme.
 *
 * The old implementation rebuilt a Vibrant Material color scheme from every course color when
 * SYSTEM_DYNAMIC was selected. That masked Wear OS system dynamic colors and made many screens
 * excessively saturated. Keep this wrapper for call-site compatibility, but let the global
 * TimetableTheme own MaterialTheme. Course/timetable accents continue to be rendered by the
 * individual cards/components that consume [seedColor].
 */
@Composable
fun DynamicSubTheme(
    seedColor: Color,
    isDark: Boolean = true,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    @Suppress("UNUSED_VARIABLE")
    val keepAccentDataSeparate = Triple(seedColor, isDark, isAmoled)
    content()
}
