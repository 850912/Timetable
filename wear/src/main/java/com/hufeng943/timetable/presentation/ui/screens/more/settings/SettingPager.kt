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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    val currentLanguageLabel = remember(config.languageTag) {
        val values = context.resources.getStringArray(R.array.language_values).toList()
        val labels = context.resources.getStringArray(R.array.language_labels).toList()
        val index = values.indexOfFirst { raw -> (if (raw == "@null") null else raw) == config.languageTag }
        if (index >= 0) labels[index] else labels[0]
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
            item { SettingItemCard(Icons.Rounded.Palette, "UI 管理", "主题、背景、玻璃与动效", transformationSpec, onUiManagementClick) }
            item { SettingItemCard(Icons.Rounded.Share, "导出课表", "ICS / CSV / JSON 备份", transformationSpec, onExportClick, true) }
            item { SettingItemCard(Icons.Rounded.FileUpload, "导入课表", "扫描本机备份一键恢复", transformationSpec, onImportClick, true) }
            item { SettingItemCard(Icons.Rounded.Language, stringResource(R.string.settings_language), currentLanguageLabel, transformationSpec, onLanguageSelectClick) }
            item { SettingItemCard(Icons.Rounded.AccessTime, stringResource(R.string.settings_time_format), currentTimeFormatLabel, transformationSpec, onTimeFormatSelectClick) }
            item { SettingItemCard(Icons.Rounded.BatterySaver, "省电模式", when (config.powerSaveMode) { com.hufeng943.timetable.presentation.ui.common.AppPowerSaveMode.FOLLOW_SYSTEM -> "跟随系统"; com.hufeng943.timetable.presentation.ui.common.AppPowerSaveMode.ALWAYS_ON -> "始终开启"; com.hufeng943.timetable.presentation.ui.common.AppPowerSaveMode.ALWAYS_OFF -> "始终关闭" }, transformationSpec, onPowerSaveSelectClick) }
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
