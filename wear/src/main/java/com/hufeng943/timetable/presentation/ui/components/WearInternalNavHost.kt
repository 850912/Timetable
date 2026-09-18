package com.hufeng943.timetable.presentation.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/**
 * Navigation host for a child flow that already lives inside the app-level
 * [androidx.wear.compose.navigation.SwipeDismissableNavHost].
 *
 * There must be only one owner for full-screen Wear navigation motion. The app-level Wear host
 * owns that motion. Child flows keep their own back stacks (which preserves the existing routes,
 * Hilt ViewModel scopes and state restoration), but deliberately perform no second full-screen
 * enter/exit animation. Running a copied Wear slide/scale/fade transition here while the parent
 * destination is still active can expose an intermediate/ghost frame at the end of navigation.
 *
 * Predictive back is opted out at application level. The innermost enabled BackHandler consumes
 * Back while a child flow has history; once the child reaches its start destination, Back falls
 * through to the app-level handler.
 */
@Composable
fun WearInternalNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit,
) {
    BackHandler(enabled = navController.previousBackStackEntry != null) {
        navController.popBackStack()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
        builder = builder,
    )
}
