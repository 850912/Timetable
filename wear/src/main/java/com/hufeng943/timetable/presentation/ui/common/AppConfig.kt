package com.hufeng943.timetable.presentation.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.hufeng943.timetable.data.FirstDayOfTheWeek
import com.hufeng943.timetable.data.TimeFormat
import kotlinx.datetime.DayOfWeek
import com.kyant.backdrop.Backdrop

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
    val isLiquidGlassEnabled: Boolean = false,
    val isFrostedGlassEnabled: Boolean = false,
    val glassOpacity: Float = 0.42f,
    val liquidGlassEffect: LiquidGlassEffect = LiquidGlassEffect.BALANCED,
    val glassHighSaturation: Boolean = false,
    val glassChromaticAberration: Boolean = false,
    val glassLensDistortion: Float = 0.20f,
    val glassBlurEnabled: Boolean = true,
    val glassBlurRadius: Float = 1f,
    val blurredBackgroundEnabled: Boolean = false,
    val backgroundBlurRadius: Float = 4f,
    val backgroundBrightness: Float = 0.62f,
    val timetableBackgroundMode: TimetableBackgroundMode = TimetableBackgroundMode.THEME,
    val timetableBackgroundImagePath: String? = null
)

val LocalAppConfig = staticCompositionLocalOf { AppConfig() }
val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
