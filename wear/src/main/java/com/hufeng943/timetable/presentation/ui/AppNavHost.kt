package com.hufeng943.timetable.presentation.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults.rememberTimeSource
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.hufeng943.timetable.presentation.ui.common.AppPowerSaveMode
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalLiquidGlassBackdrop
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.TimetableBackgroundMode
import com.hufeng943.timetable.presentation.ui.screens.detail.CourseDetailScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.CourseListScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.EditCourseColorScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.EditCourseDeleteConfirmScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.EditCourseLocationScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.EditCourseMainScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.EditCourseNameScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.course.EditCourseTeacherScreen
import com.hufeng943.timetable.presentation.ui.screens.edit.timeslot.*
import com.hufeng943.timetable.presentation.ui.screens.edit.timetable.*
import com.hufeng943.timetable.presentation.ui.screens.edit.tools.*
import com.hufeng943.timetable.presentation.ui.screens.home.HomeScreen
import com.hufeng943.timetable.presentation.ui.screens.more.about.AboutLibrariesScreen
import com.hufeng943.timetable.presentation.ui.screens.more.about.AboutScreen
import com.hufeng943.timetable.presentation.ui.screens.more.about.DeveloperOptionsScreen
import com.hufeng943.timetable.presentation.ui.screens.more.settings.*
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
import com.hufeng943.timetable.presentation.ui.theme.GalaxyAiAmbientLayer
import com.hufeng943.timetable.presentation.viewmodel.AppConfigViewModel
import com.hufeng943.timetable.presentation.viewmodel.edit.course.EditCourseViewModel
import com.hufeng943.timetable.presentation.viewmodel.edit.timetable.EditTimetableViewModel
import com.hufeng943.timetable.presentation.viewmodel.edit.timeslot.EditTimeSlotViewModel
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleAdjustmentViewModel
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleToolsViewModel
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        // Do not place a full-screen black scrim above Wear Navigation 3 host.
        // It causes the previous screen to be visible briefly during the pop animation.
        // Keep dimming subtle and behind navigation transitions.
        val scrimAlpha = ((1f - config.backgroundBrightness) * 0.18f).coerceIn(0f, 0.18f)
        if (scrimAlpha > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
        }
    }
}


private fun routeSegments(route: String): List<String> =
    route.split('/').filter { it.isNotEmpty() }

private fun normalizeRoute(requested: String, current: String?): String {
    return when (requested) {
        NavRoutes.SCHEDULE_TOOLS -> NavRoutes.SCHEDULE_TOOLS_MAIN
        NavRoutes.MORE_SETTINGS -> NavRoutes.MORE_SETTINGS_MAIN
        NavRoutes.MORE_DAY_ARRANGEMENT -> NavRoutes.MORE_DAY_ARRANGEMENT_MAIN
        NavRoutes.MORE_COURSE_ADJUSTMENT -> NavRoutes.MORE_COURSE_ADJUSTMENT_MAIN
        NavRoutes.EDIT_TIMETABLE -> {
            current?.let { routeSegments(it).takeIf { it.firstOrNull() == "edit_timetable" && it.size >= 2 } }
                ?.let { "edit_timetable/${it[1]}" }
                ?: requested
        }
        NavRoutes.EDIT_COURSE -> {
            current?.let { routeSegments(it).takeIf { it.firstOrNull() == "edit_course" && it.size >= 3 } }
                ?.let { "edit_course/${it[1]}/${it[2]}" }
                ?: requested
        }
        NavRoutes.EDIT_TIMESLOT -> {
            current?.let { routeSegments(it).takeIf { it.firstOrNull() == "edit_timeslot" && it.size >= 3 } }
                ?.let { "edit_timeslot/${it[1]}/${it[2]}" }
                ?: requested
        }
        NavRoutes.EDIT_TIMETABLE_NAME,
        NavRoutes.EDIT_TIMETABLE_START_DATE,
        NavRoutes.EDIT_TIMETABLE_END_DATE,
        NavRoutes.EDIT_TIMETABLE_COLOR,
        NavRoutes.EDIT_TIMETABLE_DELETE_CONFIRM -> {
            val parts = current?.let(::routeSegments)
            if (parts?.firstOrNull() == "edit_timetable" && parts.size >= 2) {
                "${parts[0]}/${parts[1]}/${requested.substringAfterLast('/')}"
            } else requested
        }
        NavRoutes.EDIT_TIMESLOT_START_TIME,
        NavRoutes.EDIT_TIMESLOT_END_TIME,
        NavRoutes.EDIT_TIMESLOT_DATES,
        NavRoutes.EDIT_TIMESLOT_WEEK_DAY,
        NavRoutes.EDIT_TIMESLOT_RECURRENCE,
        NavRoutes.EDIT_TIMESLOT_REMARK,
        NavRoutes.EDIT_TIMESLOT_DELETE_CONFIRM -> {
            val parts = current?.let(::routeSegments)
            if (parts?.firstOrNull() == "edit_timeslot" && parts.size >= 3) {
                "${parts[0]}/${parts[1]}/${parts[2]}/${requested.substringAfterLast('/')}"
            } else requested
        }
        else -> requested
    }
}

