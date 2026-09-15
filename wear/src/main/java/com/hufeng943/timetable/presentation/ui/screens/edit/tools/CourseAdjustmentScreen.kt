package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.material.icons.rounded.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.CourseAdjustmentMode
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleAdjustmentState
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.ScheduleAdjustmentViewModel
import com.hufeng943.timetable.shared.model.resolveDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.todayIn
import kotlin.time.Clock

enum class CourseAdjustmentPage { MAIN, DATE }

@Composable
fun CourseAdjustmentScreen(viewModel: ScheduleAdjustmentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNavController.current
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.completed.collect { nav.popSafe() }
    }

    when (val current = state) {
        ScheduleAdjustmentState.Loading -> ScreenScaffold { }
        is ScheduleAdjustmentState.Error -> SimpleMessageScreen("课程调节", current.message)
        is ScheduleAdjustmentState.Ready -> {
            val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
            var tableIndex by remember { mutableIntStateOf(0) }
            var date by remember { mutableStateOf(today) }
            var aIndex by remember { mutableIntStateOf(0) }
            var bIndex by remember { mutableIntStateOf(0) }
            var mode by remember { mutableStateOf(CourseAdjustmentMode.OCCUPY) }
            var permanent by remember { mutableStateOf(false) }
            var page by remember { mutableStateOf(CourseAdjustmentPage.MAIN) }
            BackHandler(enabled = page != CourseAdjustmentPage.MAIN) { page = CourseAdjustmentPage.MAIN }

            if (page == CourseAdjustmentPage.DATE) {
                ScreenScaffold(timeText = {}) {
                    DatePicker(
                        initialDate = date.toJavaLocalDate(),
                        onDatePicked = {
                            date = it.toKotlinLocalDate()
                            aIndex = 0
                            bIndex = 0
                            page = CourseAdjustmentPage.MAIN
                        },
                    )
                }
                return
            }

            val table = current.timetables[tableIndex.coerceIn(0, current.timetables.lastIndex)]
            val occurrences = remember(table, date) { table.resolveDate(date) }
            val a = occurrences.getOrNull(aIndex.coerceIn(0, (occurrences.size - 1).coerceAtLeast(0)))
            val bCourses = remember(table, a?.course?.id) { table.allCourses.filter { it.id != a?.course?.id } }
            val bCourse = bCourses.getOrNull(bIndex.coerceIn(0, (bCourses.size - 1).coerceAtLeast(0)))
            val bOccursToday = bCourse?.let { course -> occurrences.any { it.course.id == course.id } } == true
            val valid = a != null && bCourse != null && (mode != CourseAdjustmentMode.SWAP || bOccursToday)
            val scroll = rememberTransformingLazyColumnState()
            val transform = rememberTransformationSpec()

            ScreenScaffold(
                scrollState = scroll,
                edgeButton = {
                    EdgeButton(
                        enabled = valid,
                        onClick = {
                            val source = a
                            val target = bCourse
                            if (source != null && target != null) {
                                viewModel.applyCourseAdjustment(
                                    timetableId = table.timetableId,
                                    targetDate = date,
                                    sourceSlotId = source.timeSlot.id,
                                    targetCourseId = target.id,
                                    mode = mode,
                                    permanent = permanent,
                                )
                            }
                        },
                    ) { Icon(Icons.Rounded.Check, contentDescription = "应用课程调节") }
                },
            ) { padding ->
                TransformingLazyColumn(
                    state = scroll,
                    flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(scroll),
                    rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scroll),
                    contentPadding = padding,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        ListHeader(
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                            transformation = SurfaceTransformation(transform),
                        ) { Text("课程调节") }
                    }
                    if (current.timetables.size > 1) item {
                        OneUiCapsuleSurface(
                            title = "课表：${table.semesterName}",
                            subtitle = "点按切换课表",
                            onClick = {
                                tableIndex = (tableIndex + 1) % current.timetables.size
                                aIndex = 0; bIndex = 0
                            },
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "日期：${date.toDisplayString()}",
                            subtitle = "点按选择要调整的日期",
                            icon = Icons.Rounded.DateRange,
                            onClick = { page = CourseAdjustmentPage.DATE },
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "A 课：${a?.course?.name ?: "当天无课"}",
                            subtitle = a?.let { "${it.startTime.toDisplayString(true)}–${it.endTime.toDisplayString(true)} · 点按切换" } ?: "请换一个日期",
                            onClick = { if (occurrences.isNotEmpty()) aIndex = (aIndex + 1) % occurrences.size },
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = "B 课：${bCourse?.name ?: "无可选课程"}",
                            subtitle = if (bCourses.isEmpty()) "需要至少两门课程" else "点按切换要上的课程",
                            emphasize = bCourse != null,
                            onClick = { if (bCourses.isNotEmpty()) bIndex = (bIndex + 1) % bCourses.size },
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = if (mode == CourseAdjustmentMode.SWAP) "换课" else "占课",
                            subtitle = if (mode == CourseAdjustmentMode.SWAP) {
                                if (bOccursToday) "A、B 当天互换时间 · 点按改为占课" else "B 当天没有课时，无法换课 · 点按改为占课"
                            } else "B 使用 A 的时间，A 当天取消 · 点按改为换课",
                            icon = Icons.Rounded.SwapCalls,
                            selected = mode == CourseAdjustmentMode.SWAP,
                            onClick = { mode = if (mode == CourseAdjustmentMode.SWAP) CourseAdjustmentMode.OCCUPY else CourseAdjustmentMode.SWAP },
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                    item {
                        OneUiCapsuleSurface(
                            title = if (permanent) "永久" else "仅今天",
                            subtitle = if (permanent) "同步修改课时归属，之后课程表与编辑页一致" else "只为 ${date.toDisplayString()} 写入日期例外",
                            icon = if (permanent) Icons.Rounded.Repeat else Icons.Rounded.Today,
                            selected = permanent,
                            onClick = { permanent = !permanent },
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        )
                    }
                }
            }
        }
    }
}
