package com.hufeng943.timetable.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.CardDefaults.filledVariantCardColors
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.titleCard
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.hufeng943.timetable.R
import com.hufeng943.timetable.data.PreferenceStorage
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.model.NextCourseEngine
import com.hufeng943.timetable.shared.model.NextCourseOccurrence
import com.hufeng943.timetable.shared.model.Timetable
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import java.time.ZoneId
import kotlin.time.Clock

private data class TileCourse(
    val name: String,
    val start: String,
    val end: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val location: String?,
    val teacher: String?,
    val startEpochMillis: Long = 0L,
    val endEpochMillis: Long = 0L,
    val position: Int = 0,
    val total: Int = 0,
)

/**
 * Responsive Material 3 tile. Material3TileService owns resource registration and dynamic color,
 * while primaryLayout applies the official round/square safe margins so content is not cropped on
 * smaller watches and larger devices such as Xiaomi Watch 5.
 */
@AndroidEntryPoint
class MainTileService : Material3TileService() {

    @Inject lateinit var repository: TimetableRepository
    @Inject lateinit var preferenceStorage: PreferenceStorage

    override suspend fun MaterialScope.tileResponse(
        requestParams: RequestBuilders.TileRequest,
    ): TileBuilders.Tile {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date
        val is24Hour = runCatching {
            preferenceStorage.appConfigFlow.first().is24HourFormat
        }.getOrDefault(true)
        val tables = runCatching { repository.getAllTimetables().first() }.getOrDefault(emptyList())
        val courses = tables.coursesForDate(today, is24Hour, timeZone)
        val state = NextCourseEngine.resolve(tables, now, timeZone)
        val initialVisible = listOfNotNull(
            state.current?.toTileCourse(is24Hour),
            state.next?.takeIf { it.date == today }?.toTileCourse(is24Hour),
        ).distinctBy { it.startEpochMillis to it.name }.take(2)

        return buildTile(courses, initialVisible)
    }

    private fun MaterialScope.buildTile(
        courses: List<TileCourse>,
        initialVisible: List<TileCourse>,
    ): TileBuilders.Tile {
        val timeline = TimelineBuilders.Timeline.Builder()
        timeline.addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder()
                .setLayout(
                    LayoutElementBuilders.Layout.Builder()
                        .setRoot(tileLayout(initialVisible, courses.isNotEmpty()))
                        .build()
                )
                .build()
        )

        val timelineReady = courses.isNotEmpty() && courses.all {
            it.startEpochMillis > 0L && it.endEpochMillis > it.startEpochMillis
        }
        if (timelineReady) {
            val zone = ZoneId.systemDefault()
            val javaToday = java.time.LocalDate.now(zone)
            val dayStart = javaToday.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = javaToday.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            var cursor = dayStart

            courses.forEachIndexed { index, course ->
                if (cursor < course.startEpochMillis) {
                    addTimelineEntry(
                        timeline = timeline,
                        start = cursor,
                        end = course.startEpochMillis,
                        visibleCourses = courses.drop(index).take(2),
                        hadCoursesToday = true,
                    )
                }
                addTimelineEntry(
                    timeline = timeline,
                    start = course.startEpochMillis,
                    end = course.endEpochMillis,
                    visibleCourses = listOf(course) + courses.drop(index + 1).take(1),
                    hadCoursesToday = true,
                )
                cursor = maxOf(cursor, course.endEpochMillis)
            }
            if (cursor < dayEnd) {
                addTimelineEntry(
                    timeline = timeline,
                    start = cursor,
                    end = dayEnd,
                    visibleCourses = emptyList(),
                    hadCoursesToday = true,
                )
            }
        }

