package com.hufeng943.timetable.presentation

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hufeng943.timetable.data.PreferenceStorage
import com.hufeng943.timetable.presentation.ui.theme.TimetableTheme
import androidx.compose.runtime.getValue
import com.hufeng943.timetable.presentation.ui.AppNavHost
import com.hufeng943.timetable.presentation.viewmodel.AppConfigViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appConfigViewModel: AppConfigViewModel by viewModels()

    @javax.inject.Inject
    lateinit var themePreference: com.hufeng943.timetable.data.ThemePreference

    override fun attachBaseContext(newBase: Context) {
        // Never block Activity cold start on DataStore. The locale is mirrored by
        // PreferenceStorage.setLanguage() into a tiny synchronous preference.
        val languageTag = PreferenceStorage.peekLanguageTag(newBase)
        val context = if (languageTag != null) {
            val locale = Locale.forLanguageTag(languageTag)
            val configuration = Configuration(newBase.resources.configuration)
            Locale.setDefault(locale)
            configuration.setLocale(locale)
            newBase.createConfigurationContext(configuration)
        } else newBase
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val currentThemePreset by themePreference.themePresetFlow.collectAsStateWithLifecycle(initialValue = com.hufeng943.timetable.presentation.ui.theme.ThemePreset.AMOLED_BLACK)
            val config by appConfigViewModel.appConfig.collectAsStateWithLifecycle()
            val baseContext = LocalContext.current
            val effectiveLocale = remember(config.languageTag) {
                config.languageTag?.let { Locale.forLanguageTag(it) }
                    ?: Resources.getSystem().configuration.locales[0]
            }
            val localizedContext = remember(baseContext, effectiveLocale) {
                val localizedConfiguration = Configuration(baseContext.resources.configuration).apply {
                    setLocale(effectiveLocale)
                }
                baseContext.createConfigurationContext(localizedConfiguration)
            }
            SideEffect { Locale.setDefault(effectiveLocale) }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
            ) {
                TimetableTheme(themePreset = currentThemePreset, dynamicColorEnabled = config.isDynamicColorEnabled) {
                    AppNavHost()
                }
            }
        }
    }
}
