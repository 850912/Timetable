package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.wear.compose.navigation.SwipeDismissableNavHost

/**
 * Wear-aware navigation host for a child flow.
 *
 * A plain Navigation Compose NavHost is not sufficient here: on Wear OS the outer
 * SwipeDismissableNavHost can otherwise own the edge swipe while a child destination is visible,
 * causing a single right-swipe to pop the whole outer screen instead of the child's top entry.
 *
 * Child flows therefore use their own Wear SwipeDismissableNavHost. Swipe-to-dismiss is enabled
 * only while the child controller actually has an entry to pop. At the child's start destination
 * it is disabled, so the edge gesture is left to the parent host. This establishes one gesture
 * owner for each navigation depth:
 *
 * child detail -> child root -> parent destination -> app root -> system/home.
 *
 * On API 36+ Wear Compose maps this host to predictive back; older Wear OS versions retain the
 * native swipe-to-dismiss interaction. Do not add BackHandler/PredictiveBackHandler around this
 * host: doing so would compete with the navigation library and can break back-to-home animation.
 */
@Composable
fun WearInternalNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit,
) {
    // Observe the entry so userSwipeEnabled is recomputed immediately after push/pop.
    val currentEntry by navController.currentBackStackEntryAsState()
    val canPopChild = currentEntry != null && navController.previousBackStackEntry != null

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
        userSwipeEnabled = canPopChild,
        builder = builder,
    )
}
