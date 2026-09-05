package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.RotarySnapLayoutInfoProvider
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.time.format.TextStyle
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HorizontalDatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    scrollTrigger: Flow<Unit>? = null
) {
    val totalDaysCount = Int.MAX_VALUE
    val baseIndex = totalDaysCount / 2
    val todayEpochDays = remember {
        Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays()
    }

    val selectedIndex = remember(selectedDate, todayEpochDays) {
        (baseIndex + (selectedDate.toEpochDays() - todayEpochDays)).toInt()
    }

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var isInitialLayout by remember { mutableStateOf(true) }
    var isRotaryDriven by remember { mutableStateOf(false) }

    var lastScrolledIndex by remember { mutableStateOf(-1) }

    val snapLayoutInfoProvider = rememberLazyListRotarySnapLayoutInfoProvider(listState)

    LaunchedEffect(isVisible) {
        if (isVisible) {
            focusRequester.requestFocus()
        }
    }

    val configuration = LocalConfiguration.current
    val horizontalPadding = remember(configuration, density) {
        val screenWidth = configuration.screenWidthDp.dp
        val itemWidth = 46.dp
        (screenWidth - itemWidth * 3) / 2
    }
    val centerOffsetPx = remember(density, horizontalPadding) {
        with(density) { horizontalPadding.toPx() }
    }

    LaunchedEffect(scrollTrigger, centerOffsetPx) {
        scrollTrigger?.collect {
            if (centerOffsetPx > 0f) {
                listState.animateScrollToItem(selectedIndex, -centerOffsetPx.roundToInt())
            }
        }
    }

    LaunchedEffect(selectedIndex, centerOffsetPx) {
        if (centerOffsetPx > 0f) {
            if (isInitialLayout) {
                listState.scrollToItem(selectedIndex, -centerOffsetPx.roundToInt())
                lastScrolledIndex = selectedIndex
                isInitialLayout = false
            } else {
                val currentCenterIndex = snapLayoutInfoProvider.currentItemIndex
                if (currentCenterIndex != selectedIndex) {
                    val currentVisibleIndex = listState.firstVisibleItemIndex
                    val distance = abs(selectedIndex - currentVisibleIndex)
                    if (distance > 10) {
                        val jumpIndex = if (selectedIndex > currentVisibleIndex) {
                            selectedIndex - 3
                        } else {
                            selectedIndex + 3
                        }
                        listState.scrollToItem(jumpIndex, -centerOffsetPx.roundToInt())
                    }

                    lastScrolledIndex = selectedIndex
                    listState.animateScrollToItem(selectedIndex, -centerOffsetPx.roundToInt())
                }
            }
        }
    }

    LaunchedEffect(listState.interactionSource) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                isRotaryDriven = false
            }
        }
    }

    val isScrollInProgress = listState.isScrollInProgress
    LaunchedEffect(isScrollInProgress) {
        if (!isScrollInProgress && isRotaryDriven) {
            delay(300.milliseconds)
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val viewportCenter =
                    (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2

                val closestItem = visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    abs(itemCenter - viewportCenter)
                }

                closestItem?.let { item ->
                    val targetDate =
                        LocalDate.fromEpochDays(todayEpochDays + (item.index - baseIndex))
                    if (targetDate != selectedDate) {
                        onDateSelected(targetDate)
                    }
                }
            }
            isRotaryDriven = false
        }
    }

    LazyRow(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onPreRotaryScrollEvent {
                isRotaryDriven = true
                false
            }
            .rotaryScrollable(
                behavior = RotaryScrollableDefaults.snapBehavior(
                    scrollableState = listState,
                    layoutInfoProvider = snapLayoutInfoProvider
                ), focusRequester = focusRequester
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            count = totalDaysCount, key = { it }) { index ->
            val date = remember(index, todayEpochDays) {
                LocalDate.fromEpochDays(todayEpochDays + (index - baseIndex))
            }

            val isSelected = date == selectedDate
            val backgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .clickable {
                        onDateSelected(date)
                        if (date == selectedDate) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(
                                    selectedIndex,
                                    -centerOffsetPx.roundToInt()
                                )
                            }
                        }
                    }, contentAlignment = Alignment.Center
            ) {

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = date.dayOfWeek.toDisplayString(TextStyle.SHORT),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = date.day.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberLazyListRotarySnapLayoutInfoProvider(
    state: LazyListState
): RotarySnapLayoutInfoProvider = remember(state) {
    object : RotarySnapLayoutInfoProvider {
        override val averageItemSize: Float
            get() = state.layoutInfo.let { info ->
                if (info.visibleItemsInfo.isEmpty()) 0f
                else (info.visibleItemsInfo.sumOf { it.size }
                    .toFloat() / info.visibleItemsInfo.size) + info.mainAxisItemSpacing
            }

        override val currentItemIndex: Int
            get() = state.layoutInfo.let { info ->
                if (info.visibleItemsInfo.isEmpty()) 0
                else {
                    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                    info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
                        ?: 0
                }
            }

        override val currentItemOffset: Float
            get() = state.layoutInfo.let { info ->
                if (info.visibleItemsInfo.isEmpty()) 0f
                else {
                    // 1. 计算屏幕/Viewport的中心点
                    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2

                    // 2. 找到距离中心最近的那一天
                    val closest =
                        info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }

                    if (closest == null) 0f
                    else {
                        // 💡【核心修正】：用 center 减去 item 的中心点！
                        // 只有这样，正负号才是对的，才能把最近的那一天牢牢吸附在“今日”的正下方
                        (center - (closest.offset + closest.size / 2)).toFloat()
                    }
                }
            }

        override val totalItemCount: Int
            get() = state.layoutInfo.totalItemsCount
    }
}

