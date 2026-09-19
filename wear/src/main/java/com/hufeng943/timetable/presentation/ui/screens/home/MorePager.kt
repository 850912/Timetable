package com.hufeng943.timetable.presentation.ui.screens.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleButton

@Composable
fun MorePager() {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "?"
    }

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(scrollState),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollState, hapticFeedbackEnabled = false),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(stringResource(R.string.more_title))
                }
            }

            item {
                OneUiCapsuleButton(
                    icon = Icons.Rounded.Edit,
                    label = stringResource(R.string.more_menu_edit),
                    secondaryLabel = stringResource(R.string.more_edit_summary),
                    emphasize = true,
                    onClick = { navController.navigateSingle(NavRoutes.LIST_TIMETABLE) },
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleButton(
                    icon = Icons.Rounded.EventRepeat,
                    label = stringResource(R.string.more_day_arrangement),
                    secondaryLabel = stringResource(R.string.more_day_arrangement_summary),
                    onClick = { navController.navigateSingle(NavRoutes.MORE_DAY_ARRANGEMENT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleButton(
                    icon = Icons.Rounded.SwapCalls,
                    label = stringResource(R.string.more_course_adjustment),
                    secondaryLabel = stringResource(R.string.more_course_adjustment_summary),
                    onClick = { navController.navigateSingle(NavRoutes.MORE_COURSE_ADJUSTMENT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleButton(
                    icon = Icons.Rounded.Settings,
                    label = stringResource(R.string.more_menu_settings),
                    secondaryLabel = stringResource(R.string.more_settings_summary),
                    onClick = { navController.navigateSingle(NavRoutes.MORE_SETTINGS) },
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }

            item {
                OneUiCapsuleButton(
                    icon = Icons.Rounded.Info,
                    label = stringResource(R.string.more_menu_about),
                    secondaryLabel = stringResource(R.string.about_version_summary, versionName),
                    onClick = { navController.navigateSingle(NavRoutes.MORE_ABOUT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
