package com.hufeng943.timetable.presentation.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.hufeng943.timetable.data.FirstDayOfTheWeek
import com.hufeng943.timetable.data.TimeFormat
import kotlinx.datetime.DayOfWeek
import dev.chrisbanes.haze.HazeState

enum class TimetableBackgroundMode { SOLID, THEME, IMAGE }

enum class LiquidGlassEffect { SOFT, BALANCED, FLUID }

data class AppConfig(
    val languageTag: String? = null,
    val is24HourFormat: Boolean = true,
    val timeFormatSetting: TimeFormat = TimeFormat.SYSTEM,
    val firstDayOfTheWeekSetting: FirstDayOfTheWeek = FirstDayOfTheWeek.SYSTEM,
    val effectiveFirstDayOfTheWeek: DayOfWeek = DayOfWeek.MONDAY,
    val isDynamicColorEnabled: Boolean = true,
    val isShowTopTime: Boolean = false,
    val uiAnimationsEnabled: Boolean = true,
    val conditionalUiEnabled: Boolean = true,
    val isLiquidGlassEnabled: Boolean = false,
    val isFrostedGlassEnabled: Boolean = false,
    val isGlobalGlassMaterialEnabled: Boolean = false,
    val glassOpacity: Float = 0.42f,
    val liquidGlassEffect: LiquidGlassEffect = LiquidGlassEffect.BALANCED,
    val glassChromaticAberration: Boolean = false,
    val glassLensDistortion: Float = 0.20f,
    val glassBlurEnabled: Boolean = true,
    val glassBlurRadius: Float = 1f,
    val backgroundBrightness: Float = 0.82f,
    val timetableBackgroundMode: TimetableBackgroundMode = TimetableBackgroundMode.THEME,
    val timetableBackgroundImagePath: String? = null
)

val LocalAppConfig = staticCompositionLocalOf { AppConfig() }
/** Shared Haze state for the entire app. Kept nullable so glass can be disabled without capture work. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

// Compatibility name while screens migrate; the implementation is Haze-only.
@Deprecated("Use LocalHazeState")
val LocalLiquidGlassBackdrop = LocalHazeState
