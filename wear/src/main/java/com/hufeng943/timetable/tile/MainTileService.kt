package com.hufeng943.timetable.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.hufeng943.timetable.R
import com.hufeng943.timetable.data.PreferenceStorage
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.resolveDate
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import java.time.ZoneId

private const val RESOURCES_VERSION = "2"
private const val SURFACE = 0xFF17181B.toInt()
private const val SURFACE_ALT = 0xFF202228.toInt()
private const val PRIMARY = 0xFF5B8CFF.toInt()
private const val AI_PURPLE = 0xFF8B5CF6.toInt()
private const val TEXT_PRIMARY = 0xFFF7F8FC.toInt()
private const val TEXT_SECONDARY = 0xFFAFB4C0.toInt()

private data class TileCourse(
    val name: String,
    val start: String,
    val end: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val location: String?,
    val teacher: String?,
    val color: Int?,
    val startEpochMillis: Long = 0L,
    val endEpochMillis: Long = 0L,
)

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class MainTileService : SuspendingTileService() {

    @Inject lateinit var repository: TimetableRepository
    @Inject lateinit var preferenceStorage: PreferenceStorage

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = resources()

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val is24Hour = runCatching { preferenceStorage.appConfigFlow.first().is24HourFormat }.getOrDefault(true)
        val courses = runCatching {
            repository.getAllTimetables().first().coursesForDate(today, is24Hour)
        }.getOrDefault(emptyList())

        return tile(requestParams, this, courses)
    }
}

private fun resources(): ResourceBuilders.Resources =
    ResourceBuilders.Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .build()

private fun tile(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
    courses: List<TileCourse>,
): TileBuilders.Tile {
    val timeline = TimelineBuilders.Timeline.Builder()
    val fallback = courses.currentAndUpcoming()
    timeline.addTimelineEntry(
        TimelineBuilders.TimelineEntry.Builder()
            .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(tileLayout(requestParams, context, fallback)).build())
            .build()
    )

    val timelineReady = courses.isNotEmpty() && courses.all { it.startEpochMillis > 0L && it.endEpochMillis > it.startEpochMillis }
    if (timelineReady) {
        val zone = ZoneId.systemDefault()
        val javaToday = java.time.LocalDate.now(zone)
        val dayStart = javaToday.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = javaToday.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        var cursor = dayStart
        courses.forEachIndexed { index, course ->
            if (cursor < course.startEpochMillis) {
                val upcoming = courses.drop(index).take(2)
                timeline.addTimelineEntry(
                    TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(tileLayout(requestParams, context, upcoming)).build())
                        .setValidity(
                            TimelineBuilders.TimeInterval.Builder()
                                .setStartMillis(cursor)
                                .setEndMillis(course.startEpochMillis)
                                .build()
                        ).build()
                )
            }
            val during = listOf(course) + courses.drop(index + 1).take(1)
            timeline.addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(tileLayout(requestParams, context, during)).build())
                    .setValidity(
                        TimelineBuilders.TimeInterval.Builder()
                            .setStartMillis(course.startEpochMillis)
                            .setEndMillis(course.endEpochMillis)
                            .build()
                    ).build()
            )
            cursor = maxOf(cursor, course.endEpochMillis)
        }
        if (cursor < dayEnd) {
            timeline.addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(tileLayout(requestParams, context, emptyList())).build())
                    .setValidity(
                        TimelineBuilders.TimeInterval.Builder()
                            .setStartMillis(cursor)
                            .setEndMillis(dayEnd)
                            .build()
                    ).build()
            )
        }
    }

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(timeline.build())
        .setFreshnessIntervalMillis(6 * 60 * 60 * 1000L)
        .build()
}

