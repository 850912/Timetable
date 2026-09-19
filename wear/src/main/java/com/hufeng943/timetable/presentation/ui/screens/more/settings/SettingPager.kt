package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.hufeng943.timetable.R
import com.hufeng943.timetable.data.FirstDayOfTheWeek
import com.hufeng943.timetable.data.TimeFormat
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleButton
import kotlinx.coroutines.launch

@Composable
fun SettingPager(
    config: AppConfig,
    onUiManagementClick: () -> Unit,
    onLanguageSelectClick: () -> Unit,
    onTimeFormatSelectClick: () -> Unit,
    onFirstDaySelectClick: () -> Unit,
    onPowerSaveSelectClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val scope = rememberCoroutineScope()
    val currentLanguageLabel = when (config.languageTag) {
        "zh-CN" -> stringResource(R.string.language_simplified_chinese)
        "en" -> stringResource(R.string.language_english)
        else -> stringResource(R.string.language_follow_system)
    }
    val currentTimeFormatLabel = when (config.timeFormatSetting) {
        TimeFormat.SYSTEM -> stringResource(R.string.settings_time_format_system)
        TimeFormat.H12 -> stringResource(R.string.settings_time_format_12h)
        TimeFormat.H24 -> stringResource(R.string.settings_time_format_24h)
    }
    val currentFirstDayLabel = when (config.firstDayOfTheWeekSetting) {
        FirstDayOfTheWeek.SYSTEM -> stringResource(R.string.settings_first_day_system)
        FirstDayOfTheWeek.MONDAY -> stringResource(R.string.settings_first_day_monday)
        FirstDayOfTheWeek.SUNDAY -> stringResource(R.string.settings_first_day_sunday)
        FirstDayOfTheWeek.SATURDAY -> stringResource(R.string.settings_first_day_saturday)
    }

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(onClick = { scope.launch { scrollState.animateScrollToItem(0) } }) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(state = scrollState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text(stringResource(R.string.more_menu_settings)) }
            }
            item { SettingItemCard(Icons.Rounded.Palette, stringResource(R.string.settings_ui_management), stringResource(R.string.settings_ui_management_summary), transformationSpec, onUiManagementClick) }
            item { SettingItemCard(Icons.Rounded.Share, stringResource(R.string.settings_export), stringResource(R.string.settings_export_summary), transformationSpec, onExportClick, true) }
            item { SettingItemCard(Icons.Rounded.FileUpload, stringResource(R.string.settings_import), stringResource(R.string.settings_import_summary), transformationSpec, onImportClick, true) }
            item { SettingItemCard(Icons.Rounded.Language, stringResource(R.string.settings_language), currentLanguageLabel, transformationSpec, onLanguageSelectClick) }
            item { SettingItemCard(Icons.Rounded.AccessTime, stringResource(R.string.settings_time_format), currentTimeFormatLabel, transformationSpec, onTimeFormatSelectClick) }
            item { SettingItemCard(Icons.Rounded.BatterySaver, stringResource(R.string.settings_power_save), when (config.powerSaveMode) { com.hufeng943.timetable.presentation.ui.common.AppPowerSaveMode.FOLLOW_SYSTEM -> stringResource(R.string.settings_power_follow); com.hufeng943.timetable.presentation.ui.common.AppPowerSaveMode.ALWAYS_ON -> stringResource(R.string.settings_power_on); com.hufeng943.timetable.presentation.ui.common.AppPowerSaveMode.ALWAYS_OFF -> stringResource(R.string.settings_power_off) }, transformationSpec, onPowerSaveSelectClick) }
            item { SettingItemCard(Icons.Rounded.DateRange, stringResource(R.string.settings_first_day), currentFirstDayLabel, transformationSpec, onFirstDaySelectClick) }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.SettingItemCard(
    icon: ImageVector,
    title: String,
    value: String,
    transformationSpec: TransformationSpec,
    onClick: () -> Unit,
    emphasize: Boolean = false,
) {
    OneUiCapsuleButton(
        icon = icon,
        label = title,
        secondaryLabel = value,
        emphasize = emphasize,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
    )
}
