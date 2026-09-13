package com.hufeng943.timetable.presentation.ui.screens.more.about

import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.DataObject
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.common.LocalNavController

@Composable
fun DeveloperOptionsScreen() {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val navController = LocalNavController.current
    val runtimeSummary = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        "API ${Build.VERSION.SDK_INT} · ${configuration.screenWidthDp}×${configuration.screenHeightDp} dp"
    }

    val versionSummary = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val name = packageInfo.versionName ?: "?"
            "$name · code ${packageInfo.longVersionCode}"
        } catch (e: Exception) {
            Log.e("DeveloperOptions", "Unable to read version", e)
            "?"
        }
    }

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                ) { Text(stringResource(R.string.developer_options_title)) }
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_version_title),
                    subtitle = versionSummary,
                    icon = Icons.Rounded.Build,
                    emphasize = true,
                    titleMaxLines = Int.MAX_VALUE,
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_runtime_title),
                    subtitle = runtimeSummary,
                    icon = Icons.Rounded.PhoneAndroid,
                    titleMaxLines = Int.MAX_VALUE,
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_database_title),
                    subtitle = stringResource(R.string.developer_database_subtitle),
                    icon = Icons.Rounded.DataObject,
                    titleMaxLines = Int.MAX_VALUE,
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = "中国区 Wear 通信探针",
                    subtitle = "检查 GMS · Node · Message · Data",
                    icon = Icons.Rounded.BugReport,
                    emphasize = true,
                    onClick = { navController.navigate(NavRoutes.MORE_ABOUT_DEVELOPER_PROBE) },
                    titleMaxLines = Int.MAX_VALUE,
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_transfer_title),
                    subtitle = "/timetable/file-transfer/v1",
                    icon = Icons.Rounded.CloudSync,
                    titleMaxLines = Int.MAX_VALUE,
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_formats_title),
                    subtitle = "ICS · CSV · JSON",
                    icon = Icons.Rounded.Storage,
                    titleMaxLines = Int.MAX_VALUE,
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_core_title),
                    subtitle = stringResource(R.string.developer_core_subtitle),
                    icon = Icons.Rounded.Memory,
                    titleMaxLines = Int.MAX_VALUE,
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_tile_title),
                    subtitle = stringResource(R.string.developer_tile_subtitle),
                    icon = Icons.Rounded.GridView,
                    titleMaxLines = Int.MAX_VALUE,
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_complication_title),
                    subtitle = stringResource(R.string.developer_complication_subtitle),
                    icon = Icons.Rounded.Schedule,
                    titleMaxLines = Int.MAX_VALUE,
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_home_title),
                    subtitle = stringResource(R.string.developer_home_subtitle),
                    icon = Icons.Rounded.Schedule,
                    titleMaxLines = Int.MAX_VALUE,
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_effects_title),
                    subtitle = stringResource(R.string.developer_effects_subtitle),
                    icon = Icons.Rounded.AutoAwesome,
                    emphasize = true,
                    titleMaxLines = Int.MAX_VALUE,
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
