package com.hufeng943.timetable.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.data.FirstDayOfTheWeek
import com.hufeng943.timetable.data.PreferenceStorage
import com.hufeng943.timetable.data.TimeFormat
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppConfigViewModel @Inject constructor(
    private val preferenceStorage: PreferenceStorage
) : ViewModel() {

    val appConfig: StateFlow<AppConfig> = preferenceStorage.appConfigFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppConfig()
    )

    private val _localeRecreateEvent = MutableSharedFlow<Unit>()
    val localeRecreateEvent: SharedFlow<Unit> = _localeRecreateEvent.asSharedFlow()

    fun updateLanguage(languageTag: String?) {
        viewModelScope.launch {
            preferenceStorage.setLanguage(languageTag)
            _localeRecreateEvent.emit(Unit)
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
}