private fun tileLayout(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
    courses: List<TileCourse>,
): LayoutElementBuilders.LayoutElement {
    val column = LayoutElementBuilders.Column.Builder()
        .setWidth(expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(
            Text.Builder(context, if (courses.isEmpty()) "今天" else "今天课表")
                .setColor(argb(TEXT_PRIMARY))
                .setTypography(Typography.TYPOGRAPHY_TITLE3)
                .build()
        )
        .addContent(spacer(7f))

    if (courses.isEmpty()) {
        column.addContent(
            capsule(
                context = context,
                title = "今天没有课程",
                subtitle = "打开手机同步最新课表",
                accent = PRIMARY,
                clickId = "tile_empty",
            )
        )
    } else {
        val visibleCourses = courses.take(2)
        visibleCourses.forEachIndexed { index, course ->
            column.addContent(
                capsule(
                    context = context,
                    title = course.name.ifBlank { "未命名课程" },
                    subtitle = buildString {
                        append(course.start)
                        if (course.end.isNotBlank()) append("–${course.end}")
                        course.location?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    },
                    accent = course.color ?: if (index == 0) PRIMARY else AI_PURPLE,
                    clickId = "tile_course_$index",
                    compact = visibleCourses.size > 1,
                )
            )
            if (index < visibleCourses.lastIndex) column.addContent(spacer(3f))
        }
    }

    return PrimaryLayout.Builder(requestParams.deviceConfiguration)
        .setResponsiveContentInsetEnabled(true)
        .setContent(column.build())
        .build()
}

private fun capsule(
    context: Context,
    title: String,
    subtitle: String,
    accent: Int,
    clickId: String,
    compact: Boolean = false,
): LayoutElementBuilders.LayoutElement {
    val corner = ModifiersBuilders.Corner.Builder()
        .setRadius(dp(if (compact) 18f else 22f))
        .build()
    val background = ModifiersBuilders.Background.Builder()
        .setColor(argb(SURFACE))
        .setCorner(corner)
        .build()
    val padding = ModifiersBuilders.Padding.Builder()
        .setStart(dp(12f))
        .setEnd(dp(12f))
        .setTop(dp(if (compact) 6f else 9f))
        .setBottom(dp(if (compact) 6f else 9f))
        .build()

    val content = LayoutElementBuilders.Column.Builder()
        .setWidth(expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
        .addContent(
            Text.Builder(context, "●  $title")
                .setColor(argb(accent))
                .setTypography(Typography.TYPOGRAPHY_TITLE3)
                .setMaxLines(1)
                .build()
        )
        .addContent(spacer(2f))
        .addContent(
            Text.Builder(context, subtitle)
                .setColor(argb(TEXT_SECONDARY))
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setMaxLines(if (compact) 1 else 2)
                .build()
        )
        .build()

    val launchApp = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName("com.hufeng943.timetable")
                .setClassName("com.hufeng943.timetable.presentation.MainActivity")
                .build()
        )
        .build()
    val clickable = ModifiersBuilders.Clickable.Builder()
        .setId(clickId)
        .setOnClick(launchApp)
        .build()

    return LayoutElementBuilders.Box.Builder()
        .setWidth(expand())
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setBackground(background)
                .setPadding(padding)
                .setClickable(clickable)
                .build()
        )
        .addContent(content)
        .build()
}

private fun spacer(heightDp: Float): LayoutElementBuilders.LayoutElement =
    LayoutElementBuilders.Spacer.Builder().setHeight(dp(heightDp)).build()

private fun List<Timetable>.coursesForDate(date: LocalDate, is24Hour: Boolean): List<TileCourse> = flatMap { table ->
    table.resolveDate(date).map { occurrence ->
        val start = occurrence.startTime
        val end = occurrence.endTime
        val javaDate = java.time.LocalDate.parse(date.toString())
        val zone = ZoneId.systemDefault()
        val startEpoch = javaDate.atTime(start.hour, start.minute).atZone(zone).toInstant().toEpochMilli()
        val endDate = if (end <= start) javaDate.plusDays(1) else javaDate
        val endEpoch = endDate.atTime(end.hour, end.minute).atZone(zone).toInstant().toEpochMilli()
        TileCourse(
            name = occurrence.course.name,
            start = start.toDisplayString(is24Hour),
            end = end.toDisplayString(is24Hour),
            startMinutes = start.hour * 60 + start.minute,
            endMinutes = end.hour * 60 + end.minute,
            location = occurrence.location,
            teacher = occurrence.course.teacher,
            color = occurrence.course.color.takeIf { it != -1L }?.toInt()
                ?: table.color.takeIf { it != -1L }?.toInt(),
            startEpochMillis = startEpoch,
            endEpochMillis = endEpoch,
        )
    }
}.sortedBy { it.startEpochMillis }

private fun List<TileCourse>.currentAndUpcoming(): List<TileCourse> {
    if (isEmpty()) return emptyList()
    val localNow = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    val nowMinutes = localNow.hour * 60 + localNow.minute
    val activeOrUpcoming = mapNotNull { course ->
        val current = isWithinSlot(nowMinutes, course.startMinutes, course.endMinutes)
        val startsLaterToday = course.startMinutes - nowMinutes
        when {
            current -> course to 0
            startsLaterToday > 0 -> course to startsLaterToday
            else -> null
        }
    }.sortedBy { it.second }.map { it.first }
    return (if (activeOrUpcoming.isNotEmpty()) activeOrUpcoming else this).take(3)
}

private fun isWithinSlot(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
    if (startMinutes == Int.MAX_VALUE || endMinutes == Int.MAX_VALUE) return false
    return if (startMinutes <= endMinutes) {
        nowMinutes in startMinutes until endMinutes
    } else {
        nowMinutes >= startMinutes || nowMinutes < endMinutes
    }
}

private fun minutesUntil(nowMinutes: Int, targetMinutes: Int): Int =
    if (targetMinutes >= nowMinutes) targetMinutes - nowMinutes else (24 * 60 - nowMinutes) + targetMinutes

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData({ _: RequestBuilders.ResourcesRequest -> resources() }) {
    tile(
        it,
        context,
        listOf(
            TileCourse("高等数学", "08:00", "09:40", 480, 580, "A301", "张老师", PRIMARY),
            TileCourse("大学英语", "10:20", "12:00", 620, 720, "B205", null, AI_PURPLE),
        )
    )
}
