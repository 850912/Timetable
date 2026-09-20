package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/**
 * Navigation host for flows that are already displayed inside the app-level
 * Wear [androidx.wear.compose.navigation.SwipeDismissableNavHost].
 *
 * Nested swipe hosts can briefly translate the parent destination when a child destination is
 * pushed on some Wear OS 6 builds. That looks like the previous screen sliding left and then
 * disappearing. Child flows therefore use a plain Navigation-Compose host with explicit no-motion
 * transitions. The top-level host still owns Wear OS swipe-to-dismiss and its native motion.
 */
@Composable
fun WearInternalNavHost(
    navController: NavHostController,
    startDestination: String,
    builder: NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
        builder = builder,
    )
}
