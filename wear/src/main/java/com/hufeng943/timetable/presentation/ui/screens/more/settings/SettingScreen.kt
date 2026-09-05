package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.hufeng943.timetable.data.ThemePreference
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.screens.more.settings.export.ExportScreen
import com.hufeng943.timetable.presentation.ui.screens.more.settings.importer.ImportScreen
import com.hufeng943.timetable.presentation.ui.theme.ThemePreset
import com.hufeng943.timetable.presentation.viewmodel.AppConfigViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingScreen(
    appConfigViewModel: AppConfigViewModel = hiltViewModel(LocalContext.current as ViewModelStoreOwner),
    themePreference: ThemePreference = hiltViewModel<ThemePrefViewModel>().themePreference
) {
    val internalNavController = rememberSwipeDismissableNavController()
    val config = LocalAppConfig.current
    val scope = rememberCoroutineScope()
    val currentPreset by themePreference.themePresetFlow.collectAsState(initial = ThemePreset.AMOLED_BLACK)

    SwipeDismissableNavHost(
        navController = internalNavController,
        startDestination = InternalNavRoutes.MAIN
    ) {
        composable(InternalNavRoutes.MAIN) {
            SettingPager(
                config = config,
                currentThemePreset = currentPreset,
                onLanguageSelectClick = { internalNavController.navigateSingle(InternalNavRoutes.LANGUAGE_SELECT) },
                onTimeFormatSelectClick = { internalNavController.navigateSingle(InternalNavRoutes.TIME_FORMAT_SELECT) },
                onFirstDaySelectClick = { internalNavController.navigateSingle(InternalNavRoutes.FIRST_DAY_SELECT) },
                onThemeSelectClick = { internalNavController.navigateSingle(InternalNavRoutes.THEME_SELECT) },
                onExportClick = { internalNavController.navigateSingle(InternalNavRoutes.EXPORT) },
                onImportClick = { internalNavController.navigateSingle(InternalNavRoutes.IMPORT) },
                onShowTopTimeToggle = { enabled ->
                    appConfigViewModel.updateShowTopTime(enabled)
                }
            )
        }

        composable(InternalNavRoutes.EXPORT) {
            ExportScreen(onNavigateBack = { internalNavController.popBackStack() })
        }

        composable(InternalNavRoutes.IMPORT) {
            ImportScreen(onNavigateBack = { internalNavController.popBackStack() })
        }

        composable(InternalNavRoutes.THEME_SELECT) {
            ThemePresetSelectPager(
                currentPreset = currentPreset,
                onPresetSelect = { newPreset ->
                    scope.launch {
                        themePreference.setThemePreset(newPreset)
                        internalNavController.popBackStack()
                    }
                }
            )
        }

        composable(InternalNavRoutes.LANGUAGE_SELECT) {
            LanguageSelectPager(config = config, onLanguageSelect = { appConfigViewModel.updateLanguage(it) })
        }

        composable(InternalNavRoutes.TIME_FORMAT_SELECT) {
            TimeFormatSelectPager(config = config, onTimeFormatSelect = { appConfigViewModel.updateFormat(it) })
        }

        composable(InternalNavRoutes.FIRST_DAY_SELECT) {
            FirstDaySelectPager(config) { appConfigViewModel.updateFirstDayOfTheWeek(it) }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ThemePrefViewModel @javax.inject.Inject constructor(
    val themePreference: ThemePreference
) : androidx.lifecycle.ViewModel()
