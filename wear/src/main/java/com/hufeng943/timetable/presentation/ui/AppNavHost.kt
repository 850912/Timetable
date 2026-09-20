package com.hufeng943.timetable.presentation.ui




import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import com.hufeng943.timetable.presentation.ui.common.TimetableBackgroundMode
import com.hufeng943.timetable.presentation.ui.theme.GalaxyAiAmbientLayer
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import android.app.ActivityManager
import android.os.Build
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
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
    val lowRamDevice = remember(context) {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
    }
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
        imageBackgroundBlurEnabled = false,
        imageBackgroundFluidEnabled = false,
        liquidGlassEffect = com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect.SOFT,
    ) else storedConfig
    val globalGlassBackdrop = rememberLayerBackdrop()
    // Do not capture a live backdrop when the renderer will use the lightweight fallback anyway.
    // Avoiding the capture layer entirely matters on constrained Wear OS hardware.
    val useBackdropEffects = config.isLiquidGlassEnabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !lowRamDevice

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
                // This app owns one global background *behind* the navigation host. Wear navigation
                // applies the background scrim as an actual full-screen background to the current
                // destination, so any opaque/black value hides the selected wallpaper while idle.
                // Keep that base transparent; the content scrim still supplies gesture shading.
                CompositionLocalProvider(
                    LocalSwipeToDismissBackgroundScrimColor provides Color.Transparent,
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
                    SettingScreen(appConfigViewModel = appConfigViewModel)
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
                        if (config.imageBackgroundFluidEnabled) {
                            FluidImageToneBackground(bitmap = bitmap, softened = config.imageBackgroundBlurEnabled)
                        } else {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.06f; scaleY = 1.06f }
                                    .then(if (config.imageBackgroundBlurEnabled) Modifier.blur(10.dp) else Modifier),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    } else {
                        // Never leave a black/empty page when a previously selected image becomes unreadable.
                        GalaxyAiAmbientLayer(RectangleShape, strength = 1f)
                    }
                }
            }
        }
        // User photos can contain arbitrarily bright/detail-heavy regions. Keep white Wear text and
        // translucent cards readable with a guaranteed contrast scrim, then apply brightness on top.
        val imageReadability = if (config.timetableBackgroundMode == TimetableBackgroundMode.IMAGE) 0.24f else 0f
        val scrimAlpha = (imageReadability + (1f - config.backgroundBrightness) * 0.34f).coerceIn(0f, 0.50f)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
    }
}



private fun sampleRegionTone(bitmap: android.graphics.Bitmap, centerX: Float, centerY: Float): Color {
    // Average a small grid instead of trusting one pixel. This is still tiny work (25 reads per tone)
    // but produces a much more representative palette for photos with highlights or text overlays.
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0
    val spreadX = 0.18f
    val spreadY = 0.18f
    for (iy in -2..2) {
        for (ix in -2..2) {
            val fx = (centerX + ix * spreadX / 4f).coerceIn(0f, 1f)
            val fy = (centerY + iy * spreadY / 4f).coerceIn(0f, 1f)
            val x = ((bitmap.width - 1) * fx).toInt().coerceIn(0, bitmap.width - 1)
            val y = ((bitmap.height - 1) * fy).toInt().coerceIn(0, bitmap.height - 1)
            val pixel = bitmap.getPixel(x, y)
            red += android.graphics.Color.red(pixel)
            green += android.graphics.Color.green(pixel)
            blue += android.graphics.Color.blue(pixel)
            count++
        }
    }
    return Color(
        android.graphics.Color.rgb(
            (red / count).toInt(),
            (green / count).toInt(),
            (blue / count).toInt(),
        )
    ).copy(alpha = 1f)
}

@Composable
private fun FluidImageToneBackground(bitmap: android.graphics.Bitmap, softened: Boolean) {
    val config = LocalAppConfig.current
    val tones = remember(bitmap) {
        listOf(
            sampleRegionTone(bitmap, .24f, .25f),
            sampleRegionTone(bitmap, .74f, .28f),
            sampleRegionTone(bitmap, .30f, .74f),
            sampleRegionTone(bitmap, .76f, .77f),
        )
    }
    // Do not run an infinite transition when global UI animations are disabled. The generated
    // background remains visible, only the slow drift stops.
    val motion = if (config.uiAnimationsEnabled) {
        val transition = rememberInfiniteTransition(label = "toneFluid")
        transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(16_000), RepeatMode.Reverse),
            label = "toneFluidMotion",
        ).value
    } else 0f

    // Fluid mode is already made from low-frequency gradients. Keep three static GPU layers and
    // move only the two transparent radial layers; this avoids rebuilding gradient shaders on every
    // animation frame and is substantially cheaper than a full-screen RenderEffect blur on Wear.
    val radialScale = if (softened) 1.18f else 1f
    val spotAlpha = if (softened) .78f else .90f
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.linearGradient(
                    colors = listOf(tones[0], tones[3]),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
            )
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = radialScale
                    scaleY = radialScale
                    translationX = size.width * motion * .055f
                    translationY = size.height * -motion * .035f
                }
        ) {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(tones[1].copy(alpha = spotAlpha), Color.Transparent),
                    center = Offset(size.width * .72f, size.height * .28f),
                    radius = size.maxDimension * .72f,
                )
            )
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = radialScale
                    scaleY = radialScale
                    translationX = size.width * -motion * .05f
                    translationY = size.height * motion * .03f
                }
        ) {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(tones[2].copy(alpha = spotAlpha), Color.Transparent),
                    center = Offset(size.width * .28f, size.height * .76f),
                    radius = size.maxDimension * .72f,
                )
            )
        }
    }
}
