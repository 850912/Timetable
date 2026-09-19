package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/**
 * Navigation host for child flows that already live inside the app-level
 * Wear navigation host.
 *
 * The app-level host owns swipe-to-dismiss. Nesting another swipe host makes
 * both containers animate the outgoing page, which produces the visible
 * left-shift/ghost frame on forward navigation. Internal flows therefore use
 * a plain NavHost. Its default crossfade keeps the current page stationary
 * while the destination is composed and also avoids competing back gestures.
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
        builder = builder,
    )
}
