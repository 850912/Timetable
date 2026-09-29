package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.hufeng943.timetable.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.math.roundToInt
import kotlin.time.Clock

@Composable
fun rememberPullToDatePickerState(
    maxDragDistanceDp: Dp = 120.dp, refreshThresholdDp: Dp = 110.dp
): PullToDatePickerState {
    val density = LocalDensity.current
    val maxDragDistance = remember(density) { with(density) { maxDragDistanceDp.toPx() } }
    val refreshThreshold = remember(density) { with(density) { refreshThresholdDp.toPx() } }
    val scope = rememberCoroutineScope()

    return remember(maxDragDistance, refreshThreshold, scope) {
        PullToDatePickerState(maxDragDistance, refreshThreshold, scope)
    }
}

fun Modifier.pullToDatePickerDrag(
    state: PullToDatePickerState,
    allowOpenFromTop: Boolean,
    topPullStartHeightPx: Float,
): Modifier = this.then(
    Modifier.pointerInput(state, allowOpenFromTop, topPullStartHeightPx) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

            // Course lists open the picker exclusively through nested scroll, which knows whether
            // the list is actually at its top.  The direct detector is kept for the empty page and
            // for gestures that start on the already-revealed picker itself.  This prevents a
            // downward swipe in the middle of a scrolled timetable from opening the date picker.
            val startsOnRevealedPicker = state.dragOffset > 0f &&
                down.position.y <= state.dragOffset + viewConfiguration.touchSlop
            val startsInTopPullZone = allowOpenFromTop &&
                state.dragOffset <= 0f && down.position.y <= topPullStartHeightPx
            if (!startsOnRevealedPicker && !startsInTopPullZone) {
                return@awaitEachGesture
            }

            var lastX = down.position.x
            var lastY = down.position.y
            var totalX = 0f
            var totalY = 0f
            var verticalDragActive = false
            var rejectedAsHorizontal = false
            val touchSlop = viewConfiguration.touchSlop

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: break
                if (change.changedToUp() || !change.pressed) break

                val x = change.position.x
                val y = change.position.y
                val deltaX = x - lastX
                val deltaY = y - lastY
                lastX = x
                lastY = y

                if (!verticalDragActive) {
                    totalX += deltaX
                    totalY += deltaY
                    val absX = kotlin.math.abs(totalX)
                    val absY = kotlin.math.abs(totalY)
                    if (absX < touchSlop && absY < touchSlop) continue

                    // Axis-lock before consuming anything. A horizontal gesture belongs to the
                    // home HorizontalPager (or another horizontal child) and must pass through.
                    if (absX > absY) {
                        rejectedAsHorizontal = true
                        break
                    }
                    verticalDragActive = true
                }

                // Only a gesture that has been positively identified as vertical may move the
                // date picker. This prevents slight Y jitter during a left/right swipe from
                // stealing the whole pointer stream from HorizontalPager.
                val oldOffset = state.dragOffset
                state.snapTo(oldOffset + deltaY)
                if (state.dragOffset != oldOffset) change.consumePositionChange()
            }

            if (verticalDragActive && !rejectedAsHorizontal) {
                state.animateToTarget()
            }
        }
    })

@Composable
fun rememberPullToRefreshConnection(
    scrollState: TransformingLazyColumnState,
    state: PullToDatePickerState,
    isTouching: () -> Boolean,
    canOpenFromTouch: () -> Boolean,
): NestedScrollConnection {
    return remember(scrollState, state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!isTouching()) {
                    return Offset.Zero
                }
                val currentOffset = state.dragOffset

                // Do not let tiny vertical jitter from a horizontal pager gesture open/close the
                // date picker. Nested scroll should participate only when the delta is vertical.
                if (kotlin.math.abs(available.x) > kotlin.math.abs(available.y)) {
                    return Offset.Zero
                }

                // At the top of the timetable, pulling down reveals the picker.
                // When it is already visible, swiping up dismisses it.
                val isOpening =
                    available.y > 0 && canOpenFromTouch() &&
                        !scrollState.canScrollBackward && currentOffset < state.maxDragDistance
                val isCollapsing = available.y < 0 && currentOffset > 0

                if (isOpening || isCollapsing) {
                    val newOffset =
                        (currentOffset + available.y).coerceIn(0f, state.maxDragDistance)
                    val consumed = newOffset - currentOffset
                    state.snapTo(newOffset)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                state.animateToTarget()
                return Velocity.Zero
            }
        }
    }
}

@Composable
fun PullToDatePicker(
    state: PullToDatePickerState,
    dragOffset: Float,
    refreshThreshold: Float,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    allowDirectTopPull: Boolean = false,
    content: @Composable () -> Unit
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    val scrollTrigger = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    val progress = (dragOffset / refreshThreshold).coerceIn(0f, 1f)

    val density = LocalDensity.current
    val offsetShiftPx = remember(density) { with(density) { 65.dp.toPx() } }
    val topPullStartHeightPx = remember(density) { with(density) { 64.dp.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToDatePickerDrag(
                state = state,
                allowOpenFromTop = allowDirectTopPull,
                topPullStartHeightPx = topPullStartHeightPx,
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, (dragOffset - offsetShiftPx).roundToInt()) }
                .graphicsLayer {
                    alpha = progress
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable {
                        onDateSelected(today)
                        scrollTrigger.tryEmit(Unit)
                    }
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.today),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            HorizontalDatePicker(
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                isVisible = progress > 0f,
                scrollTrigger = scrollTrigger
            )
        }

        Box(
            modifier = Modifier.offset {
                IntOffset(0, dragOffset.roundToInt())
            }) {
            content()
        }
    }
}
