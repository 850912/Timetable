package com.hufeng943.timetable.presentation.viewmodel

import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.app.LocaleManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.data.FirstDayOfTheWeek
import com.hufeng943.timetable.data.PreferenceStorage
import com.hufeng943.timetable.data.TimeFormat
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.common.TimetableBackgroundMode
import com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect
import com.hufeng943.timetable.presentation.ui.common.AppPowerSaveMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class AppConfigViewModel @Inject constructor(
    private val preferenceStorage: PreferenceStorage,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val appConfig: StateFlow<AppConfig> = preferenceStorage.appConfigFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppConfig()
    )

    private val _localeRecreateEvent = MutableSharedFlow<Unit>()
    val localeRecreateEvent: SharedFlow<Unit> = _localeRecreateEvent.asSharedFlow()

    fun updateLanguage(languageTag: String?) {
        if (appConfig.value.languageTag == languageTag) return
        viewModelScope.launch {
            // Keep the persistent preference as the single source of truth.
            // Android 13+ applies app locales through the framework and performs the
            // required configuration/lifecycle transition itself, so do not call recreate().
            preferenceStorage.setLanguage(languageTag)
            if (Build.VERSION.SDK_INT >= 33) {
                val localeManager = appContext.getSystemService(LocaleManager::class.java)
                localeManager.applicationLocales = if (languageTag.isNullOrBlank()) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(languageTag)
                }
            } else {
                // API 32 and below still use the synchronous mirror + attachBaseContext path.
                _localeRecreateEvent.emit(Unit)
            }
        }
    }

    fun updateFormat(timeFormat: TimeFormat) {
        viewModelScope.launch { preferenceStorage.setTimeFormat(timeFormat) }
    }

    fun updateFirstDayOfTheWeek(firstDay: FirstDayOfTheWeek) {
        viewModelScope.launch { preferenceStorage.setFirstDayOfTheWeek(firstDay) }
    }

    fun updateDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { preferenceStorage.setDynamicColorEnabled(enabled) }
    }

    fun updateShowTopTime(enabled: Boolean) {
        viewModelScope.launch { preferenceStorage.setShowTopTime(enabled) }
    }

    fun updatePowerSaveMode(mode: AppPowerSaveMode) {
        viewModelScope.launch { preferenceStorage.setPowerSaveMode(mode) }
    }

    fun updateUiAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferenceStorage.setUiAnimationsEnabled(enabled) }
    }

    fun updateLiquidGlassEnabled(enabled: Boolean) {
        viewModelScope.launch { preferenceStorage.setLiquidGlassEnabled(enabled) }
    }

    fun updateGlassOpacity(value: Float) { viewModelScope.launch { preferenceStorage.setGlassOpacity(value) } }
    fun updateGlassClarity(value: Float) { viewModelScope.launch { preferenceStorage.setGlassClarity(value) } }
    fun updateLiquidGlassEffect(value: LiquidGlassEffect) { viewModelScope.launch { preferenceStorage.setLiquidGlassEffect(value) } }
    fun updateGlassChromaticAberration(enabled: Boolean) { viewModelScope.launch { preferenceStorage.setGlassChromaticAberration(enabled) } }
    fun updateGlassLensDistortion(value: Float) { viewModelScope.launch { preferenceStorage.setGlassLensDistortion(value) } }
    fun updateGlassBlurEnabled(enabled: Boolean) { viewModelScope.launch { preferenceStorage.setGlassBlurEnabled(enabled) } }
    fun updateGlassBlurRadius(value: Float) { viewModelScope.launch { preferenceStorage.setGlassBlurRadius(value) } }
    fun updateBackgroundBrightness(value: Float) { viewModelScope.launch { preferenceStorage.setBackgroundBrightness(value) } }

    fun updateTimetableBackground(mode: TimetableBackgroundMode, imagePath: String? = null) {
        viewModelScope.launch { preferenceStorage.setTimetableBackground(mode, imagePath) }
    }
}
