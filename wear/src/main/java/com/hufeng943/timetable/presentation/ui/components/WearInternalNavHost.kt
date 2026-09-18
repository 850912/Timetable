package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.wear.compose.navigation.SwipeDismissableNavHost

/**
 * Navigation host for child flows that already live inside the app-level
 * [SwipeDismissableNavHost].
 *
 * Use the same Wear navigation implementation at every navigation depth so
 * programmatic forward/back transitions have one motion language. The nested
 * host only owns swipe-to-dismiss while it actually has an internal page to
 * pop; at its start destination the gesture is handed back to the outer host.
 * This avoids gesture competition without replacing Wear navigation motion
 * with a different fade animation.
 */
@Composable
fun WearInternalNavHost(
    navController: NavHostController,
    startDestination: String,
    builder: NavGraphBuilder.() -> Unit,
) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val canPopInternally = currentEntry != null && navController.previousBackStackEntry != null

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = startDestination,
        userSwipeEnabled = canPopInternally,
        builder = builder,
    )
}
