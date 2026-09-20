package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.currentBackStackEntryAsState

/**
 * Wear-native navigation host for child flows.
 *
 * The project previously mixed two navigation engines: the root used Wear OS
 * [SwipeDismissableNavHost], while settings/edit child flows used the phone/tablet
 * Navigation-Compose [androidx.navigation.compose.NavHost]. On Wear OS 6 this produces visibly
 * different push/pop behavior and, on some builds, a short left translation followed by an
 * abrupt replacement.
 *
 * Child flows now use the Wear navigator as well. When the child graph is at its root destination
 * its swipe gesture is disabled so the app-level host can handle back. Once a child destination is
 * pushed, the child host owns swipe-to-dismiss and reveals the previous child screen underneath.
 * This avoids nested hosts competing for the same edge gesture while keeping one Wear-native
 * transition model throughout the app.
 */
@Composable
fun WearInternalNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit,
) {
    // Reading the current entry as State makes the ownership switch recompute after every push/pop.
    val currentEntry by navController.currentBackStackEntryAsState()
    val hasChildBackStack = currentEntry != null && navController.previousBackStackEntry != null

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
        userSwipeEnabled = hasChildBackStack,
        builder = builder,
    )
}
