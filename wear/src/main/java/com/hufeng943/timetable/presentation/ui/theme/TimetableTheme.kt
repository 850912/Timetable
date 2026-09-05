package com.hufeng943.timetable.presentation.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

val AmoledBlackColorScheme = ColorScheme(
    primary = Color(0xFFBB86FC),
    primaryContainer = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC6),
    secondaryContainer = Color(0xFF018786),
    background = Color.Black,
    surfaceContainer = Color(0xFF121212),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCACACA)
)

val DeepBlueColorScheme = ColorScheme(
    primary = Color(0xFF64B5F6),
    primaryContainer = Color(0xFF1976D2),
    secondary = Color(0xFF42A5F5),
    secondaryContainer = Color(0xFF0D47A1),
    background = Color.Black,
    surfaceContainer = Color(0xFF001A33),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0BEC5)
)

val CyanTealColorScheme = ColorScheme(
    primary = Color(0xFF4DB6AC),
    primaryContainer = Color(0xFF00796B),
    secondary = Color(0xFF26A69A),
    secondaryContainer = Color(0xFF004D40),
    background = Color.Black,
    surfaceContainer = Color(0xFF00221A),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB2DFDB)
)

val RoyalPurpleColorScheme = ColorScheme(
    primary = Color(0xFFE040FB),
    primaryContainer = Color(0xFF7B1FA2),
    secondary = Color(0xFFAB47BC),
    secondaryContainer = Color(0xFF4A148C),
    background = Color.Black,
    surfaceContainer = Color(0xFF220033),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFE1BEE7)
)

val SunsetOrangeColorScheme = ColorScheme(
    primary = Color(0xFFFFB74D),
    primaryContainer = Color(0xFFE65100),
    secondary = Color(0xFFFF9800),
    secondaryContainer = Color(0xFFBF360C),
    background = Color.Black,
    surfaceContainer = Color(0xFF331A00),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFFFE0B2)
)

val SakuraPinkColorScheme = ColorScheme(
    primary = Color(0xFFFF80AB),
    primaryContainer = Color(0xFFC2185B),
    secondary = Color(0xFFF06292),
    secondaryContainer = Color(0xFF880E4F),
    background = Color.Black,
    surfaceContainer = Color(0xFF33001A),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFF8BBD0)
)


private fun TimetableColors.fromColorScheme(scheme: ColorScheme): TimetableColors = copy(
    primary = scheme.primary,
    primaryContainer = scheme.primaryContainer,
    secondary = scheme.secondary,
    background = scheme.background,
    surface = scheme.surfaceContainer,
    surfaceContainer = scheme.surfaceContainer,
    courseCurrent = scheme.primaryContainer,
    courseNext = scheme.secondaryContainer,
    courseFinished = scheme.surfaceContainer,
    textPrimary = scheme.onBackground,
    textSecondary = scheme.onSurfaceVariant,
    badgeActive = scheme.secondary,
    weekend = scheme.secondary
)

private fun ThemePreset.baseColors(scheme: ColorScheme): TimetableColors = when (this) {
    ThemePreset.AMOLED_BLACK -> AmoledBlackColors
    ThemePreset.DEEP_BLUE -> DeepBlueColors
    ThemePreset.CYAN_TEAL -> CyanTealColors
    ThemePreset.ROYAL_PURPLE -> RoyalPurpleColors
    ThemePreset.SUNSET_ORANGE -> SunsetOrangeColors
    ThemePreset.SAKURA_PINK -> SakuraPinkColors
    ThemePreset.SYSTEM_DYNAMIC -> AmoledBlackColors.fromColorScheme(scheme)
}

@Composable
fun TimetableTheme(
    themePreset: ThemePreset = ThemePreset.AMOLED_BLACK,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when (themePreset) {
        ThemePreset.SYSTEM_DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicColorScheme(context) ?: AmoledBlackColorScheme
            } else {
                AmoledBlackColorScheme
            }
        }
        ThemePreset.AMOLED_BLACK -> AmoledBlackColorScheme
        ThemePreset.DEEP_BLUE -> DeepBlueColorScheme
        ThemePreset.CYAN_TEAL -> CyanTealColorScheme
        ThemePreset.ROYAL_PURPLE -> RoyalPurpleColorScheme
        ThemePreset.SUNSET_ORANGE -> SunsetOrangeColorScheme
        ThemePreset.SAKURA_PINK -> SakuraPinkColorScheme
        else -> AmoledBlackColorScheme
    }

    val timetableColors = themePreset.baseColors(colorScheme)

    CompositionLocalProvider(
        LocalThemePreset provides themePreset,
        LocalTimetableColors provides timetableColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
