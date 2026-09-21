package com.hufeng943.timetable.presentation.viewmodel.edit.tools

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.data.repository.TimeSlotMutation
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.ResolvedSchedule
import com.hufeng943.timetable.shared.model.ScheduleBatchOperations
import com.hufeng943.timetable.shared.model.ScheduleOverride
import com.hufeng943.timetable.shared.model.ScheduleOverrideType
import com.hufeng943.timetable.shared.model.TimeSlot
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.WeekPattern
import com.hufeng943.timetable.shared.model.resolveDate
import com.hufeng943.timetable.surface.WearSurfaceRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class CourseAdjustmentMode { SWAP, OCCUPY }

data class DayArrangementEditorState(
    val date: LocalDate,
    val sourceDay: DayOfWeek,
)

data class CourseAdjustmentEditorState(
    val date: LocalDate,
    val sourceSlotId: Long = -1L,
    val sourceTableId: Long = -1L,
    val targetCourseId: Long = -1L,
    val browsingCourseId: Long = -1L,
    val mode: CourseAdjustmentMode = CourseAdjustmentMode.OCCUPY,
    val permanent: Boolean = false,
)

sealed interface ScheduleAdjustmentState {
    data object Loading : ScheduleAdjustmentState
    data class Ready(val timetables: List<Timetable>) : ScheduleAdjustmentState
    data class Error(val message: String) : ScheduleAdjustmentState
}

