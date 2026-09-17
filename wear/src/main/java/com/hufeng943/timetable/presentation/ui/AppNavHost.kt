package com.hufeng943.timetable.presentation.ui




import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.hufeng943.timetable.presentation.ui.common.TimetableBackgroundMode
import com.hufeng943.timetable.presentation.ui.theme.GalaxyAiAmbientLayer
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.navigation.NavType
import androidx.navigation.navigation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults.rememberTimeSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalLiquidGlassBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.screens.detail.CourseDetailScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.CourseListScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.EditCourseColorScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.EditCourseDeleteConfirmScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.EditCourseLocationScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.EditCourseMainScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.EditCourseNameScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.EditCourseTeacherScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.timeslot.EditTimeSlotScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.timeslot.TimeSlotListScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.timetable.TimetableListScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.tools.ScheduleToolsScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.timetable.EditTimetableScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.tools.DayArrangementScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.tools.CourseAdjustmentScreen
import com.hufeng943.timetable.presentation.ui.screens.home.HomeScreen
import com.hufeng943.timetable.presentation.ui.screens.more.about.AboutLibrariesScreen
import com.hufeng943.timetable.presentation.ui.screens.more.about.AboutScreen
import com.hufeng943.timetable.presentation.ui.screens.more.about.DeveloperOptionsScreen
import com.hufeng943.timetable.presentation.ui.screens.more.settings.SettingScreen
import com.hufeng943.timetable.presentation.viewmodel.AppConfigViewModel
import com.hufeng943.timetable.presentation.viewmodel.edit.course.EditCourseViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppNavHost(appConfigViewModel: AppConfigViewModel = hiltViewModel()) {
    val navController = rememberSwipeDismissableNavController()
    val config by appConfigViewModel.appConfig.collectAsStateWithLifecycle()
    val globalGlassBackdrop = rememberLayerBackdrop()
    // Backdrop capture is shared by all glass surfaces; the renderer falls back below Android 13.
    val useBackdropEffects = config.isLiquidGlassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    AppScaffold(
        containerColor = Color.Transparent,
        timeText = {
            if (config.isShowTopTime) {
                TimeText(
                    backgroundColor = Color.Transparent,
                    timeSource = if (config.is24HourFormat) {
                        rememberTimeSource("HH:mm")
                    } else {
                        rememberTimeSource("h:mm a")
                    },
                )
            }
        }
    ) {
        CompositionLocalProvider(
            LocalNavController provides navController,
            LocalAppConfig provides config,
            LocalLiquidGlassBackdrop provides globalGlassBackdrop.takeIf { useBackdropEffects }
        ) {
            AppBackground(
                config = config,
                modifier = if (useBackdropEffects) Modifier.layerBackdrop(globalGlassBackdrop) else Modifier,
            )
            Box(Modifier.fillMaxSize()) {
                // Keep the navigation transition fully owned by Wear Compose.
                // Custom swipe scrims caused a visible discontinuity at the end of back gestures.
                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = NavRoutes.MAIN
                ) {
                composable(NavRoutes.MAIN) {
                    HomeScreen()
                }

                composable(NavRoutes.COURSE_DETAIL) {
                    CourseDetailScreen()
                }

                composable(NavRoutes.LIST_TIMETABLE) { TimetableListScreen() }
                composable(NavRoutes.SCHEDULE_TOOLS) { ScheduleToolsScreen() }

                composable(NavRoutes.LIST_COURSE) {
                    CourseListScreen()
                }

                navigation(
                    startDestination = NavRoutes.EDIT_COURSE_MAIN,
                    route = NavRoutes.EDIT_COURSE
                ) {
                    argument(NavArgs.TABLE_ID) {
                        type = NavType.LongType
                    }
                    argument(NavArgs.COURSE_ID) {
                        type = NavType.LongType
                    }

                    composable(NavRoutes.EDIT_COURSE_MAIN) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(NavRoutes.EDIT_COURSE)
                        }
                        val viewModel: EditCourseViewModel =
                            hiltViewModel(parentEntry)
                        EditCourseMainScreen(viewModel)
                    }

                    // 子页面路由改为带参数的模板
                    composable(NavRoutes.EDIT_COURSE_NAME) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(NavRoutes.EDIT_COURSE)
                        }
                        val viewModel: EditCourseViewModel =
                            hiltViewModel(parentEntry)
                        EditCourseNameScreen(viewModel)
                    }

                    composable(NavRoutes.EDIT_COURSE_LOCATION) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(NavRoutes.EDIT_COURSE)
                        }
                        val viewModel: EditCourseViewModel =
                            hiltViewModel(parentEntry)
                        EditCourseLocationScreen(viewModel)
                    }

                    composable(NavRoutes.EDIT_COURSE_TEACHER) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(NavRoutes.EDIT_COURSE)
                        }
                        val viewModel: EditCourseViewModel =
                            hiltViewModel(parentEntry)
                        EditCourseTeacherScreen(viewModel)
                    }

                    composable(NavRoutes.EDIT_COURSE_COLOR) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(NavRoutes.EDIT_COURSE)
                        }
                        val viewModel: EditCourseViewModel =
                            hiltViewModel(parentEntry)
                        EditCourseColorScreen(viewModel)
                    }

                    composable(NavRoutes.EDIT_COURSE_DELETE_CONFIRM) { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(NavRoutes.EDIT_COURSE)
                        }
                        val viewModel: EditCourseViewModel =
                            hiltViewModel(parentEntry)
                        EditCourseDeleteConfirmScreen(viewModel)
                    }
                }

                composable(NavRoutes.LIST_TIMESLOT) {
                    TimeSlotListScreen()
                }

                composable(NavRoutes.EDIT_TIMESLOT) {
                    EditTimeSlotScreen()
                }



                composable(NavRoutes.MORE_ABOUT) {
                    AboutScreen()
                }

                composable(NavRoutes.MORE_ABOUT_LIBRARIES) {
                    AboutLibrariesScreen()
                }

                composable(NavRoutes.MORE_ABOUT_DEVELOPER) {
                    DeveloperOptionsScreen()
                }


                composable(NavRoutes.MORE_SETTINGS) {
                    SettingScreen()
                }

                composable(NavRoutes.MORE_DAY_ARRANGEMENT) {
                    DayArrangementScreen()
                }

                composable(NavRoutes.MORE_COURSE_ADJUSTMENT) {
                    CourseAdjustmentScreen()
                }

                composable(NavRoutes.EDIT_TIMETABLE) {
                    EditTimetableScreen()
                }
                }
            }
        }
    }
}


