package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

enum class CourseStatus {
    NOT_STARTED,
    IN_PROGRESS,
    FINISHED
}

data class LiveCountdownState(
    val status: CourseStatus,
    val label: String,
    val countdownText: String
)

enum class TickerFrequency {
    STOPPED,
    LOW_FREQUENCY,
    SECOND_LEVEL
}

@Immutable
data class CourseTimeRange(
    val start: LocalTime,
    val end: LocalTime
)

@Composable
fun rememberScheduleSeconds(
    isAmbient: Boolean = false,
    todaySlots: List<CourseTimeRange> = emptyList()
): Int {
    var currentSecondOfDay by remember {
        mutableIntStateOf(getCurrentSecondOfDay())
    }

    LaunchedEffect(isAmbient, todaySlots) {
        while (isActive) {
            currentSecondOfDay = getCurrentSecondOfDay()
            val nowSec = currentSecondOfDay
            val nowMillis = java.lang.System.currentTimeMillis()

            val mode = calculateTickerMode(nowSec, todaySlots, isAmbient)

            when (mode) {
                TickerFrequency.STOPPED,
                TickerFrequency.LOW_FREQUENCY -> {
                    val delayMillis = 60_000L - (nowMillis % 60_000L)
                    delay(if (delayMillis > 0) delayMillis else 60_000L)
                }
                TickerFrequency.SECOND_LEVEL -> {
                    val delayMillis = 1000L - (nowMillis % 1000L)
                    delay(if (delayMillis > 0) delayMillis else 1000L)
                }
            }
        }
    }

    return currentSecondOfDay
}

private fun getCurrentSecondOfDay(): Int {
    val nowTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    return nowTime.hour * 3600 + nowTime.minute * 60 + nowTime.second
}

private fun calculateTickerMode(
    nowSeconds: Int,
    slots: List<CourseTimeRange>,
    isAmbient: Boolean
): TickerFrequency {
    if (slots.isEmpty() || isAmbient) {
        return if (isAmbient && slots.isNotEmpty()) TickerFrequency.LOW_FREQUENCY else TickerFrequency.STOPPED
    }

    var hasUpcoming = false
    var requiresSecondTick = false

    for (range in slots) {
        val startSec = range.start.hour * 3600 + range.start.minute * 60 + range.start.second
        val endSec = range.end.hour * 3600 + range.end.minute * 60 + range.end.second
        val (status, diff) = calculateSlotStatus(nowSeconds, startSec, endSec)

        if (status == CourseStatus.IN_PROGRESS) {
            requiresSecondTick = true
            break
        } else if (status == CourseStatus.NOT_STARTED) {
            hasUpcoming = true
            if (diff <= 60) {
                requiresSecondTick = true
                break
            }
        }
    }

    return when {
        requiresSecondTick -> TickerFrequency.SECOND_LEVEL
        hasUpcoming -> TickerFrequency.LOW_FREQUENCY
        else -> TickerFrequency.STOPPED
    }
}

fun calculateSlotStatus(nowSec: Int, startSec: Int, endSec: Int): Pair<CourseStatus, Int> {
    return if (startSec <= endSec) {
        when {
            nowSec < startSec -> CourseStatus.NOT_STARTED to (startSec - nowSec)
            nowSec < endSec -> CourseStatus.IN_PROGRESS to (endSec - nowSec)
            else -> CourseStatus.FINISHED to 0
        }
    } else {
        when {
            nowSec >= startSec -> CourseStatus.IN_PROGRESS to ((86400 - nowSec) + endSec)
            nowSec < endSec -> CourseStatus.IN_PROGRESS to (endSec - nowSec)
            else -> CourseStatus.NOT_STARTED to (startSec - nowSec)
        }
    }
}

fun deriveCourseLiveState(
    nowSeconds: Int,
    startTime: LocalTime,
    endTime: LocalTime,
    isAmbient: Boolean = false
): LiveCountdownState {
    val startSeconds = startTime.hour * 3600 + startTime.minute * 60 + startTime.second
    val endSeconds = endTime.hour * 3600 + endTime.minute * 60 + endTime.second
    val (status, diff) = calculateSlotStatus(nowSeconds, startSeconds, endSeconds)

    return when (status) {
        CourseStatus.NOT_STARTED -> {
            val mm = diff / 60
            val ss = (diff % 60).toString().padStart(2, '0')
            val countdownText = if (isAmbient) {
                "距上课 ${mm}m"
            } else if (diff <= 3600) {
                "距上课 $mm:$ss"
            } else {
                "距上课 ${mm / 60}h ${mm % 60}m"
            }
            LiveCountdownState(
                status = CourseStatus.NOT_STARTED,
                label = "即将上课",
                countdownText = countdownText
            )
        }
        CourseStatus.IN_PROGRESS -> {
            val mm = diff / 60
            val ss = (diff % 60).toString().padStart(2, '0')
            LiveCountdownState(
                status = CourseStatus.IN_PROGRESS,
                label = "正在上课",
                countdownText = if (isAmbient) "${mm}m" else "$mm:$ss"
            )
        }
        CourseStatus.FINISHED -> {
            LiveCountdownState(
                status = CourseStatus.FINISHED,
                label = "已结束",
                countdownText = "下课"
            )
        }
    }
}
