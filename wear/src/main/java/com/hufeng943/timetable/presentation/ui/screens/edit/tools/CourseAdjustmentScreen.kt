package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.material.icons.rounded.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.hufeng943.timetable.presentation.ui.NavRoutes
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.common.ui.mappers.toCourseUi
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.WearDatePickerPage
import com.hufeng943.timetable.presentation.ui.components.edit.EditCourseCard
import com.hufeng943.timetable.presentation.ui.components.edit.EditTimeSlotCard
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.CourseAdjustmentEditorState
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.CourseAdjustmentMode
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleAdjustmentState
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleAdjustmentViewModel
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.ResolvedSchedule
import com.hufeng943.timetable.shared.model.Timetable
import com.hufeng943.timetable.shared.model.resolveDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.format.TextStyle

private data class AdjustmentContext(
    val allOccurrences: List<Triple<Long, String, ResolvedSchedule>>,
    val selected: Triple<Long, String, ResolvedSchedule>?,
    val table: Timetable?,
    val a: ResolvedSchedule?,
    val bCourses: List<Course>,
    val bTableId: Long,
    val b: Course?,
    val bOccurs: Boolean,
) {
    val valid: Boolean
        get() = a != null && b != null && table != null
}

private fun buildAdjustmentContext(
    timetables: List<Timetable>,
    editor: CourseAdjustmentEditorState,
): AdjustmentContext {
    val all = timetables.flatMap { table ->
        table.resolveDate(editor.date).map { occurrence ->
            Triple(table.timetableId, table.semesterName, occurrence)
        }
    }
    val selected = all.firstOrNull { (tableId, _, occurrence) ->
        tableId == editor.sourceTableId && occurrence.timeSlot.id == editor.sourceSlotId
    } ?: all.firstOrNull()
    val table = selected?.let { hit -> timetables.firstOrNull { it.timetableId == hit.first } }
    val a = selected?.third
    val tableOccurrences = table?.resolveDate(editor.date).orEmpty()
    val bCourses = timetables.flatMap { it.allCourses }.filter { it.id != a?.course?.id }
    val bTableId = editor.targetTableId.takeIf { id -> timetables.any { it.timetableId == id } }
        ?: bCourses.firstOrNull { it.id == editor.targetCourseId }?.let { course ->
            timetables.firstOrNull { table -> table.allCourses.any { it.id == course.id } }?.timetableId
        } ?: -1L
    val b = bCourses.firstOrNull { it.id == editor.targetCourseId } ?: bCourses.firstOrNull()
    val bOccurs = b?.let { course ->
        timetables.firstOrNull { it.timetableId == bTableId }?.resolveDate(editor.date)?.any { it.course.id == course.id } == true
    } == true
    return AdjustmentContext(all, selected, table, a, bCourses, bTableId, b, bOccurs)
}

@Composable
private fun CourseAdjustmentCompletionEffect(viewModel: ScheduleAdjustmentViewModel) {
    val nav = LocalNavController.current
    LaunchedEffect(viewModel) {
        viewModel.completed.collect {
            nav.popBackStack(NavRoutes.MORE_COURSE_ADJUSTMENT, inclusive = true)
        }
    }
}

