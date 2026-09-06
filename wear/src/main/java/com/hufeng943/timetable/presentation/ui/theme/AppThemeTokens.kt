package com.hufeng943.timetable.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemePreset(val title: String) {
    AMOLED_BLACK("曜石黑"),
    DEEP_BLUE("Galaxy 蓝"),
    CYAN_TEAL("薄荷青"),
    ROYAL_PURPLE("星云紫"),
    SUNSET_ORANGE("活力橙"),
    SAKURA_PINK("珊瑚粉"),
    SYSTEM_DYNAMIC("系统动态色")
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
    primary = Color(0xFFE8EAED),
    primaryContainer = Color(0xFF25272B),
    secondary = Color(0xFFAEB4BC),
    background = Color(0xFF000000),
    surface = Color(0xFF101113),
    surfaceContainer = Color(0xFF1A1B1F),
    courseCurrent = Color(0xFF1B5E20),
    courseNext = Color(0xFF263238),
    courseFinished = Color(0xFF262626),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB0B0B0),
    badgeActive = Color(0xFFB8C7DA),
    weekend = Color(0xFFFFB74D)
)

val DeepBlueColors = TimetableColors(
    primary = Color(0xFF64B5F6),
    primaryContainer = Color(0xFF102A43),
    secondary = Color(0xFF4FC3F7),
    background = Color.Black,
    surface = Color(0xFF101114),
    surfaceContainer = Color(0xFF1A1B1F),
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
    background = Color.Black,
    surface = Color(0xFF101114),
    surfaceContainer = Color(0xFF1A1B1F),
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
    background = Color.Black,
    surface = Color(0xFF101114),
    surfaceContainer = Color(0xFF1A1B1F),
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
    background = Color.Black,
    surface = Color(0xFF101114),
    surfaceContainer = Color(0xFF1A1B1F),
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
    background = Color.Black,
    surface = Color(0xFF101114),
    surfaceContainer = Color(0xFF1A1B1F),
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