@Composable
fun AppNavHost(appConfigViewModel: AppConfigViewModel = hiltViewModel()) {
    val navBackStack = rememberNavBackStack(TimetableRouteKey(NavRoutes.MAIN))
    val strategy = rememberSwipeDismissableSceneStrategy<TimetableRouteKey>()
    val storedConfig by appConfigViewModel.appConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
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

    val conservativeVendorRenderer = remember {
        Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
            Build.BRAND.equals("Xiaomi", ignoreCase = true)
    }
    val useBackdropEffects = config.isLiquidGlassEnabled && !conservativeVendorRenderer
    val globalGlassBackdrop = if (useBackdropEffects) rememberLayerBackdrop() else null

    val navigator = remember(navBackStack) {
        object : com.hufeng943.timetable.presentation.ui.common.TimetableNavigator {
            override fun navigate(route: String) {
                val current = navBackStack.lastOrNull()?.route
                val normalized = normalizeRoute(route, current)
                if (normalized.isBlank() || normalized == current) return
                navBackStack.add(TimetableRouteKey(normalized))
            }

            override fun pop() {
                if (navBackStack.size > 1) {
                    navBackStack.removeAt(navBackStack.lastIndex)
                }
            }

            override fun popBackStack(route: String, inclusive: Boolean): Boolean {
                if (navBackStack.size <= 1) return false
                val current = navBackStack.lastOrNull()?.route
                val target = normalizeRoute(route, current)
                val index = navBackStack.indexOfLast { it.route == target }
                if (index < 0) return false
                val removeFrom = if (inclusive) index else index + 1
                if (removeFrom >= navBackStack.size) return false
                repeat(navBackStack.size - removeFrom) {
                    navBackStack.removeAt(navBackStack.lastIndex)
                }
                return true
            }
        }
    }

    AppScaffold(
        containerColor = Color.Transparent,
        timeText = {
            if (config.isShowTopTime) {
                TimeText(
                    backgroundColor = Color.Transparent,
                    timeSource = if (config.is24HourFormat) {
                        rememberTimeSource("HH:mm")
                    } else {
                        rememberTimeSource("h:mm")
                    },
                )
            }
        }
    ) {
        CompositionLocalProvider(
            LocalNavController provides navigator,
            LocalAppConfig provides config,
            LocalLiquidGlassBackdrop provides globalGlassBackdrop.takeIf { useBackdropEffects },
        ) {
            AppBackground(
                config = config,
                modifier = if (useBackdropEffects && globalGlassBackdrop != null) {
                    Modifier.layerBackdrop(globalGlassBackdrop)
                } else {
                    Modifier
                },
            )

            NavDisplay(
                backStack = navBackStack,
                modifier = Modifier.fillMaxSize(),
                onBack = {
                    if (navBackStack.size > 1) {
                        navBackStack.removeAt(navBackStack.lastIndex)
                    }
                },
                sceneStrategies = listOf(strategy),
                entryProvider = entryProvider {
                    entry<TimetableRouteKey> { key ->
                        val route = key.route
                        when {
                            route == NavRoutes.MAIN -> HomeScreen()
                            route.startsWith("course_detail/") -> CourseDetailScreen()
                            route == NavRoutes.LIST_TIMETABLE -> TimetableListScreen()
                            route.startsWith("list_course/") -> CourseListScreen()
                            route.startsWith("list_timeslot/") -> TimeSlotListScreen()

                            routeSegments(route).size == 2 &&
                                routeSegments(route).firstOrNull() == "edit_timetable" -> {
                                val tableId = routeSegments(route)[1].toLongOrNull() ?: -1L
                                EditTimetableScreen(
                                    hiltViewModel<EditTimetableViewModel>(key = "edit-timetable:$tableId")
                                )
                            }
                            route.endsWith("/name") &&
                                route.startsWith("edit_timetable/") -> {
                                val parts = routeSegments(route)
                                val tableId = parts.getOrNull(1)?.toLongOrNull() ?: -1L
                                EditTimetableNameScreen(
                                    hiltViewModel<EditTimetableViewModel>(key = "edit-timetable:$tableId")
                                )
                            }
                            route.endsWith("/start_date") &&
                                route.startsWith("edit_timetable/") -> {
                                val tableId = routeSegments(route).getOrNull(1)?.toLongOrNull() ?: -1L
                                EditTimetableStartDateScreen(
                                    hiltViewModel<EditTimetableViewModel>(key = "edit-timetable:$tableId")
                                )
                            }
                            route.endsWith("/end_date") &&
                                route.startsWith("edit_timetable/") -> {
                                val tableId = routeSegments(route).getOrNull(1)?.toLongOrNull() ?: -1L
                                EditTimetableEndDateScreen(
                                    hiltViewModel<EditTimetableViewModel>(key = "edit-timetable:$tableId")
                                )
                            }
                            route.endsWith("/color") &&
                                route.startsWith("edit_timetable/") -> {
                                val tableId = routeSegments(route).getOrNull(1)?.toLongOrNull() ?: -1L
                                EditTimetableColorScreen(
                                    hiltViewModel<EditTimetableViewModel>(key = "edit-timetable:$tableId")
                                )
                            }
                            route.endsWith("/delete_confirm") &&
                                route.startsWith("edit_timetable/") -> {
                                val tableId = routeSegments(route).getOrNull(1)?.toLongOrNull() ?: -1L
                                EditTimetableDeleteConfirmScreen(
                                    hiltViewModel<EditTimetableViewModel>(key = "edit-timetable:$tableId")
                                )
                            }

                            routeSegments(route).size == 3 &&
                                route.startsWith("edit_course/") -> {
                                val parts = routeSegments(route)
                                val tableId = parts[1].toLongOrNull() ?: -1L
                                val courseId = parts[2].toLongOrNull() ?: -1L
                                EditCourseMainScreen(
                                    hiltViewModel<EditCourseViewModel>(key = "edit-course:$tableId:$courseId")
                                )
                            }
                            route.endsWith("/name") && route.startsWith("edit_course/") -> {
                                val parts = routeSegments(route)
                                EditCourseNameScreen(
                                    hiltViewModel<EditCourseViewModel>(
                                        key = "edit-course:${parts.getOrNull(1)}:${parts.getOrNull(2)}"
                                    )
                                )
                            }
                            route.endsWith("/location") && route.startsWith("edit_course/") -> {
                                val parts = routeSegments(route)
                                EditCourseLocationScreen(
                                    hiltViewModel<EditCourseViewModel>(
                                        key = "edit-course:${parts.getOrNull(1)}:${parts.getOrNull(2)}"
                                    )
                                )
                            }
                            route.endsWith("/teacher") && route.startsWith("edit_course/") -> {
                                val parts = routeSegments(route)
                                EditCourseTeacherScreen(
                                    hiltViewModel<EditCourseViewModel>(
                                        key = "edit-course:${parts.getOrNull(1)}:${parts.getOrNull(2)}"
                                    )
                                )
                            }
                            route.endsWith("/color") && route.startsWith("edit_course/") -> {
                                val parts = routeSegments(route)
                                EditCourseColorScreen(
                                    hiltViewModel<EditCourseViewModel>(
                                        key = "edit-course:${parts.getOrNull(1)}:${parts.getOrNull(2)}"
                                    )
                                )
                            }
                            route.endsWith("/delete_confirm") && route.startsWith("edit_course/") -> {
                                val parts = routeSegments(route)
                                EditCourseDeleteConfirmScreen(
                                    hiltViewModel<EditCourseViewModel>(
                                        key = "edit-course:${parts.getOrNull(1)}:${parts.getOrNull(2)}"
                                    )
                                )
                            }

                            routeSegments(route).size == 3 &&
                                route.startsWith("edit_timeslot/") -> {
                                val parts = routeSegments(route)
                                val courseId = parts[1].toLongOrNull() ?: -1L
                                val slotId = parts[2].toLongOrNull() ?: -1L
                                EditTimeSlotScreen(
                                    hiltViewModel<EditTimeSlotViewModel>(key = "edit-timeslot:$courseId:$slotId")
                                )
                            }
                            route.startsWith("edit_timeslot/") && route.endsWith("/start_time") -> {
                                val parts = routeSegments(route)
                                EditTimeSlotStartTimeScreen(
                                    hiltViewModel<EditTimeSlotViewModel>(key = "edit-timeslot:${parts.getOrNull(1)}:${parts.getOrNull(2)}")
                                )
                            }
                            route.startsWith("edit_timeslot/") && route.endsWith("/end_time") -> {
                                val parts = routeSegments(route)
                                EditTimeSlotEndTimeScreen(
                                    hiltViewModel<EditTimeSlotViewModel>(key = "edit-timeslot:${parts.getOrNull(1)}:${parts.getOrNull(2)}")
                                )
                            }
                            route.startsWith("edit_timeslot/") && route.endsWith("/dates") -> {
                                val parts = routeSegments(route)
                                EditTimeSlotDatesScreen(
                                    hiltViewModel<EditTimeSlotViewModel>(key = "edit-timeslot:${parts.getOrNull(1)}:${parts.getOrNull(2)}")
                                )
                            }
                            route.startsWith("edit_timeslot/") && route.endsWith("/week_day") -> {
                                val parts = routeSegments(route)
                                EditTimeSlotWeekDayScreen(
                                    hiltViewModel<EditTimeSlotViewModel>(key = "edit-timeslot:${parts.getOrNull(1)}:${parts.getOrNull(2)}")
                                )
                            }
                            route.startsWith("edit_timeslot/") && route.endsWith("/recurrence") -> {
                                val parts = routeSegments(route)
                                EditTimeSlotRecurrenceScreen(
                                    hiltViewModel<EditTimeSlotViewModel>(key = "edit-timeslot:${parts.getOrNull(1)}:${parts.getOrNull(2)}")
                                )
                            }
                            route.startsWith("edit_timeslot/") && route.endsWith("/remark") -> {
                                val parts = routeSegments(route)
                                EditTimeSlotRemarkScreen(
                                    hiltViewModel<EditTimeSlotViewModel>(key = "edit-timeslot:${parts.getOrNull(1)}:${parts.getOrNull(2)}")
                                )
                            }
                            route.startsWith("edit_timeslot/") && route.endsWith("/delete_confirm") -> {
                                val parts = routeSegments(route)
                                EditTimeSlotDeleteConfirmScreen(
                                    hiltViewModel<EditTimeSlotViewModel>(key = "edit-timeslot:${parts.getOrNull(1)}:${parts.getOrNull(2)}")
                                )
                            }

                            route == NavRoutes.SCHEDULE_TOOLS_MAIN -> {
                                ScheduleToolsScreen(
                                    hiltViewModel<ScheduleToolsViewModel>(key = "schedule-tools")
                                )
                            }
                            route == NavRoutes.SCHEDULE_TOOLS_START -> ScheduleToolsStartDateScreen(
                                hiltViewModel<ScheduleToolsViewModel>(key = "schedule-tools")
                            )
                            route == NavRoutes.SCHEDULE_TOOLS_END -> ScheduleToolsEndDateScreen(
                                hiltViewModel<ScheduleToolsViewModel>(key = "schedule-tools")
                            )
                            route == NavRoutes.SCHEDULE_TOOLS_WINDOW_START -> ScheduleToolsWindowStartScreen(
                                hiltViewModel<ScheduleToolsViewModel>(key = "schedule-tools")
                            )
                            route == NavRoutes.SCHEDULE_TOOLS_WINDOW_END -> ScheduleToolsWindowEndScreen(
                                hiltViewModel<ScheduleToolsViewModel>(key = "schedule-tools")
                            )
                            route == NavRoutes.SCHEDULE_TOOLS_DAYS -> ScheduleToolsDaysScreen(
                                hiltViewModel<ScheduleToolsViewModel>(key = "schedule-tools")
                            )
                            route == NavRoutes.SCHEDULE_TOOLS_OFFSET -> ScheduleToolsOffsetScreen(
                                hiltViewModel<ScheduleToolsViewModel>(key = "schedule-tools")
                            )

                            route == NavRoutes.MORE_ABOUT -> AboutScreen()
                            route == NavRoutes.MORE_ABOUT_LIBRARIES -> AboutLibrariesScreen()
                            route == NavRoutes.MORE_ABOUT_DEVELOPER -> DeveloperOptionsScreen()

                            route == NavRoutes.MORE_SETTINGS_MAIN -> SettingScreen()
                            route == NavRoutes.MORE_SETTINGS_UI -> SettingsUiManagementScreen(appConfigViewModel)
                            route == NavRoutes.MORE_SETTINGS_LANGUAGE -> SettingsLanguageScreen(appConfigViewModel)
                            route == NavRoutes.MORE_SETTINGS_TIME_FORMAT -> SettingsTimeFormatScreen(appConfigViewModel)
                            route == NavRoutes.MORE_SETTINGS_FIRST_DAY -> SettingsFirstDayScreen(appConfigViewModel)
                            route == NavRoutes.MORE_SETTINGS_POWER_SAVE -> SettingsPowerSaveScreen(appConfigViewModel)
                            route == NavRoutes.MORE_SETTINGS_EXPORT -> SettingsExportScreen()
                            route == NavRoutes.MORE_SETTINGS_IMPORT -> SettingsImportScreen()
                            route == NavRoutes.MORE_SETTINGS_THEME -> SettingsThemeScreen()
                            route == NavRoutes.MORE_SETTINGS_BACKGROUND -> SettingsBackgroundScreen(appConfigViewModel)
                            route == NavRoutes.MORE_SETTINGS_LIQUID_GLASS -> LiquidGlassAdvancedPager(
                                config = config,
                                onEnabledChange = appConfigViewModel::updateLiquidGlassEnabled,
                                onEffectChange = appConfigViewModel::updateLiquidGlassEffect,
                                onChromaticAberrationChange = appConfigViewModel::updateGlassChromaticAberration,
                                onLensDistortionChange = appConfigViewModel::updateGlassLensDistortion,
                                onBlurEnabledChange = appConfigViewModel::updateGlassBlurEnabled,
                                onBlurRadiusChange = appConfigViewModel::updateGlassBlurRadius,
                                onOpacityClick = { navigator.navigateSingle(NavRoutes.MORE_SETTINGS_GLASS_OPACITY) },
                                onClarityClick = { navigator.navigateSingle(NavRoutes.MORE_SETTINGS_GLASS_CLARITY) },
                                onBrightnessClick = { navigator.navigateSingle(NavRoutes.MORE_SETTINGS_BACKGROUND_BRIGHTNESS) },
                            )
                            route == NavRoutes.MORE_SETTINGS_GLASS_OPACITY -> LiquidGlassIntegerAdjustPager(
                                title = "玻璃底色浓度",
                                value = (config.glassOpacity * 100).toInt(),
                                range = 5..95,
                                onApply = { appConfigViewModel.updateGlassOpacity(it / 100f) },
                                onClose = { navigator.pop() },
                            )
                            route == NavRoutes.MORE_SETTINGS_GLASS_CLARITY -> LiquidGlassIntegerAdjustPager(
                                title = "玻璃清透度",
                                value = (config.glassClarity * 100).toInt(),
                                range = 0..100,
                                onApply = { appConfigViewModel.updateGlassClarity(it / 100f) },
                                onClose = { navigator.pop() },
                            )
                            route == NavRoutes.MORE_SETTINGS_BACKGROUND_BRIGHTNESS -> LiquidGlassIntegerAdjustPager(
                                title = "背景亮度",
                                value = (config.backgroundBrightness * 100).toInt(),
                                range = 10..100,
                                onApply = { appConfigViewModel.updateBackgroundBrightness(it / 100f) },
                                onClose = { navigator.pop() },
                            )

                            route == NavRoutes.MORE_DAY_ARRANGEMENT_MAIN -> {
                                DayArrangementScreen(
                                    hiltViewModel<ScheduleAdjustmentViewModel>(key = "day-arrangement")
                                )
                            }
                            route == NavRoutes.MORE_DAY_ARRANGEMENT_DATE -> DayArrangementDateScreen(
                                hiltViewModel<ScheduleAdjustmentViewModel>(key = "day-arrangement")
                            )
                            route == NavRoutes.MORE_DAY_ARRANGEMENT_SOURCE -> DayArrangementSourceScreen(
                                hiltViewModel<ScheduleAdjustmentViewModel>(key = "day-arrangement")
                            )

                            route == NavRoutes.MORE_COURSE_ADJUSTMENT_MAIN -> {
                                CourseAdjustmentScreen(
                                    hiltViewModel<ScheduleAdjustmentViewModel>(key = "course-adjustment")
                                )
                            }
                            route == NavRoutes.MORE_COURSE_ADJUSTMENT_DATE -> CourseAdjustmentDateScreen(
                                hiltViewModel<ScheduleAdjustmentViewModel>(key = "course-adjustment")
                            )
                            route == NavRoutes.MORE_COURSE_ADJUSTMENT_A_COURSES -> CourseAdjustmentACoursesScreen(
                                hiltViewModel<ScheduleAdjustmentViewModel>(key = "course-adjustment")
                            )
                            route == NavRoutes.MORE_COURSE_ADJUSTMENT_A_SLOTS -> CourseAdjustmentASlotsScreen(
                                hiltViewModel<ScheduleAdjustmentViewModel>(key = "course-adjustment")
                            )
                            route == NavRoutes.MORE_COURSE_ADJUSTMENT_B_COURSES -> CourseAdjustmentBCoursesScreen(
                                hiltViewModel<ScheduleAdjustmentViewModel>(key = "course-adjustment")
                            )
                            route == NavRoutes.MORE_COURSE_ADJUSTMENT_B_SLOTS -> CourseAdjustmentBSlotsScreen(
                                hiltViewModel<ScheduleAdjustmentViewModel>(key = "course-adjustment")
                            )
                        }
                    }
                }
            )
        }
    }
}