private fun decodeWearBackground(path: String, maxSide: Int = 512): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    var largest = maxOf(bounds.outWidth, bounds.outHeight)
    while (largest / sample > maxSide) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample.coerceAtLeast(1)
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }
    return BitmapFactory.decodeFile(path, options)?.also { bitmap ->
        // Start texture upload before the first Image draw; this avoids doing all preparation on
        // the first visible frame when a custom background becomes active.
        bitmap.prepareToDraw()
    }
}

@Composable
private fun AppBackground(
    config: com.hufeng943.timetable.presentation.ui.common.AppConfig,
    modifier: Modifier = Modifier,
) {
    val backgroundBitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        key1 = config.timetableBackgroundMode,
        key2 = config.timetableBackgroundImagePath,
    ) {
        val decoded = if (config.timetableBackgroundMode == TimetableBackgroundMode.IMAGE) {
            withContext(Dispatchers.IO) {
                config.timetableBackgroundImagePath?.let { path ->
                    runCatching { decodeWearBackground(path) }.getOrNull()
                }
            }
        } else null
        value = decoded
        awaitDispose {
            decoded?.takeUnless { it.isRecycled }?.recycle()
        }
    }

    // The base color sits BEHIND the selected background. Do not place a readability scrim above
    // THEME/IMAGE: it both obscures the user's background and adds a full-screen alpha blend.
    Box(modifier.fillMaxSize().background(AppTheme.colors.background)) {
        // AppConfig now exposes the real source luminance. Legacy scrim-era values are converted
        // in PreferenceStorage, so rendering and the settings percentage share one meaning.
        val sourceLuminance = config.backgroundBrightness.coerceIn(0.10f, 1f)
        when (config.timetableBackgroundMode) {
            TimetableBackgroundMode.SOLID -> Unit
            TimetableBackgroundMode.THEME -> {
                // Keep the ambient effect at full material strength. Brightness changes its source
                // colors, not the layer opacity, so it no longer doubles as an "effect intensity".
                GalaxyAiAmbientLayer(
                    shape = RectangleShape,
                    strength = 1f,
                    sourceLuminance = sourceLuminance,
                )
            }
            TimetableBackgroundMode.IMAGE -> {
                val bitmap = backgroundBitmap
                if (bitmap != null) {
                    val brightnessMatrix = remember(sourceLuminance) {
                        ColorMatrix().apply {
                            setToScale(sourceLuminance, sourceLuminance, sourceLuminance, 1f)
                        }
                    }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(brightnessMatrix),
                    )
                } else {
                    // Keep the fallback visible without adding an overlay above the backdrop.
                    GalaxyAiAmbientLayer(
                        shape = RectangleShape,
                        strength = 1f,
                        sourceLuminance = sourceLuminance,
                    )
                }
            }
        }
    }
}
