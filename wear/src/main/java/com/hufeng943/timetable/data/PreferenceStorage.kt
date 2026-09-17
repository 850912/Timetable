package com.hufeng943.timetable.data

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.common.TimetableBackgroundMode
import com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "timetable_settings")

@Singleton
class PreferenceStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val LOCALE_MIRROR_PREFS = "locale_mirror"
        private const val LOCALE_MIRROR_KEY = "app_language"

        fun peekLanguageTag(context: Context): String? {
            val value = context.getSharedPreferences(LOCALE_MIRROR_PREFS, Context.MODE_PRIVATE)
                .getString(LOCALE_MIRROR_KEY, "system")
            return value?.takeUnless { it == "system" || it.isBlank() }
        }
    }
    private object Keys {
        val TIME_FORMAT = stringPreferencesKey("time_format")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val FIRST_DAY_OF_THE_WEEK = stringPreferencesKey("first_day_of_the_week")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val SHOW_TOP_TIME = booleanPreferencesKey("show_top_time")
        val UI_ANIMATIONS_ENABLED = booleanPreferencesKey("ui_animations_enabled")
        val CONDITIONAL_UI_ENABLED = booleanPreferencesKey("conditional_ui_enabled")
        val LIQUID_GLASS_ENABLED = booleanPreferencesKey("liquid_glass_enabled")
        val FROSTED_GLASS_ENABLED = booleanPreferencesKey("frosted_glass_enabled")
        val GLOBAL_GLASS_MATERIAL_ENABLED = booleanPreferencesKey("global_glass_material_enabled")
        val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        val LIQUID_GLASS_EFFECT = stringPreferencesKey("liquid_glass_effect")
        val GLASS_CHROMATIC_ABERRATION = booleanPreferencesKey("glass_chromatic_aberration")
        val GLASS_LENS_DISTORTION = floatPreferencesKey("glass_lens_distortion")
        val GLASS_BLUR_ENABLED = booleanPreferencesKey("glass_blur_enabled")
        val GLASS_BLUR_RADIUS = floatPreferencesKey("glass_blur_radius")
        val BACKGROUND_BRIGHTNESS = floatPreferencesKey("background_brightness")
        val TIMETABLE_BACKGROUND_MODE = stringPreferencesKey("timetable_background_mode")
        val TIMETABLE_BACKGROUND_IMAGE_PATH = stringPreferencesKey("timetable_background_image_path")
    }

    val appConfigFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        val langSetting = prefs[Keys.APP_LANGUAGE].let { tag -> if (tag == "system") null else tag }
        val formatSetting = runCatching {
            TimeFormat.valueOf(prefs[Keys.TIME_FORMAT] ?: TimeFormat.SYSTEM.name)
        }.getOrDefault(TimeFormat.SYSTEM)
        val firstDaySetting = runCatching {
            FirstDayOfTheWeek.valueOf(
                prefs[Keys.FIRST_DAY_OF_THE_WEEK] ?: FirstDayOfTheWeek.SYSTEM.name
            )
        }.getOrDefault(FirstDayOfTheWeek.SYSTEM)

        val effectiveFirstDay: DayOfWeek = if (firstDaySetting == FirstDayOfTheWeek.SYSTEM) {
            val calendar = java.util.Calendar.getInstance(java.util.Locale.getDefault())
            when (calendar.firstDayOfWeek) {
                java.util.Calendar.MONDAY -> DayOfWeek.MONDAY
                java.util.Calendar.SUNDAY -> DayOfWeek.SUNDAY
                java.util.Calendar.SATURDAY -> DayOfWeek.SATURDAY
                else -> DayOfWeek.MONDAY
            }
        } else {
            firstDaySetting.dayOfWeek ?: DayOfWeek.MONDAY
        }

        val finalIs24Hour = when (formatSetting) {
            TimeFormat.H12 -> false
            TimeFormat.H24 -> true
            TimeFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        }

        AppConfig(
            languageTag = langSetting,
            is24HourFormat = finalIs24Hour,
            timeFormatSetting = formatSetting,
            firstDayOfTheWeekSetting = firstDaySetting,
            effectiveFirstDayOfTheWeek = effectiveFirstDay,
            isDynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: true,
            isShowTopTime = prefs[Keys.SHOW_TOP_TIME] ?: false,
            uiAnimationsEnabled = prefs[Keys.UI_ANIMATIONS_ENABLED] ?: true,
            conditionalUiEnabled = prefs[Keys.CONDITIONAL_UI_ENABLED] ?: true,
            isLiquidGlassEnabled = prefs[Keys.LIQUID_GLASS_ENABLED] ?: false,
            isFrostedGlassEnabled = prefs[Keys.FROSTED_GLASS_ENABLED] ?: false,
            isGlobalGlassMaterialEnabled = prefs[Keys.GLOBAL_GLASS_MATERIAL_ENABLED] ?: false,
            glassOpacity = (prefs[Keys.GLASS_OPACITY] ?: 0.42f).coerceIn(0.05f, 0.95f),
            liquidGlassEffect = runCatching { LiquidGlassEffect.valueOf(prefs[Keys.LIQUID_GLASS_EFFECT] ?: LiquidGlassEffect.BALANCED.name) }.getOrDefault(LiquidGlassEffect.BALANCED),
            glassChromaticAberration = prefs[Keys.GLASS_CHROMATIC_ABERRATION] ?: false,
            glassLensDistortion = (prefs[Keys.GLASS_LENS_DISTORTION] ?: 0.20f).coerceIn(0f, 1f),
            glassBlurEnabled = prefs[Keys.GLASS_BLUR_ENABLED] ?: true,
            glassBlurRadius = (prefs[Keys.GLASS_BLUR_RADIUS] ?: 1f).coerceIn(0f, 8f),
            backgroundBrightness = (prefs[Keys.BACKGROUND_BRIGHTNESS] ?: 0.82f).coerceIn(0.10f, 1f),
            timetableBackgroundMode = runCatching {
                TimetableBackgroundMode.valueOf(prefs[Keys.TIMETABLE_BACKGROUND_MODE] ?: TimetableBackgroundMode.THEME.name)
            }.getOrDefault(TimetableBackgroundMode.THEME),
            timetableBackgroundImagePath = prefs[Keys.TIMETABLE_BACKGROUND_IMAGE_PATH]
        )
    }

    suspend fun setTimeFormat(format: TimeFormat) {
        context.dataStore.edit { it[Keys.TIME_FORMAT] = format.name }
    }

    suspend fun setLanguage(languageTag: String?) {
        val persisted = languageTag ?: "system"
        // Mirror the locale in SharedPreferences so Activity.attachBaseContext can read it
        // synchronously without blocking the main thread on DataStore I/O during cold start.
        context.getSharedPreferences(LOCALE_MIRROR_PREFS, Context.MODE_PRIVATE)
            .edit().putString(LOCALE_MIRROR_KEY, persisted).apply()
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = persisted }
    }

    suspend fun setFirstDayOfTheWeek(firstDay: FirstDayOfTheWeek) {
        context.dataStore.edit { it[Keys.FIRST_DAY_OF_THE_WEEK] = firstDay.name }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    suspend fun setShowTopTime(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TOP_TIME] = enabled }
    }

    suspend fun setUiAnimationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.UI_ANIMATIONS_ENABLED] = enabled }
    }

    suspend fun setConditionalUiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CONDITIONAL_UI_ENABLED] = enabled }
    }

    suspend fun setLiquidGlassEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            // Keep the visible liquid-glass switch authoritative; legacy global/frosted flags
            // are migration-only and must not remain active behind it.
            prefs[Keys.LIQUID_GLASS_ENABLED] = enabled
            prefs[Keys.FROSTED_GLASS_ENABLED] = false
            prefs[Keys.GLOBAL_GLASS_MATERIAL_ENABLED] = false
        }
    }

    suspend fun setFrostedGlassEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FROSTED_GLASS_ENABLED] = enabled }
    }

    suspend fun setGlobalGlassMaterialEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GLOBAL_GLASS_MATERIAL_ENABLED] = enabled }
    }

    suspend fun setGlassOpacity(value: Float) {
        context.dataStore.edit { it[Keys.GLASS_OPACITY] = value.coerceIn(0.05f, 0.95f) }
    }

    suspend fun setLiquidGlassEffect(value: LiquidGlassEffect) {
        context.dataStore.edit { it[Keys.LIQUID_GLASS_EFFECT] = value.name }
    }

    suspend fun setGlassChromaticAberration(enabled: Boolean) { context.dataStore.edit { it[Keys.GLASS_CHROMATIC_ABERRATION] = enabled } }
    suspend fun setGlassLensDistortion(value: Float) { context.dataStore.edit { it[Keys.GLASS_LENS_DISTORTION] = value.coerceIn(0f, 1f) } }
    suspend fun setGlassBlurEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.GLASS_BLUR_ENABLED] = enabled } }
    suspend fun setGlassBlurRadius(value: Float) { context.dataStore.edit { it[Keys.GLASS_BLUR_RADIUS] = value.coerceIn(0f, 8f) } }

    suspend fun setBackgroundBrightness(value: Float) {
        context.dataStore.edit { it[Keys.BACKGROUND_BRIGHTNESS] = value.coerceIn(0.10f, 1f) }
    }

    suspend fun setTimetableBackground(mode: TimetableBackgroundMode, imagePath: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TIMETABLE_BACKGROUND_MODE] = mode.name
            if (imagePath.isNullOrBlank()) prefs.remove(Keys.TIMETABLE_BACKGROUND_IMAGE_PATH)
            else prefs[Keys.TIMETABLE_BACKGROUND_IMAGE_PATH] = imagePath
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PreferenceStorageEntryPoint {
    fun preferenceStorage(): PreferenceStorage
}
