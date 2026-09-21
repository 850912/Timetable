package com.hufeng943.timetable.presentation.ui.common

import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("未提供 NavController！")
}

fun NavController.navigateSingle(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    val currentEntry = currentBackStackEntry
    val targetEntry = runCatching { getBackStackEntry(route) }.getOrNull()
    val targetDestination = targetEntry?.destination
    val isCurrentDestination = currentEntry != null && (
        currentEntry.destination.hierarchy.any { destination -> destination.route == route } ||
            targetEntry === currentEntry ||
            (targetDestination is NavGraph &&
                currentEntry.destination.hierarchy.any { destination ->
                    destination === targetDestination
                })
    )

    if (isCurrentDestination) {
        Log.d("NavController", "跳转被拦截: $route")
        return
    }

    navigate(route) {
        launchSingleTop = true
        restoreState = true
        builder()
    }
    Log.d("NavController", "跳转至: $route")
}

fun NavController.popSafe() {
    val currentRoute = currentBackStackEntry?.destination?.route
    if (currentRoute != null && previousBackStackEntry != null) {
        popBackStack()
        Log.d("NavController", "弹出: $currentRoute")
    } else {
        Log.d("NavController", "弹出被拦截: $currentRoute")
    }
}
