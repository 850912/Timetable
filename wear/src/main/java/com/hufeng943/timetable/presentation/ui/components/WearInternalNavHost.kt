package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.wear.compose.material3.MaterialTheme
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig

/**
 * Navigation host for child flows that already live inside the app-level SwipeDismissableNavHost.
 *
 * Child hosts must not implement a second swipe-dismiss surface. For their programmatic page
 * changes we use Wear Material 3's MotionScheme effects spec (alpha only), avoiding spatial page
 * translation that can reveal the previous destination at the circular screen edge.
 */
@Composable
fun WearInternalNavHost(
    navController: NavHostController,
    startDestination: String,
    builder: NavGraphBuilder.() -> Unit,
) {
    val animationsEnabled = LocalAppConfig.current.uiAnimationsEnabled
    val effectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            if (animationsEnabled) fadeIn(effectsSpec) else EnterTransition.None
        },
        exitTransition = {
            if (animationsEnabled) fadeOut(effectsSpec) else ExitTransition.None
        },
        popEnterTransition = {
            if (animationsEnabled) fadeIn(effectsSpec) else EnterTransition.None
        },
        popExitTransition = {
            if (animationsEnabled) fadeOut(effectsSpec) else ExitTransition.None
        },
        builder = builder,
    )
}
