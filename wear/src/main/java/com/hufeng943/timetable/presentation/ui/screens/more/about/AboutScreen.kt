package com.hufeng943.timetable.presentation.ui.screens.more.about

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Star
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.OneUiInfoCapsule
import kotlinx.coroutines.launch

@Composable
fun AboutScreen() {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var changelogExpanded by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableStateOf(0) }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            Log.e("AboutScreen", "版本号获取失败：$e")
            null
        }
    } ?: stringResource(R.string.unknown)

    val icon = remember {
        context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
    }

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(onClick = { scope.launch { scrollState.animateScrollToItem(0) } }) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.back_to_top))
            }
        }
    ) { contentPadding ->
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
                ) { Text(stringResource(R.string.more_menu_about)) }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(BitmapPainter(icon), contentDescription = null, modifier = Modifier.size(58.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(
                        text = versionName,
                        modifier = Modifier.clickable {
                            versionTapCount += 1
                            if (versionTapCount >= 7) {
                                versionTapCount = 0
                                navController.navigateSingle(NavRoutes.MORE_ABOUT_DEVELOPER)
                            }
                        }.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                OneUiInfoCapsule(
                    icon = Icons.Rounded.Info,
                    text = stringResource(R.string.about_description),
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiInfoCapsule(
                    icon = Icons.Rounded.Star,
                    text = stringResource(R.string.about_features_text).trimMargin(),
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.about_changelog_title),
                    subtitle = if (changelogExpanded) null else stringResource(R.string.about_changelog_expand_hint),
                    icon = Icons.Rounded.History,
                    emphasize = changelogExpanded,
                    onClick = { changelogExpanded = !changelogExpanded },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                AnimatedVisibility(
                    visible = changelogExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    OneUiInfoCapsule(
                        icon = Icons.Rounded.History,
                        text = stringResource(R.string.about_changelog),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.about_developer_name),
                    subtitle = stringResource(R.string.about_developer_subtitle),
                    icon = Icons.Rounded.Person,
                    emphasize = true,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiInfoCapsule(
                    icon = Icons.Rounded.Info,
                    text = stringResource(R.string.about_declaration_text).trimMargin(),
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
        }
    }
}
