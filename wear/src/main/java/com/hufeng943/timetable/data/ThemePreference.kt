package com.hufeng943.timetable.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hufeng943.timetable.presentation.ui.theme.ThemePreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "timetable_theme_pref")

@Singleton
class ThemePreference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_KEY = stringPreferencesKey("app_theme_preset")

    val themePresetFlow: Flow<ThemePreset> = context.themeDataStore.data.map { prefs ->
        val name = prefs[THEME_KEY] ?: ThemePreset.AMOLED_BLACK.name
        runCatching { ThemePreset.valueOf(name) }.getOrDefault(ThemePreset.AMOLED_BLACK)
    }

    suspend fun setThemePreset(preset: ThemePreset) {
        context.themeDataStore.edit { it[THEME_KEY] = preset.name }
    }
}