        return TileBuilders.Tile.Builder()
            .setTileTimeline(timeline.build())
            .setFreshnessIntervalMillis(30 * 60 * 1000L)
            .build()
    }

    private fun MaterialScope.addTimelineEntry(
        timeline: TimelineBuilders.Timeline.Builder,
        start: Long,
        end: Long,
        visibleCourses: List<TileCourse>,
        hadCoursesToday: Boolean,
    ) {
        timeline.addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder()
                .setLayout(
                    LayoutElementBuilders.Layout.Builder()
                        .setRoot(tileLayout(visibleCourses, hadCoursesToday))
                        .build()
                )
                .setValidity(
                    TimelineBuilders.TimeInterval.Builder()
                        .setStartMillis(start)
                        .setEndMillis(end)
                        .build()
                )
                .build()
        )
    }

    private fun MaterialScope.tileLayout(
        courses: List<TileCourse>,
        hadCoursesToday: Boolean,
    ): LayoutElementBuilders.LayoutElement {
        val nowMillis = System.currentTimeMillis()
        val course = courses.firstOrNull()
        val next = courses.getOrNull(1)
        val isCurrent = course?.let {
            it.startEpochMillis > 0L && nowMillis in it.startEpochMillis until it.endEpochMillis
        } == true

        val status = when {
            course == null && hadCoursesToday -> getString(R.string.tile_status_finished)
            course == null -> getString(R.string.tile_status_today)
            isCurrent -> buildString {
                append(getString(R.string.tile_status_current))
                val left = ((course.endEpochMillis - nowMillis) / 60_000L).coerceAtLeast(1L)
                append(" · 余${left}分")
            }
            else -> buildString {
                append(getString(R.string.tile_status_next))
                val wait = ((course.startEpochMillis - nowMillis) / 60_000L).coerceAtLeast(0L)
                append(" · ${wait}分后")
            }
        }

        val title = course?.name?.ifBlank { getString(R.string.tile_unnamed_course) }
            ?: if (hadCoursesToday) getString(R.string.home_day_finished_free_title)
            else getString(R.string.tile_no_courses)

        val detail = course?.let {
            buildString {
                append(it.start)
                if (it.end.isNotBlank()) append("–${it.end}")
                it.location?.takeIf { value -> value.isNotBlank() }?.let { location -> append(" · $location") }
                it.teacher?.takeIf { value -> value.isNotBlank() }?.let { teacher -> append("\n$teacher") }
                if (it.position > 0) {
                    append(if (it.teacher.isNullOrBlank()) "\n" else " · ")
                    append("第${it.position}/${it.total}节")
                }
            }
        } ?: if (hadCoursesToday) getString(R.string.home_day_finished_title)
        else getString(R.string.tile_tap_to_open)

        val bottom = next?.let { "下一节 ${it.start} · ${it.name}" }
            ?: getString(R.string.tile_tap_to_open)
        val clickable = launchAppClickable("open_timetable")
        val timeSlot: (MaterialScope.() -> LayoutElementBuilders.LayoutElement)? = course?.let { selected ->
            { text(selected.start.layoutString) }
        }

        return primaryLayout(
            titleSlot = { text(status.layoutString) },
            mainSlot = {
                titleCard(
                    onClick = clickable,
                    modifier = LayoutModifier.contentDescription("$title，$detail"),
                    height = expand(),
                    colors = filledVariantCardColors(),
                    title = { text(title.layoutString) },
                    time = timeSlot,
                    content = { text(detail.layoutString) },
                )
            },
            bottomSlot = { text(bottom.layoutString) },
            onClick = clickable,
        )
    }

    private fun launchAppClickable(id: String): ModifiersBuilders.Clickable {
        val action = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName("com.hufeng943.timetable.presentation.MainActivity")
                    .build()
            )
            .build()
        return ModifiersBuilders.Clickable.Builder()
            .setId(id)
            .setOnClick(action)
            .build()
    }
}

private fun List<Timetable>.coursesForDate(
    date: LocalDate,
    is24Hour: Boolean,
    timeZone: TimeZone,
): List<TileCourse> = NextCourseEngine.occurrencesForDate(this, date, timeZone)
    .map { it.toTileCourse(is24Hour) }
    .let { sorted ->
        sorted.mapIndexed { index, course -> course.copy(position = index + 1, total = sorted.size) }
    }

private fun NextCourseOccurrence.toTileCourse(is24Hour: Boolean): TileCourse = TileCourse(
    name = courseName,
    start = startTime.toDisplayString(is24Hour),
    end = endTime.toDisplayString(is24Hour),
    startMinutes = startTime.hour * 60 + startTime.minute,
    endMinutes = endTime.hour * 60 + endTime.minute,
    location = location,
    teacher = teacher,
    startEpochMillis = startInstant.toEpochMilliseconds(),
    endEpochMillis = endInstant.toEpochMilliseconds(),
)
