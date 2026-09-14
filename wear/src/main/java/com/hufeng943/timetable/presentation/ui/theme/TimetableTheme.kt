package com.hufeng943.timetable.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

val AmoledBlackColorScheme = ColorScheme(
    primary = Color(0xFFE8EAED),
    primaryContainer = Color(0xFF25272B),
    secondary = Color(0xFFAEB4BC),
    secondaryContainer = Color(0xFF23262A),
    background = Color.Black,
    surfaceContainer = Color(0xFF1A1B1F),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB8BDC5)
)

val DeepBlueColorScheme = ColorScheme(
    primary = Color(0xFF9CB7CF),
    primaryContainer = Color(0xFF29445A),
    secondary = Color(0xFF8FAABD),
    secondaryContainer = Color(0xFF203443),
    background = Color.Black,
    surfaceContainer = Color(0xFF181A1F),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB8C1C8)
)

val CyanTealColorScheme = ColorScheme(
    primary = Color(0xFF91B9B3),
    primaryContainer = Color(0xFF31524D),
    secondary = Color(0xFF87AAA5),
    secondaryContainer = Color(0xFF263E3A),
    background = Color.Black,
    surfaceContainer = Color(0xFF181A1F),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB8CBC8)
)

val RoyalPurpleColorScheme = ColorScheme(
    primary = Color(0xFFB9A7C8),
    primaryContainer = Color(0xFF493A55),
    secondary = Color(0xFFA997B7),
    secondaryContainer = Color(0xFF362C40),
    background = Color.Black,
    surfaceContainer = Color(0xFF181A1F),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFC8BECF)
)

val SunsetOrangeColorScheme = ColorScheme(
    primary = Color(0xFFC9AD8E),
    primaryContainer = Color(0xFF594536),
    secondary = Color(0xFFBDA58C),
    secondaryContainer = Color(0xFF43352C),
    background = Color.Black,
    surfaceContainer = Color(0xFF181A1F),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFD3C6B8)
)


val GraphiteColorScheme = ColorScheme(
    primary = Color(0xFFD6C8B8),
    primaryContainer = Color(0xFF3B342E),
    secondary = Color(0xFFB8AEA3),
    secondaryContainer = Color(0xFF2D2925),
    background = Color(0xFF050505),
    surfaceContainer = Color(0xFF1B1917),
    onPrimary = Color(0xFF211C17),
    onSecondary = Color(0xFF211C17),
    onBackground = Color(0xFFF1ECE6),
    onSurface = Color(0xFFF1ECE6),
    onSurfaceVariant = Color(0xFFC3BAB0)
)

val AuroraColorScheme = ColorScheme(
    primary = Color(0xFF9ABDB6),
    primaryContainer = Color(0xFF29413C),
    secondary = Color(0xFFAFA9C4),
    secondaryContainer = Color(0xFF393546),
    background = Color(0xFF030505),
    surfaceContainer = Color(0xFF171A19),
    onPrimary = Color(0xFF14201D),
    onSecondary = Color(0xFF1D1A25),
    onBackground = Color(0xFFEAF1EF),
    onSurface = Color(0xFFEAF1EF),
    onSurfaceVariant = Color(0xFFBAC6C3)
)
val SakuraPinkColorScheme = ColorScheme(
    primary = Color(0xFFC4A5B0),
    primaryContainer = Color(0xFF533A43),
    secondary = Color(0xFFB79AA4),
    secondaryContainer = Color(0xFF3E2C33),
    background = Color.Black,
    surfaceContainer = Color(0xFF181A1F),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFD0C0C5)
)


private fun TimetableColors.fromColorScheme(scheme: ColorScheme): TimetableColors = copy(
    primary = scheme.primary,
    primaryContainer = scheme.primaryContainer,
    secondary = scheme.secondary,
    background = scheme.background,
    surface = scheme.surfaceContainer,
    surfaceContainer = scheme.surfaceContainer,
    // Keep schedule semantic/accent colors owned by the base preset. Theme selection
    // only changes the shell colors; saved timetable/course seed colors remain authoritative.
    textPrimary = scheme.onBackground,
    textSecondary = scheme.onSurfaceVariant
)

private fun ThemePreset.baseColors(scheme: ColorScheme): TimetableColors = when (this) {
    ThemePreset.AMOLED_BLACK -> AmoledBlackColors
    ThemePreset.DEEP_BLUE -> DeepBlueColors
    ThemePreset.CYAN_TEAL -> CyanTealColors
    ThemePreset.ROYAL_PURPLE -> RoyalPurpleColors
    ThemePreset.SUNSET_ORANGE -> SunsetOrangeColors
    ThemePreset.SAKURA_PINK -> SakuraPinkColors
    ThemePreset.GRAPHITE -> AmoledBlackColors.fromColorScheme(scheme)
    ThemePreset.AURORA -> AmoledBlackColors.fromColorScheme(scheme)
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
            dynamicColorScheme(context) ?: AmoledBlackColorScheme
        }
        ThemePreset.AMOLED_BLACK -> AmoledBlackColorScheme
        ThemePreset.DEEP_BLUE -> DeepBlueColorScheme
        ThemePreset.CYAN_TEAL -> CyanTealColorScheme
        ThemePreset.ROYAL_PURPLE -> RoyalPurpleColorScheme
        ThemePreset.SUNSET_ORANGE -> SunsetOrangeColorScheme
        ThemePreset.SAKURA_PINK -> SakuraPinkColorScheme
        ThemePreset.GRAPHITE -> GraphiteColorScheme
        ThemePreset.AURORA -> AuroraColorScheme
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
