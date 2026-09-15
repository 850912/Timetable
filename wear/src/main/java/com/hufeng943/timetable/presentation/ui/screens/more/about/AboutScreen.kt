package com.hufeng943.timetable.presentation.ui.screens.more.about

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.OneUiInfoCapsule

/** Compact About page: keep useful attribution/release information, remove build-era diagnostics. */
@Composable
fun AboutScreen() {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val navController = LocalNavController.current
    val context = LocalContext.current
    var versionTapCount by remember { mutableIntStateOf(0) }

    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.onFailure { Log.e("AboutScreen", "Unable to read version", it) }
            .getOrNull()
    } ?: stringResource(R.string.unknown)

    val icon = remember(context) {
        context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
    }

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(scrollState),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollState),
            contentPadding = contentPadding,
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                ) { Text(stringResource(R.string.more_menu_about)) }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(BitmapPainter(icon), contentDescription = null, modifier = Modifier.size(54.dp))
                    Spacer(Modifier.height(5.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = versionName,
                        modifier = Modifier
                            .clickable {
                                versionTapCount += 1
                                if (versionTapCount >= 7) {
                                    versionTapCount = 0
                                    navController.navigateSingle(NavRoutes.MORE_ABOUT_DEVELOPER)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item {
                OneUiInfoCapsule(
                    icon = Icons.Rounded.Info,
                    text = stringResource(R.string.about_description),
                    maxLines = Int.MAX_VALUE,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiInfoCapsule(
                    icon = Icons.Rounded.History,
                    text = stringResource(R.string.about_changelog_part1),
                    maxLines = Int.MAX_VALUE,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.about_license_title),
                    subtitle = stringResource(R.string.about_license_subtitle),
                    icon = Icons.Rounded.Description,
                    onClick = { navController.navigateSingle(NavRoutes.MORE_ABOUT_LIBRARIES) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.about_developer_name),
                    subtitle = stringResource(R.string.about_developer_subtitle),
                    icon = Icons.Rounded.Code,
                    onClick = { navController.navigateSingle(NavRoutes.MORE_ABOUT_DEVELOPER) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
