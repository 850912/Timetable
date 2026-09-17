package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Drag state for the home date picker.
 *
 * Drag input can arrive many times per frame on Wear OS. Keeping the raw drag offset as
 * Compose state avoids launching a coroutine for every pointer/nested-scroll delta. We only
 * allocate animation work when the gesture finishes and the picker settles open/closed.
 */
@Stable
class PullToDatePickerState(
    val maxDragDistance: Float,
    val refreshThreshold: Float,
    private val coroutineScope: CoroutineScope,
) {
    var dragOffset by mutableFloatStateOf(0f)
        private set

    private var settleJob: Job? = null

    fun snapTo(value: Float) {
        settleJob?.cancel()
        dragOffset = value.coerceIn(0f, maxDragDistance)
    }

    fun animateToTarget() {
        settleJob?.cancel()
        if (dragOffset <= 0f) return

        val targetValue = if (dragOffset >= refreshThreshold) refreshThreshold else 0f
        val startValue = dragOffset
        settleJob = coroutineScope.launch {
            Animatable(startValue).animateTo(
                targetValue = targetValue,
                animationSpec = tween(durationMillis = 300),
            ) {
                dragOffset = value
            }
        }
    }
}
