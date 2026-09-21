package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hufeng943.timetable.data.ThemePreference
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.screens.more.settings.export.ExportScreen
import com.hufeng943.timetable.presentation.ui.screens.more.settings.importer.ImportScreen
import com.hufeng943.timetable.presentation.ui.theme.ThemePreset
import com.hufeng943.timetable.presentation.viewmodel.AppConfigViewModel
import kotlinx.coroutines.launch

/** Settings landing page. All child pages are destinations in the app-level Wear NavHost. */
@Composable
fun SettingScreen() {
    val nav = LocalNavController.current
    val config = LocalAppConfig.current
    SettingPager(
        config = config,
        onUiManagementClick = { nav.navigateSingle(NavRoutes.MORE_SETTINGS_UI) },
        onLanguageSelectClick = { nav.navigateSingle(NavRoutes.MORE_SETTINGS_LANGUAGE) },
        onTimeFormatSelectClick = { nav.navigateSingle(NavRoutes.MORE_SETTINGS_TIME_FORMAT) },
        onFirstDaySelectClick = { nav.navigateSingle(NavRoutes.MORE_SETTINGS_FIRST_DAY) },
        onPowerSaveSelectClick = { nav.navigateSingle(NavRoutes.MORE_SETTINGS_POWER_SAVE) },
        onExportClick = { nav.navigateSingle(NavRoutes.MORE_SETTINGS_EXPORT) },
        onImportClick = { nav.navigateSingle(NavRoutes.MORE_SETTINGS_IMPORT) },
    )
}

@Composable
fun SettingsUiManagementScreen(
    appConfigViewModel: AppConfigViewModel,
    themePreference: ThemePreference = hiltViewModel<ThemePrefViewModel>().themePreference,
) {
    val nav = LocalNavController.current
    val config = LocalAppConfig.current
    val currentPreset by themePreference.themePresetFlow.collectAsStateWithLifecycle(
        initialValue = ThemePreset.AMOLED_BLACK,
    )
    UiManagementPager(
        config = config,
        currentThemePreset = currentPreset,
        onThemeSelectClick = { nav.navigateSingle(NavRoutes.MORE_SETTINGS_THEME) },
        onBackgroundSelectClick = { nav.navigateSingle(NavRoutes.MORE_SETTINGS_BACKGROUND) },
        onLiquidGlassAdvancedClick = { nav.navigateSingle(NavRoutes.MORE_SETTINGS_LIQUID_GLASS) },
        onDynamicColorToggle = appConfigViewModel::updateDynamicColorEnabled,
        onShowTopTimeToggle = appConfigViewModel::updateShowTopTime,
        onUiAnimationsToggle = appConfigViewModel::updateUiAnimationsEnabled,
    )
}

@Composable
fun SettingsLanguageScreen(appConfigViewModel: AppConfigViewModel) {
    LanguageSelectPager(
        config = LocalAppConfig.current,
        onLanguageSelect = appConfigViewModel::updateLanguage,
    )
}

@Composable
fun SettingsTimeFormatScreen(appConfigViewModel: AppConfigViewModel) {
    val nav = LocalNavController.current
    TimeFormatSelectPager(LocalAppConfig.current) {
        appConfigViewModel.updateFormat(it)
        nav.popSafe()
    }
}

@Composable
fun SettingsFirstDayScreen(appConfigViewModel: AppConfigViewModel) {
    val nav = LocalNavController.current
    FirstDaySelectPager(LocalAppConfig.current) {
        appConfigViewModel.updateFirstDayOfTheWeek(it)
        nav.popSafe()
    }
}

@Composable
fun SettingsPowerSaveScreen(appConfigViewModel: AppConfigViewModel) {
    val nav = LocalNavController.current
    PowerSaveModeSelectPager(LocalAppConfig.current) {
        appConfigViewModel.updatePowerSaveMode(it)
        nav.popSafe()
    }
}

@Composable
fun SettingsBackgroundScreen(appConfigViewModel: AppConfigViewModel) {
    val nav = LocalNavController.current
    BackgroundSelectPager(LocalAppConfig.current) { mode, path ->
        appConfigViewModel.updateTimetableBackground(mode, path)
        nav.popSafe()
    }
}

@Composable
fun SettingsThemeScreen(
    themePreference: ThemePreference = hiltViewModel<ThemePrefViewModel>().themePreference,
) {
    val nav = LocalNavController.current
    val scope = rememberCoroutineScope()
    val currentPreset by themePreference.themePresetFlow.collectAsStateWithLifecycle(
        initialValue = ThemePreset.AMOLED_BLACK,
    )
    ThemePresetSelectPager(currentPreset) { preset ->
        scope.launch {
            themePreference.setThemePreset(preset)
            nav.popSafe()
        }
    }
}

@Composable
fun SettingsExportScreen() {
    val nav = LocalNavController.current
    ExportScreen(onNavigateBack = { nav.popSafe() })
}

@Composable
fun SettingsImportScreen() {
    val nav = LocalNavController.current
    ImportScreen(onNavigateBack = { nav.popSafe() })
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ThemePrefViewModel @javax.inject.Inject constructor(
    val themePreference: ThemePreference,
) : androidx.lifecycle.ViewModel()
