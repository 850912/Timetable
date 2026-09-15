package com.hufeng943.timetable.presentation.ui.screens.more.about

import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

/** Small diagnostics page intended for release verification, not feature discovery. */
@Composable
fun DeveloperOptionsScreen() {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val runtimeSummary = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        "API ${Build.VERSION.SDK_INT} · ${configuration.screenWidthDp}×${configuration.screenHeightDp} dp"
    }
    val versionSummary = remember(context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName ?: "?"} · code ${info.longVersionCode}"
        }.onFailure { Log.e("DeveloperOptions", "Unable to read version", it) }
            .getOrDefault("?")
    }

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(state = scrollState,
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(scrollState),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollState), contentPadding = contentPadding) {
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
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_runtime_title),
                    subtitle = runtimeSummary,
                    icon = Icons.Rounded.PhoneAndroid,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_database_title),
                    subtitle = stringResource(R.string.developer_database_subtitle),
                    icon = Icons.Rounded.DataObject,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_core_title),
                    subtitle = stringResource(R.string.developer_core_subtitle),
                    icon = Icons.Rounded.Memory,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.developer_transfer_title),
                    subtitle = "/timetable/file-transfer/v1 · phone ↔ watch",
                    icon = Icons.Rounded.CloudSync,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
