package com.hufeng943.timetable.presentation.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.hufeng943.timetable.data.FirstDayOfTheWeek
import com.hufeng943.timetable.data.TimeFormat
import kotlinx.datetime.DayOfWeek
import com.kyant.backdrop.Backdrop

enum class TimetableBackgroundMode { SOLID, THEME, IMAGE }

enum class LiquidGlassEffect { SOFT, BALANCED, FLUID }

/** Controls app-side power reductions. FOLLOW_SYSTEM uses Android PowerManager. */
enum class AppPowerSaveMode { FOLLOW_SYSTEM, ALWAYS_ON, ALWAYS_OFF }

data class AppConfig(
    val languageTag: String? = null,
    val is24HourFormat: Boolean = true,
    val timeFormatSetting: TimeFormat = TimeFormat.SYSTEM,
    val firstDayOfTheWeekSetting: FirstDayOfTheWeek = FirstDayOfTheWeek.SYSTEM,
    val effectiveFirstDayOfTheWeek: DayOfWeek = DayOfWeek.MONDAY,
    val isDynamicColorEnabled: Boolean = true,
    val isShowTopTime: Boolean = false,
    val uiAnimationsEnabled: Boolean = true,
    val powerSaveMode: AppPowerSaveMode = AppPowerSaveMode.FOLLOW_SYSTEM,
    val isLiquidGlassEnabled: Boolean = false,
    val glassOpacity: Float = 0.42f,
    val glassClarity: Float = 0.70f,
    val liquidGlassEffect: LiquidGlassEffect = LiquidGlassEffect.BALANCED,
    val glassChromaticAberration: Boolean = false,
    val glassLensDistortion: Float = 0.20f,
    val glassBlurEnabled: Boolean = true,
    val glassBlurRadius: Float = 1f,
    val backgroundBrightness: Float = 0.82f,
    val timetableBackgroundMode: TimetableBackgroundMode = TimetableBackgroundMode.THEME,
    val timetableBackgroundImagePath: String? = null,
    val backgroundImageBlurEnabled: Boolean = false,
    val backgroundImageBlurRadius: Float = 6f,
    val backgroundImageFluidEnabled: Boolean = false
)

val LocalAppConfig = staticCompositionLocalOf { AppConfig() }
/** Shared backdrop for liquid-glass surfaces. Null keeps capture work off when glass is disabled. */
val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