@HiltViewModel
class ScheduleAdjustmentViewModel @Inject constructor(
    private val repository: TimetableRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _state = MutableStateFlow<ScheduleAdjustmentState>(ScheduleAdjustmentState.Loading)
    val state: StateFlow<ScheduleAdjustmentState> = _state.asStateFlow()

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val _dayEditor = MutableStateFlow(
        DayArrangementEditorState(
            date = today,
            sourceDay = DayOfWeek.entries[(today.dayOfWeek.ordinal + 1) % DayOfWeek.entries.size],
        ),
    )
    val dayEditor: StateFlow<DayArrangementEditorState> = _dayEditor.asStateFlow()

    private val _courseEditor = MutableStateFlow(CourseAdjustmentEditorState(date = today))
    val courseEditor: StateFlow<CourseAdjustmentEditorState> = _courseEditor.asStateFlow()

    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed = _completed.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getAllTimetables().collect { tables ->
                _state.value = if (tables.isEmpty()) ScheduleAdjustmentState.Error("暂无课表")
                else ScheduleAdjustmentState.Ready(tables)
            }
        }
    }

    fun updateDayArrangementDate(date: LocalDate) {
        val current = _dayEditor.value
        val source = if (current.sourceDay == date.dayOfWeek) {
            DayOfWeek.entries[(current.sourceDay.ordinal + 1) % DayOfWeek.entries.size]
        } else current.sourceDay
        _dayEditor.value = current.copy(date = date, sourceDay = source)
    }

    fun updateDayArrangementSource(day: DayOfWeek) {
        if (day != _dayEditor.value.date.dayOfWeek) _dayEditor.value = _dayEditor.value.copy(sourceDay = day)
    }

    fun updateCourseAdjustmentDate(date: LocalDate) {
        _courseEditor.value = _courseEditor.value.copy(
            date = date,
            sourceSlotId = -1L,
            sourceTableId = -1L,
            targetCourseId = -1L,
            browsingCourseId = -1L,
        )
    }

    fun browseCourse(courseId: Long) {
        _courseEditor.value = _courseEditor.value.copy(browsingCourseId = courseId)
    }

    fun selectSourceOccurrence(tableId: Long, slotId: Long) {
        _courseEditor.value = _courseEditor.value.copy(
            sourceTableId = tableId,
            sourceSlotId = slotId,
            targetCourseId = -1L,
        )
    }

    fun selectTargetCourse(courseId: Long) {
        _courseEditor.value = _courseEditor.value.copy(targetCourseId = courseId)
    }

    fun toggleCourseAdjustmentMode() {
        val mode = _courseEditor.value.mode
        _courseEditor.value = _courseEditor.value.copy(
            mode = if (mode == CourseAdjustmentMode.SWAP) CourseAdjustmentMode.OCCUPY else CourseAdjustmentMode.SWAP,
        )
    }

    fun toggleCourseAdjustmentPermanent() {
        _courseEditor.value = _courseEditor.value.copy(permanent = !_courseEditor.value.permanent)
    }

    fun applyDayArrangement(timetableId: Long?, targetDate: LocalDate, sourceDay: DayOfWeek) {
        viewModelScope.launch {
            runCatching {
                val tables = (_state.value as? ScheduleAdjustmentState.Ready)?.timetables.orEmpty()
                    .filter { timetableId == null || it.timetableId == timetableId }
                require(tables.isNotEmpty()) { "课表不存在" }
                val dayDelta = sourceDay.isoNumber() - targetDate.dayOfWeek.isoNumber()
                val sourceDate = targetDate.plus(dayDelta, DateTimeUnit.DAY)
                require(sourceDate != targetDate) { "目标日期与来源星期相同" }

                tables.forEach { table ->
                    val target = table.resolveDate(targetDate)
                    val source = table.resolveDate(sourceDate)
                    val slotById = table.allCourses.flatMap { course -> course.timeSlots.map { it.id to (course.id to it) } }.toMap()
                    val updates = mutableMapOf<Long, Pair<Long, TimeSlot>>()

                    target.forEach { occurrence ->
                        val current = updates[occurrence.timeSlot.id]?.second ?: occurrence.timeSlot
                        updates[occurrence.timeSlot.id] = occurrence.course.id to
                            ScheduleBatchOperations.cancelDates(current, listOf(targetDate))
                    }
                    source.forEach { occurrence ->
                        val original = updates[occurrence.timeSlot.id]?.second
                            ?: slotById[occurrence.timeSlot.id]?.second
                            ?: occurrence.timeSlot
                        val extra = original.withExactOverride(
                            ScheduleOverride(
                                date = targetDate,
                                type = ScheduleOverrideType.EXTRA,
                                startTime = occurrence.startTime,
                                endTime = occurrence.endTime,
                                location = occurrence.location,
                                remark = occurrence.timeSlot.remark,
                            )
                        )
                        updates[occurrence.timeSlot.id] = occurrence.course.id to extra
                    }
                    repository.applyAdjustmentMutations(
                        timetableId = table.timetableId,
                        mutations = updates.values.map { (courseId, slot) -> TimeSlotMutation(slot, courseId) },
                    )
                }
                WearSurfaceRefresher.refresh(appContext)
                _completed.tryEmit(Unit)
            }.onFailure { _state.value = ScheduleAdjustmentState.Error(it.message ?: "调休失败") }
        }
    }

    fun restoreAdjustments(timetableId: Long? = null) {
        viewModelScope.launch {
            runCatching {
                val tables = (_state.value as? ScheduleAdjustmentState.Ready)?.timetables.orEmpty()
                    .filter { timetableId == null || it.timetableId == timetableId }
                require(tables.isNotEmpty()) { "课表不存在" }
                tables.forEach { repository.restoreAdjustmentMutations(it.timetableId) }
                // One-time compatibility cleanup for histories written by versions before Room-backed undo.
                val prefs = appContext.getSharedPreferences("schedule_adjustment_history", Context.MODE_PRIVATE)
                val courseIds = tables.flatMap { it.allCourses }.map { it.id }.toSet()
                // Replay newest -> oldest so multiple edits of the same slot unwind deterministically.
                // Legacy slot_/created_ keys are still accepted for backward compatibility.
                prefs.all.toList().sortedByDescending { it.first }.forEach { (key, raw) ->
                    val value = raw as? String ?: return@forEach
                    if (value.startsWith("DELETE|")) {
                        val parts = value.split('|')
                        val courseId = parts.getOrNull(1)?.toLongOrNull() ?: return@forEach
                        val slotId = parts.getOrNull(2)?.toLongOrNull() ?: return@forEach
                        if (courseId in courseIds) { repository.deleteTimeSlot(slotId); prefs.edit().remove(key).apply() }
                        return@forEach
                    }
                    val split = value.indexOf('|')
                    if (split <= 0) return@forEach
                    val courseId = value.substring(0, split).toLongOrNull() ?: return@forEach
                    if (courseId !in courseIds) return@forEach
                    val slot = Json.decodeFromString<TimeSlot>(value.substring(split + 1))
                    repository.upsertTimeSlot(slot, courseId)
                    prefs.edit().remove(key).apply()
                }
                WearSurfaceRefresher.refresh(appContext)
                _completed.tryEmit(Unit)
            }.onFailure { _state.value = ScheduleAdjustmentState.Error(it.message ?: "恢复失败") }
        }
    }

    fun applyCourseAdjustment(
        timetableId: Long,
        targetDate: LocalDate,
        sourceSlotId: Long,
        targetCourseId: Long,
        mode: CourseAdjustmentMode,
        permanent: Boolean,
    ) {
        viewModelScope.launch {
            runCatching {
                val table = (_state.value as? ScheduleAdjustmentState.Ready)?.timetables
                    ?.firstOrNull { it.timetableId == timetableId } ?: error("课表不存在")
                val occurrences = table.resolveDate(targetDate)
                val a = occurrences.firstOrNull { it.timeSlot.id == sourceSlotId } ?: error("A 课在当天不存在")
                val bCourse = table.allCourses.firstOrNull { it.id == targetCourseId } ?: error("B 课不存在")
                require(a.course.id != bCourse.id) { "A 课与 B 课不能相同" }
                val bOccurrence = occurrences.firstOrNull { it.course.id == bCourse.id }

                if (permanent) {
                    applyPermanent(timetableId, a, bCourse, bOccurrence, mode)
                } else {
                    applyToday(timetableId, targetDate, a, bCourse, bOccurrence, mode)
                }
                WearSurfaceRefresher.refresh(appContext)
                _completed.tryEmit(Unit)
            }.onFailure { _state.value = ScheduleAdjustmentState.Error(it.message ?: "课程调节失败") }
        }
    }

    private suspend fun applyToday(
        timetableId: Long,
        date: LocalDate,
        a: ResolvedSchedule,
        bCourse: Course,
        bOccurrence: ResolvedSchedule?,
        mode: CourseAdjustmentMode,
    ) {
        val mutations = mutableListOf<TimeSlotMutation>()
        when (mode) {
            CourseAdjustmentMode.OCCUPY -> {
                mutations += TimeSlotMutation(ScheduleBatchOperations.cancelDates(a.timeSlot, listOf(date)), a.course.id)
                val bSlot = bOccurrence?.timeSlot ?: bCourse.timeSlots.firstOrNull()
                val occupied = if (bSlot != null) {
                    bSlot.withExactOverride(ScheduleOverride(date, ScheduleOverrideType.EXTRA, a.startTime, a.endTime, bCourse.location, bSlot.remark))
                } else {
                    TimeSlot(
                        startTime = a.startTime, endTime = a.endTime, dayOfWeek = date.dayOfWeek, recurrence = WeekPattern.DATE_ONLY,
                        overrides = listOf(ScheduleOverride(date, ScheduleOverrideType.EXTRA, a.startTime, a.endTime, bCourse.location)),
                    )
                }
                mutations += TimeSlotMutation(occupied, bCourse.id)
            }
            CourseAdjustmentMode.SWAP -> {
                val b = requireNotNull(bOccurrence) { "换课要求 B 课当天也有课时" }
                mutations += TimeSlotMutation(
                    a.timeSlot.withExactOverride(ScheduleOverride(date, ScheduleOverrideType.EXTRA, b.startTime, b.endTime, a.location, a.timeSlot.remark)), a.course.id
                )
                mutations += TimeSlotMutation(
                    b.timeSlot.withExactOverride(ScheduleOverride(date, ScheduleOverrideType.EXTRA, a.startTime, a.endTime, b.location, b.timeSlot.remark)), b.course.id
                )
            }
        }
        repository.applyAdjustmentMutations(timetableId, mutations)
    }

    private suspend fun applyPermanent(
        timetableId: Long,
        a: ResolvedSchedule,
        bCourse: Course,
        bOccurrence: ResolvedSchedule?,
        mode: CourseAdjustmentMode,
    ) {
        val mutations = when (mode) {
            CourseAdjustmentMode.OCCUPY -> {
                requireNoPermanentConflict(a.timeSlot, bCourse, emptySet(), "永久占课")
                listOf(TimeSlotMutation(a.timeSlot, bCourse.id))
            }
            CourseAdjustmentMode.SWAP -> {
                val b = requireNotNull(bOccurrence) { "永久换课要求 B 课当天也有课时" }
                requireNoPermanentConflict(a.timeSlot, bCourse, setOf(b.timeSlot.id), "永久换课")
                requireNoPermanentConflict(b.timeSlot, a.course, setOf(a.timeSlot.id), "永久换课")
                listOf(TimeSlotMutation(a.timeSlot, b.course.id), TimeSlotMutation(b.timeSlot, a.course.id))
            }
        }
        repository.applyAdjustmentMutations(timetableId, mutations)
    }
    private fun requireNoPermanentConflict(
        incoming: TimeSlot,
        targetCourse: Course,
        excludingSlotIds: Set<Long>,
        actionName: String,
    ) {
        val conflict = targetCourse.timeSlots.firstOrNull { existing ->
            existing.id !in excludingSlotIds && slotsConflict(incoming, existing)
        }
        require(conflict == null) {
            "${actionName}失败：目标课程已有重叠课时，请先处理时间冲突"
        }
    }

    private fun slotsConflict(a: TimeSlot, b: TimeSlot): Boolean {
        val aDay = a.dayOfWeek ?: return false
        val bDay = b.dayOfWeek ?: return false
        if (aDay != bDay) return false
        val aStart = a.startTime ?: return false
        val aEnd = a.endTime ?: return false
        val bStart = b.startTime ?: return false
        val bEnd = b.endTime ?: return false
        if (!(aStart < bEnd && bStart < aEnd)) return false
        return recurrenceOverlaps(a, b)
    }

    private fun recurrenceOverlaps(a: TimeSlot, b: TimeSlot): Boolean {
        if (a.recurrence == WeekPattern.DATE_ONLY || b.recurrence == WeekPattern.DATE_ONLY) {
            val aDates = a.overrides.filter { it.type != ScheduleOverrideType.CANCELLED }.map { it.date }.toSet()
            val bDates = b.overrides.filter { it.type != ScheduleOverrideType.CANCELLED }.map { it.date }.toSet()
            return when {
                a.recurrence == WeekPattern.DATE_ONLY && b.recurrence == WeekPattern.DATE_ONLY -> aDates.intersect(bDates).isNotEmpty()
                // A recurring slot can occur on any matching weekday during the semester; if the
                // date-only slot has at least one date on that weekday, treat it as a real conflict.
                a.recurrence == WeekPattern.DATE_ONLY -> aDates.isNotEmpty()
                else -> bDates.isNotEmpty()
            }
        }
        return a.recurrence == WeekPattern.EVERY_WEEK ||
            b.recurrence == WeekPattern.EVERY_WEEK ||
            a.recurrence == b.recurrence
    }


}

private fun TimeSlot.withExactOverride(override: ScheduleOverride): TimeSlot =
    copy(overrides = overrides.filterNot { it.date == override.date && it.endDate == null } + override)

private fun DayOfWeek.isoNumber(): Int = ordinal + 1
