package com.hufeng943.timetable.presentation.ui.common

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Small navigation abstraction used by Wear screens.
 *
 * The UI does not need to know whether the app uses Navigation 2 or Navigation 3.
 * Keeping this surface tiny also prevents screen code from depending on a concrete
 * NavController implementation.
 */
interface TimetableNavigator {
    fun navigate(route: String)
    fun pop()
    fun popBackStack(route: String, inclusive: Boolean = false): Boolean
}

val LocalNavController = staticCompositionLocalOf<TimetableNavigator> {
    error("未提供 TimetableNavigator！")
}

fun TimetableNavigator.navigateSingle(route: String) {
    navigate(route)
}

fun TimetableNavigator.popSafe() {
    pop()
}
