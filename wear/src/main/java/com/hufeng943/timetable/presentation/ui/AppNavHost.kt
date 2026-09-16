package com.hufeng943.timetable.presentation.ui

import com.hufeng943.timetable.presentation.ui.common.LocalLiquidGlassBackdrop

import com.kyant.backdrop.backdrops.rememberLayerBackdrop

import com.kyant.backdrop.backdrops.layerBackdrop

import android.os.Build

import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.hufeng943.timetable.presentation.ui.common.TimetableBackgroundMode
import com.hufeng943.timetable.presentation.ui.theme.GalaxyAiAmbientLayer
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
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
    val useLiquidGlass = config.isLiquidGlassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    AppScaffold(
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
            LocalLiquidGlassBackdrop provides globalGlassBackdrop.takeIf { useLiquidGlass }
        ) {
            AppBackground(config = config, liquidGlassBackdrop = globalGlassBackdrop.takeIf { useLiquidGlass })
            Box(Modifier.fillMaxSize()) {
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


@Composable
private fun AppBackground(config: com.hufeng943.timetable.presentation.ui.common.AppConfig, liquidGlassBackdrop: com.kyant.backdrop.Backdrop?) {
    val backgroundBitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        key1 = config.timetableBackgroundMode,
        key2 = config.timetableBackgroundImagePath,
    ) {
        value = if (config.timetableBackgroundMode == TimetableBackgroundMode.IMAGE) {
            withContext(Dispatchers.IO) {
                config.timetableBackgroundImagePath?.let { path ->
                    runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
                }
            }
        } else null
    }

    Box(Modifier.fillMaxSize().then(if (liquidGlassBackdrop != null) Modifier.layerBackdrop(liquidGlassBackdrop) else Modifier).background(AppTheme.colors.background)) {
        // Background blur is a single full-screen layer, not one blur pass per card. This keeps
        // the optional effect predictable on Wear OS while allowing it to be disabled entirely.
        Box(
            Modifier
                .fillMaxSize()
                .then(if (config.blurredBackgroundEnabled && config.backgroundBlurRadius > 0f) Modifier.blur(config.backgroundBlurRadius.dp) else Modifier)
        ) {
            when (config.timetableBackgroundMode) {
                TimetableBackgroundMode.SOLID -> Unit
                TimetableBackgroundMode.THEME ->
                    GalaxyAiAmbientLayer(RectangleShape, strength = 0.92f * config.backgroundBrightness)
                TimetableBackgroundMode.IMAGE -> backgroundBitmap?.let { bitmap ->
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
        val scrimAlpha = ((1f - config.backgroundBrightness) * 0.70f).coerceIn(0f, 0.70f)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
    }
}