@Composable
fun CourseAdjustmentScreen(viewModel: ScheduleAdjustmentViewModel) {
    CourseAdjustmentCompletionEffect(viewModel)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editor by viewModel.courseEditor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current

    when (val current = state) {
        ScheduleAdjustmentState.Loading -> ScreenScaffold(timeText = {}) {}
        is ScheduleAdjustmentState.Error -> SimpleMessageScreen("课程调节", current.message)
        is ScheduleAdjustmentState.Ready -> {
            val ctx = buildAdjustmentContext(current.timetables, editor)
            val canApply = ctx.valid &&
                (editor.mode != CourseAdjustmentMode.SWAP || ctx.bOccurs)
            val scroll = rememberTransformingLazyColumnState()
            val transform = rememberTransformationSpec()
            ScreenScaffold(
                scrollState = scroll,
                timeText = {},
                edgeButton = {
                    EdgeButton(
                        enabled = canApply,
                        onClick = {
                            val table = ctx.table
                            val a = ctx.a
                            val b = ctx.b
                            if (table != null && a != null && b != null) {
                                viewModel.applyCourseAdjustment(
                                    timetableId = table.timetableId,
                                    targetDate = editor.date,
                                    sourceSlotId = a.timeSlot.id,
                                    targetTableId = ctx.bTableId,
                                    targetCourseId = b.id,
                                    mode = editor.mode,
                                    permanent = editor.permanent,
                                )
                            }
                        },
                    ) { Icon(Icons.Rounded.Check, "确认") }
                },
            ) { padding ->
                TransformingLazyColumn(
                    state = scroll,
                    contentPadding = padding,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        ListHeader(
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                            transformation = SurfaceTransformation(transform),
                        ) { Text("课程调节") }
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "范围：全部课表",
                            subtitle = "${current.timetables.size} 个课表 · 全部课程/课时",
                            icon = Icons.Rounded.SelectAll,
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "日期：${editor.date.toDisplayString()}",
                            subtitle = "${editor.date.dayOfWeek.toDisplayString(TextStyle.FULL)} · 点按选日期",
                            icon = Icons.Rounded.DateRange,
                            onClick = { nav.navigateSingle(NavRoutes.MORE_COURSE_ADJUSTMENT_DATE) },
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "A 课：${ctx.a?.course?.name ?: "当天无课"}",
                            subtitle = ctx.a?.let {
                                "${ctx.selected?.second.orEmpty()} · ${it.startTime.toDisplayString(true)}–${it.endTime.toDisplayString(true)}"
                            } ?: "所有课表当天均无课",
                            onClick = { nav.navigateSingle(NavRoutes.MORE_COURSE_ADJUSTMENT_A_COURSES) },
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "B 课：${ctx.b?.name ?: "无可选课程"}",
                            subtitle = "可选择全部课表课程",
                            onClick = { nav.navigateSingle(NavRoutes.MORE_COURSE_ADJUSTMENT_B_COURSES) },
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = if (editor.mode == CourseAdjustmentMode.SWAP) "换课" else "占课",
                            subtitle = if (editor.mode == CourseAdjustmentMode.SWAP) {
                                if (ctx.bOccurs) "A、B 互换" else "B 当天无课时，不能换课"
                            } else {
                                "B 使用 A 的时间，A 当天取消"
                            },
                            icon = Icons.Rounded.SwapCalls,
                            selected = editor.mode == CourseAdjustmentMode.SWAP,
                            onClick = viewModel::toggleCourseAdjustmentMode,
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = if (editor.permanent) "永久" else "仅当天",
                            subtitle = if (editor.permanent) "同步修改实际课时归属" else "只修改所选日期",
                            icon = if (editor.permanent) Icons.Rounded.Repeat else Icons.Rounded.Today,
                            selected = editor.permanent,
                            onClick = viewModel::toggleCourseAdjustmentPermanent,
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "恢复课程调节",
                            subtitle = "恢复调休与课程调节",
                            icon = Icons.Rounded.Restore,
                            onClick = { viewModel.restoreAdjustments() },
                            modifier = Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseAdjustmentDateScreen(viewModel: ScheduleAdjustmentViewModel) {
    val editor by viewModel.courseEditor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    WearDatePickerPage(
        initialDate = editor.date.toJavaLocalDate(),
        onDatePicked = {
            viewModel.updateCourseAdjustmentDate(it.toKotlinLocalDate())
            nav.popSafe()
        },
    )
}

@Composable
fun CourseAdjustmentACoursesScreen(viewModel: ScheduleAdjustmentViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editor by viewModel.courseEditor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    when (val current = state) {
        ScheduleAdjustmentState.Loading -> ScreenScaffold(timeText = {}) {}
        is ScheduleAdjustmentState.Error -> SimpleMessageScreen("选择 A 课", current.message)
        is ScheduleAdjustmentState.Ready -> {
            val courses = buildAdjustmentContext(current.timetables, editor)
                .allOccurrences.map { it.third.course }.distinctBy { it.id }
            CourseSelectionPage("选择 A 课", courses) {
                viewModel.browseCourse(it)
                nav.navigateSingle(NavRoutes.MORE_COURSE_ADJUSTMENT_A_SLOTS)
            }
        }
    }
}

@Composable
fun CourseAdjustmentASlotsScreen(viewModel: ScheduleAdjustmentViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editor by viewModel.courseEditor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    when (val current = state) {
        ScheduleAdjustmentState.Loading -> ScreenScaffold(timeText = {}) {}
        is ScheduleAdjustmentState.Error -> SimpleMessageScreen("选择 A 课时", current.message)
        is ScheduleAdjustmentState.Ready -> {
            val slots = buildAdjustmentContext(current.timetables, editor).allOccurrences
                .filter { it.third.course.id == editor.browsingCourseId }
            GlobalOccurrenceSelectionPage("选择 A 课时", slots) { tableId, slotId ->
                viewModel.selectSourceOccurrence(tableId, slotId)
                nav.popBackStack(NavRoutes.MORE_COURSE_ADJUSTMENT_MAIN, inclusive = false)
            }
        }
    }
}

@Composable
fun CourseAdjustmentBCoursesScreen(viewModel: ScheduleAdjustmentViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editor by viewModel.courseEditor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    when (val current = state) {
        ScheduleAdjustmentState.Loading -> ScreenScaffold(timeText = {}) {}
        is ScheduleAdjustmentState.Error -> SimpleMessageScreen("选择 B 课", current.message)
        is ScheduleAdjustmentState.Ready -> {
            val courses = buildAdjustmentContext(current.timetables, editor).bCourses
            CourseSelectionPage("选择 B 课", courses) {
                viewModel.browseCourse(it)
                nav.navigateSingle(NavRoutes.MORE_COURSE_ADJUSTMENT_B_SLOTS)
            }
        }
    }
}

@Composable
fun CourseAdjustmentBSlotsScreen(viewModel: ScheduleAdjustmentViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editor by viewModel.courseEditor.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    when (val current = state) {
        ScheduleAdjustmentState.Loading -> ScreenScaffold(timeText = {}) {}
        is ScheduleAdjustmentState.Error -> SimpleMessageScreen("选择 B 课时", current.message)
        is ScheduleAdjustmentState.Ready -> {
            val ctx = buildAdjustmentContext(current.timetables, editor)
            val course = ctx.bCourses.firstOrNull { it.id == editor.browsingCourseId }
            TimeSlotSelectionPage("选择 B 课时", course) {
                if (course != null) {
                    val tableId = current.timetables.firstOrNull { table -> table.allCourses.any { it.id == course.id } }?.timetableId ?: -1L
                    viewModel.selectTargetCourse(tableId, course.id)
                }
                nav.popBackStack(NavRoutes.MORE_COURSE_ADJUSTMENT_MAIN, inclusive = false)
            }
        }
    }
}

@Composable
private fun GlobalOccurrenceSelectionPage(
    title: String,
    items: List<Triple<Long, String, ResolvedSchedule>>,
    onSelect: (Long, Long) -> Unit,
) {
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()
    ScreenScaffold(scrollState = state, timeText = {}) { padding ->
        TransformingLazyColumn(state = state, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text(title) }
            }
            items(items, key = { "${it.first}:${it.third.timeSlot.id}" }) { hit ->
                OneUiCapsuleSurface(
                    title = hit.third.course.name,
                    subtitle = "${hit.second} · ${hit.third.startTime.toDisplayString(true)}–${hit.third.endTime.toDisplayString(true)}",
                    onClick = { onSelect(hit.first, hit.third.timeSlot.id) },
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}

@Composable
private fun CourseSelectionPage(title: String, courses: List<Course>, onCourse: (Long) -> Unit) {
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()
    ScreenScaffold(scrollState = state, timeText = {}) { padding ->
        TransformingLazyColumn(state = state, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text(title) }
            }
            items(courses, key = { it.id }) { course ->
                EditCourseCard(
                    course = course.toCourseUi(),
                    onClick = { onCourse(course.id) },
                    onLongClick = { onCourse(course.id) },
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transform),
                )
            }
        }
    }
}

@Composable
private fun TimeSlotSelectionPage(title: String, course: Course?, onSelect: () -> Unit) {
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()
    ScreenScaffold(scrollState = state, timeText = {}) { padding ->
        TransformingLazyColumn(state = state, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text(title) }
            }
            course?.toCourseUi()?.timeSlots?.let { slots ->
                items(slots, key = { it.id }) { slot ->
                    EditTimeSlotCard(
                        timeSlot = slot,
                        onClick = onSelect,
                        modifier = Modifier.fillMaxWidth()
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        transformation = SurfaceTransformation(transform),
                    )
                }
            }
        }
    }
}
