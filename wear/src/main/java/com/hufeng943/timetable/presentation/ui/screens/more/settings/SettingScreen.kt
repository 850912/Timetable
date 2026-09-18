package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hufeng943.timetable.data.ThemePreference
import com.hufeng943.timetable.presentation.ui.components.WearInternalNavHost
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.screens.more.settings.export.ExportScreen
import com.hufeng943.timetable.presentation.ui.screens.more.settings.importer.ImportScreen
import com.hufeng943.timetable.presentation.ui.theme.ThemePreset
import com.hufeng943.timetable.presentation.viewmodel.AppConfigViewModel
import kotlinx.coroutines.launch

import androidx.lifecycle.compose.collectAsStateWithLifecycle
@Composable
fun SettingScreen(
    appConfigViewModel: AppConfigViewModel = hiltViewModel(LocalContext.current as ViewModelStoreOwner),
    themePreference: ThemePreference = hiltViewModel<ThemePrefViewModel>().themePreference
) {
    val internalNavController = rememberNavController()
    val config = LocalAppConfig.current
    val scope = rememberCoroutineScope()
    val currentPreset by themePreference.themePresetFlow.collectAsStateWithLifecycle(initialValue = ThemePreset.AMOLED_BLACK)

    WearInternalNavHost(
        navController = internalNavController,
        startDestination = InternalNavRoutes.MAIN) {
        composable(InternalNavRoutes.MAIN) {
            SettingPager(
                config = config,
                onUiManagementClick = { internalNavController.navigateSingle(InternalNavRoutes.UI_MANAGEMENT) },
                onLanguageSelectClick = { internalNavController.navigateSingle(InternalNavRoutes.LANGUAGE_SELECT) },
                onTimeFormatSelectClick = { internalNavController.navigateSingle(InternalNavRoutes.TIME_FORMAT_SELECT) },
                onFirstDaySelectClick = { internalNavController.navigateSingle(InternalNavRoutes.FIRST_DAY_SELECT) },
                onPowerSaveSelectClick = { internalNavController.navigateSingle(InternalNavRoutes.POWER_SAVE_SELECT) },
                onExportClick = { internalNavController.navigateSingle(InternalNavRoutes.EXPORT) },
                onImportClick = { internalNavController.navigateSingle(InternalNavRoutes.IMPORT) },
            )
        }


        composable(InternalNavRoutes.UI_MANAGEMENT) {
            UiManagementPager(
                config = config,
                currentThemePreset = currentPreset,
                onThemeSelectClick = { internalNavController.navigateSingle(InternalNavRoutes.THEME_SELECT) },
                onBackgroundSelectClick = { internalNavController.navigateSingle(InternalNavRoutes.BACKGROUND_SELECT) },
                onLiquidGlassAdvancedClick = { internalNavController.navigateSingle(InternalNavRoutes.LIQUID_GLASS_ADVANCED) },
                onDynamicColorToggle = appConfigViewModel::updateDynamicColorEnabled,
                onShowTopTimeToggle = appConfigViewModel::updateShowTopTime,
                onUiAnimationsToggle = appConfigViewModel::updateUiAnimationsEnabled,
            )
        }

        composable(InternalNavRoutes.EXPORT) {
            ExportScreen(onNavigateBack = { internalNavController.popBackStack() })
        }

        composable(InternalNavRoutes.IMPORT) {
            ImportScreen(onNavigateBack = { internalNavController.popBackStack() })
        }


        composable(InternalNavRoutes.POWER_SAVE_SELECT) {
            PowerSaveModeSelectPager(config = config) { mode ->
                appConfigViewModel.updatePowerSaveMode(mode)
                internalNavController.popBackStack()
            }
        }

        composable(InternalNavRoutes.LIQUID_GLASS_ADVANCED) {
            LiquidGlassAdvancedPager(
                config = config,
                onEnabledChange = appConfigViewModel::updateLiquidGlassEnabled,
                onOpacityChange = appConfigViewModel::updateGlassOpacity,
                onClarityChange = appConfigViewModel::updateGlassClarity,
                onEffectChange = appConfigViewModel::updateLiquidGlassEffect,
                onBrightnessChange = appConfigViewModel::updateBackgroundBrightness,
                onChromaticAberrationChange = appConfigViewModel::updateGlassChromaticAberration,
                onLensDistortionChange = appConfigViewModel::updateGlassLensDistortion,
                onBlurEnabledChange = appConfigViewModel::updateGlassBlurEnabled,
                onBlurRadiusChange = appConfigViewModel::updateGlassBlurRadius,
            )
        }

        composable(InternalNavRoutes.BACKGROUND_SELECT) {
            BackgroundSelectPager(
                config = config,
                onBackgroundSelected = { mode, path ->
                    appConfigViewModel.updateTimetableBackground(mode, path)
                    internalNavController.popBackStack()
                }
            )
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
            LanguageSelectPager(
                config = config,
                onLanguageSelect = {
                    appConfigViewModel.updateLanguage(it)
                    internalNavController.popBackStack()
                }
            )
        }

        composable(InternalNavRoutes.TIME_FORMAT_SELECT) {
            TimeFormatSelectPager(
                config = config,
                onTimeFormatSelect = {
                    appConfigViewModel.updateFormat(it)
                    internalNavController.popBackStack()
                }
            )
        }

        composable(InternalNavRoutes.FIRST_DAY_SELECT) {
            FirstDaySelectPager(
                config = config,
                onFirstDaySelect = {
                    appConfigViewModel.updateFirstDayOfTheWeek(it)
                    internalNavController.popBackStack()
                }
            )
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ThemePrefViewModel @javax.inject.Inject constructor(
    val themePreference: ThemePreference
) : androidx.lifecycle.ViewModel()
