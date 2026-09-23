package com.hufeng943.timetable.presentation

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hufeng943.timetable.data.PreferenceStorage
import com.hufeng943.timetable.presentation.ui.theme.TimetableTheme
import androidx.compose.runtime.getValue
import com.hufeng943.timetable.presentation.ui.AppNavHost
import com.hufeng943.timetable.presentation.viewmodel.AppConfigViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale


import androidx.lifecycle.compose.collectAsStateWithLifecycle
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appConfigViewModel: AppConfigViewModel by viewModels()

    @javax.inject.Inject
    lateinit var themePreference: com.hufeng943.timetable.data.ThemePreference

    private val requestChinaBlePermissions = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            com.hufeng943.timetable.sync.ChinaWearBleService.start(this)
        }
    }

    private fun startChinaBleTransportIfNeeded() {
        val gmsAvailable = runCatching {
            com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this) == 0
        }.getOrDefault(false)
        if (gmsAvailable) return
        val permissions = when {
            android.os.Build.VERSION.SDK_INT >= 31 -> listOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
            )
            android.os.Build.VERSION.SDK_INT >= 23 -> listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            else -> emptyList()
        }
        val missing = permissions.filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            requestChinaBlePermissions.launch(missing.toTypedArray())
            return
        }
        runCatching { com.hufeng943.timetable.sync.ChinaWearBleService.start(this) }
    }

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
        startChinaBleTransportIfNeeded()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appConfigViewModel.localeRecreateEvent.collect {
                    recreate()
                }
            }
        }

        setContent {
            val currentThemePreset by themePreference.themePresetFlow.collectAsStateWithLifecycle(initialValue = com.hufeng943.timetable.presentation.ui.theme.ThemePreset.AMOLED_BLACK)
            val config by appConfigViewModel.appConfig.collectAsStateWithLifecycle()
            TimetableTheme(themePreset = currentThemePreset, dynamicColorEnabled = config.isDynamicColorEnabled) {
                AppNavHost()
            }
        }
    }
}
