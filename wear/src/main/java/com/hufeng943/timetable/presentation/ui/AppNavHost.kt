package com.hufeng943.timetable.presentation.ui




import androidx.compose.ui.draw.blur
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.navigation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults.rememberTimeSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalLiquidGlassBackdrop
import com.hufeng943.timetable.presentation.ui.common.AppPowerSaveMode
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
import androidx.wear.compose.foundation.LocalSwipeToDismissBackgroundScrimColor
import androidx.wear.compose.foundation.LocalSwipeToDismissContentScrimColor

@Composable
fun AppNavHost(appConfigViewModel: AppConfigViewModel = hiltViewModel()) {
    val navController = rememberSwipeDismissableNavController()
    val storedConfig by appConfigViewModel.appConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var isPowerSaveMode by remember(powerManager) { mutableStateOf(powerManager.isPowerSaveMode) }
    DisposableEffect(context, powerManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    isPowerSaveMode = powerManager.isPowerSaveMode
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    // Resolve the user's app power policy against Android's real PowerManager state.
    // In effective power-save mode we avoid backdrop capture/blur, chromatic passes and motion;
    // these are GPU/CPU-heavy on Wear OS and provide no scheduling correctness benefit.
    val effectivePowerSave = when (storedConfig.powerSaveMode) {
        AppPowerSaveMode.FOLLOW_SYSTEM -> isPowerSaveMode
        AppPowerSaveMode.ALWAYS_ON -> true
        AppPowerSaveMode.ALWAYS_OFF -> false
    }
    val config = if (effectivePowerSave) storedConfig.copy(
        uiAnimationsEnabled = false,
        isLiquidGlassEnabled = false,
        glassChromaticAberration = false,
        glassBlurEnabled = false,
        liquidGlassEffect = com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect.SOFT,
    ) else storedConfig
    val globalGlassBackdrop = rememberLayerBackdrop()
    // Xiaomi Watch 5 / China-ROM devices have shown vendor GPU crashes in RuntimeShader-heavy
    // backdrop paths. Keep the same visual language but route Xiaomi watches through the cheap
    // translucent renderer; other devices retain the shared real-time backdrop.
    val conservativeVendorRenderer = remember {
        Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
            Build.BRAND.equals("Xiaomi", ignoreCase = true)
    }
    val useBackdropEffects = config.isLiquidGlassEnabled && !conservativeVendorRenderer

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
                CompositionLocalProvider(
                    LocalSwipeToDismissBackgroundScrimColor provides Color.Black.copy(alpha = 0.18f),
                    LocalSwipeToDismissContentScrimColor provides Color.Black.copy(alpha = 0.10f),
                ) {
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
}


private fun decodeWearBackground(path: String, maxSide: Int = 512): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    var largest = maxOf(bounds.outWidth, bounds.outHeight)
    while (largest / sample > maxSide * 2) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample.coerceAtLeast(1)
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }
    return BitmapFactory.decodeFile(path, options)
}

private fun extractFluidPalette(bitmap: android.graphics.Bitmap): Pair<Color, Color> {
    fun average(xStart: Int, xEnd: Int, yStart: Int, yEnd: Int): Color {
        var r = 0L; var g = 0L; var b = 0L; var count = 0L
        val stepX = ((xEnd - xStart) / 10).coerceAtLeast(1)
        val stepY = ((yEnd - yStart) / 10).coerceAtLeast(1)
        var y = yStart
        while (y < yEnd) {
            var x = xStart
            while (x < xEnd) {
                val c = bitmap.getPixel(x.coerceIn(0, bitmap.width - 1), y.coerceIn(0, bitmap.height - 1))
                r += android.graphics.Color.red(c); g += android.graphics.Color.green(c); b += android.graphics.Color.blue(c); count++
                x += stepX
            }
            y += stepY
        }
        if (count == 0L) return Color(0xFF6577B8)
        return Color((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
    }
    val w = bitmap.width.coerceAtLeast(1); val h = bitmap.height.coerceAtLeast(1)
    return average(0, w, 0, (h * 2 / 3).coerceAtLeast(1)) to
        average(0, w, (h / 3).coerceAtMost(h - 1), h)
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
        val decoded = if (config.timetableBackgroundMode == TimetableBackgroundMode.IMAGE || config.timetableBackgroundMode == TimetableBackgroundMode.FLUID_IMAGE) {
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

    val fluidPalette = remember(backgroundBitmap, config.timetableBackgroundMode) {
        if (config.timetableBackgroundMode == TimetableBackgroundMode.FLUID_IMAGE) {
            backgroundBitmap?.let(::extractFluidPalette)
        } else null
    }

    Box(modifier.fillMaxSize().background(AppTheme.colors.background)) {
        // Background blur is a single full-screen layer, not one blur pass per card. This keeps
        // the optional effect predictable on Wear OS while allowing it to be disabled entirely.
        Box(
            Modifier
                .fillMaxSize()
                
        ) {
            when (config.timetableBackgroundMode) {
                TimetableBackgroundMode.SOLID -> Unit
                TimetableBackgroundMode.THEME ->
                    GalaxyAiAmbientLayer(RectangleShape, strength = 1f)
                TimetableBackgroundMode.IMAGE -> {
                    val bitmap = backgroundBitmap
                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        GalaxyAiAmbientLayer(RectangleShape, strength = 1f)
                    }
                }
                TimetableBackgroundMode.FLUID_IMAGE -> {
                    val bitmap = backgroundBitmap
                    if (bitmap != null) {
                        val palette = fluidPalette ?: extractFluidPalette(bitmap)
                        GalaxyAiAmbientLayer(
                            RectangleShape,
                            strength = 1f,
                            primaryOverride = palette.first,
                            secondaryOverride = palette.second,
                        )
                    } else {
                        GalaxyAiAmbientLayer(RectangleShape, strength = 1f)
                    }
                }
            }
        }
        val scrimAlpha = ((1f - config.backgroundBrightness) * 0.35f).coerceIn(0f, 0.35f)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
    }
}
