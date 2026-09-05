package com.hufeng943.timetable.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemePreset(val title: String) {
    AMOLED_BLACK("AMOLED 纯黑"),
    DEEP_BLUE("深海蓝"),
    CYAN_TEAL("青绿"),
    ROYAL_PURPLE("极光紫"),
    SUNSET_ORANGE("落日橙"),
    SAKURA_PINK("樱花粉"),
    SYSTEM_DYNAMIC("Material You 动态")
}

data class TimetableColors(
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val courseCurrent: Color,
    val courseNext: Color,
    val courseFinished: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val badgeActive: Color,
    val weekend: Color
)

val AmoledBlackColors = TimetableColors(
    primary = Color(0xFF90CAF9),
    primaryContainer = Color(0xFF1E3A5F),
    secondary = Color(0xFF81D4FA),
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    surfaceContainer = Color(0xFF1E1E1E),
    courseCurrent = Color(0xFF1B5E20),
    courseNext = Color(0xFF0D47A1),
    courseFinished = Color(0xFF262626),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB0B0B0),
    badgeActive = Color(0xFF4CAF50),
    weekend = Color(0xFFFFB74D)
)

val DeepBlueColors = TimetableColors(
    primary = Color(0xFF64B5F6),
    primaryContainer = Color(0xFF102A43),
    secondary = Color(0xFF4FC3F7),
    background = Color(0xFF051124),
    surface = Color(0xFF0D2142),
    surfaceContainer = Color(0xFF153363),
    courseCurrent = Color(0xFF00695C),
    courseNext = Color(0xFF1565C0),
    courseFinished = Color(0xFF1A2A3A),
    textPrimary = Color(0xFFE3F2FD),
    textSecondary = Color(0xFF90CAF9),
    badgeActive = Color(0xFF00E676),
    weekend = Color(0xFFFFCC80)
)

val CyanTealColors = TimetableColors(
    primary = Color(0xFF4DB6AC),
    primaryContainer = Color(0xFF00363A),
    secondary = Color(0xFF80CBC4),
    background = Color(0xFF001514),
    surface = Color(0xFF002B28),
    surfaceContainer = Color(0xFF00433E),
    courseCurrent = Color(0xFF00796B),
    courseNext = Color(0xFF00838F),
    courseFinished = Color(0xFF1A2E2C),
    textPrimary = Color(0xFFE0F2F1),
    textSecondary = Color(0xFF80CBC4),
    badgeActive = Color(0xFF64FFDA),
    weekend = Color(0xFFFFD54F)
)

val RoyalPurpleColors = TimetableColors(
    primary = Color(0xFFBA68C8),
    primaryContainer = Color(0xFF311B92),
    secondary = Color(0xFFCE93D8),
    background = Color(0xFF12051E),
    surface = Color(0xFF240E3B),
    surfaceContainer = Color(0xFF381A5A),
    courseCurrent = Color(0xFF6A1B9A),
    courseNext = Color(0xFF4527A0),
    courseFinished = Color(0xFF2A1B30),
    textPrimary = Color(0xFFF3E5F5),
    textSecondary = Color(0xFFCE93D8),
    badgeActive = Color(0xFFE040FB),
    weekend = Color(0xFFFFAB91)
)

val SunsetOrangeColors = TimetableColors(
    primary = Color(0xFFFFB74D),
    primaryContainer = Color(0xFF4E2600),
    secondary = Color(0xFFFFCC80),
    background = Color(0xFF1A0A00),
    surface = Color(0xFF331700),
    surfaceContainer = Color(0xFF4D2400),
    courseCurrent = Color(0xFFBF360C),
    courseNext = Color(0xFFE65100),
    courseFinished = Color(0xFF2D1B10),
    textPrimary = Color(0xFFFFF3E0),
    textSecondary = Color(0xFFFFCC80),
    badgeActive = Color(0xFFFF9100),
    weekend = Color(0xFF80D8FF)
)

val SakuraPinkColors = TimetableColors(
    primary = Color(0xFFF48FB1),
    primaryContainer = Color(0xFF4A0023),
    secondary = Color(0xFFF8BBD0),
    background = Color(0xFF1E050D),
    surface = Color(0xFF3B0E1E),
    surfaceContainer = Color(0xFF5A1A31),
    courseCurrent = Color(0xFF880E4F),
    courseNext = Color(0xFFAD1457),
    courseFinished = Color(0xFF2E1A22),
    textPrimary = Color(0xFFFCE4EC),
    textSecondary = Color(0xFFF48FB1),
    badgeActive = Color(0xFFFF4081),
    weekend = Color(0xFFB388FF)
)

val LocalTimetableColors = staticCompositionLocalOf { AmoledBlackColors }
val LocalThemePreset = staticCompositionLocalOf { ThemePreset.AMOLED_BLACK }

object AppTheme {
    val colors: TimetableColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTimetableColors.current
}
