package com.hufeng943.timetable.presentation.ui.common

import com.hufeng943.timetable.logging.AppLogger
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("未提供 NavController！")
}

fun NavController.navigateSingle(
    route: String, builder: NavOptionsBuilder.() -> Unit = {}
) {
    if (currentBackStackEntry?.destination?.route != route) {
        navigate(route) {
            launchSingleTop = true
            restoreState = true
            builder()
        }
        AppLogger.debug("NavController", "跳转至: $route")

    } else {
        AppLogger.debug("NavController", "跳转被拦截: $route")
    }
}

fun NavController.popSafe() {
    val currentRoute = currentBackStackEntry?.destination?.route
    if (currentRoute != null && previousBackStackEntry != null) {
        popBackStack()
        AppLogger.debug("NavController", "弹出: $currentRoute")
    } else {
        AppLogger.debug("NavController", "弹出被拦截: $currentRoute")
    }
}
