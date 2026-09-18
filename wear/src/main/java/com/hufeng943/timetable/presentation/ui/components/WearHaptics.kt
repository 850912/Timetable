package com.hufeng943.timetable.presentation.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** Small, dependency-free haptic adapter. Keeps feedback consistent across Galaxy Watch and Wear OS. */
@Composable
fun rememberWearHaptics(): WearHaptics {
    val view = LocalView.current
    return remember(view) { WearHaptics { constant -> view.performHapticFeedback(constant) } }
}

class WearHaptics internal constructor(private val perform: (Int) -> Boolean) {
    fun toggle() { perform(HapticFeedbackConstants.CONTEXT_CLICK) }
    fun tick() { perform(HapticFeedbackConstants.CLOCK_TICK) }
    fun confirm() { perform(HapticFeedbackConstants.CONFIRM) }
}
