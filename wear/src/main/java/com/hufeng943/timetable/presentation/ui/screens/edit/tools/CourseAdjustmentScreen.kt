package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.*
import com.hufeng943.timetable.shared.model.resolveDate
import kotlinx.datetime.*
import java.time.format.TextStyle
import kotlin.time.Clock

enum class CourseAdjustmentPage { MAIN, TABLE, DATE, A, B }

@Composable
fun CourseAdjustmentScreen(viewModel: ScheduleAdjustmentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); val nav = LocalNavController.current
    LaunchedEffect(viewModel) { viewModel.completed.collect { nav.popSafe() } }
    when (val current = state) {
        ScheduleAdjustmentState.Loading -> ScreenScaffold(timeText = {}) { }
        is ScheduleAdjustmentState.Error -> SimpleMessageScreen("课程调节", current.message)
        is ScheduleAdjustmentState.Ready -> {
            val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
            var tableId by remember { mutableLongStateOf(current.timetables.first().timetableId) }
            var date by remember { mutableStateOf(today) }
            var sourceSlotId by remember { mutableLongStateOf(-1L) }
            var targetCourseId by remember { mutableLongStateOf(-1L) }
            var mode by remember { mutableStateOf(CourseAdjustmentMode.OCCUPY) }
            var permanent by remember { mutableStateOf(false) }
            var page by remember { mutableStateOf(CourseAdjustmentPage.MAIN) }
            BackHandler(enabled = page != CourseAdjustmentPage.MAIN) { page = CourseAdjustmentPage.MAIN }
            val table = current.timetables.firstOrNull { it.timetableId == tableId } ?: current.timetables.first()
            val occurrences = remember(table, date) { table.resolveDate(date) }
            val a = occurrences.firstOrNull { it.timeSlot.id == sourceSlotId } ?: occurrences.firstOrNull()
            if (sourceSlotId < 0 && a != null) sourceSlotId = a.timeSlot.id
            val bCourses = table.allCourses.filter { it.id != a?.course?.id }
            val b = bCourses.firstOrNull { it.id == targetCourseId } ?: bCourses.firstOrNull()
            if (targetCourseId < 0 && b != null) targetCourseId = b.id
            val bOccurs = b?.let { bc -> occurrences.any { it.course.id == bc.id } } == true
            val valid = a != null && b != null && (mode != CourseAdjustmentMode.SWAP || bOccurs)

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (targetState == CourseAdjustmentPage.MAIN) {
                        slideInHorizontally { -it / 3 } togetherWith slideOutHorizontally { it }
                    } else {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
                    }
                },
                label = "CourseAdjustmentPageTransition",
            ) { visiblePage ->
                when (visiblePage) {
                CourseAdjustmentPage.TABLE -> TimetablePickerPage(current.timetables, tableId) { tableId = it; sourceSlotId = -1; targetCourseId = -1; page = CourseAdjustmentPage.MAIN }
                CourseAdjustmentPage.DATE -> ScreenScaffold(timeText = {}) { DatePicker(initialDate = date.toJavaLocalDate(), onDatePicked = { date = it.toKotlinLocalDate(); sourceSlotId = -1; targetCourseId = -1; page = CourseAdjustmentPage.MAIN }) }
                CourseAdjustmentPage.A -> OccurrencePicker("选择 A 课", occurrences, sourceSlotId) { sourceSlotId = it; targetCourseId = -1; page = CourseAdjustmentPage.MAIN }
                CourseAdjustmentPage.B -> CoursePicker("选择 B 课", bCourses, targetCourseId) { targetCourseId = it; page = CourseAdjustmentPage.MAIN }
                CourseAdjustmentPage.MAIN -> {
                    val scroll = rememberTransformingLazyColumnState(); val transform = rememberTransformationSpec()
                    ScreenScaffold(scrollState = scroll, timeText = {}, edgeButton = {
                        EdgeButton(enabled = valid, onClick = { if (a != null && b != null) viewModel.applyCourseAdjustment(tableId, date, a.timeSlot.id, b.id, mode, permanent) }) { Icon(Icons.Rounded.Check, "确认") }
                    }) { padding ->
                        TransformingLazyColumn(state = scroll, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                            item { ListHeader(modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding), transformation = SurfaceTransformation(transform)) { Text("课程调节") } }
                            item { OneUiCapsuleSurface(title = "课表：${table.semesterName}", subtitle = "点按进入课表/课程/课时选择", onClick = { page = CourseAdjustmentPage.TABLE }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) }
                            item { OneUiCapsuleSurface(title = "日期：${date.toDisplayString()}", subtitle = "${date.dayOfWeek.toDisplayString(TextStyle.FULL)} · 点按打开日期选择", icon = Icons.Rounded.DateRange, onClick = { page = CourseAdjustmentPage.DATE }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) }
                            item { OneUiCapsuleSurface(title = "A 课：${a?.course?.name ?: "当天无课"}", subtitle = a?.let { "${it.startTime.toDisplayString(true)}–${it.endTime.toDisplayString(true)} · 点按选择课时" } ?: "请选择其他日期", onClick = { page = CourseAdjustmentPage.A }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) }
                            item { OneUiCapsuleSurface(title = "B 课：${b?.name ?: "无可选课程"}", subtitle = "点按进入课程列表；课程可展开查看课时", onClick = { page = CourseAdjustmentPage.B }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) }
                            item { OneUiCapsuleSurface(title = if (mode == CourseAdjustmentMode.SWAP) "换课" else "占课", subtitle = if (mode == CourseAdjustmentMode.SWAP) if (bOccurs) "A、B 互换" else "B 当天无课时，不能换课" else "B 使用 A 的时间，A 当天取消", icon = Icons.Rounded.SwapCalls, selected = mode == CourseAdjustmentMode.SWAP, onClick = { mode = if (mode == CourseAdjustmentMode.SWAP) CourseAdjustmentMode.OCCUPY else CourseAdjustmentMode.SWAP }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) }
                            item { OneUiCapsuleSurface(title = if (permanent) "永久" else "仅当天", subtitle = if (permanent) "同步修改实际课时归属" else "只修改所选日期", icon = if (permanent) Icons.Rounded.Repeat else Icons.Rounded.Today, selected = permanent, onClick = { permanent = !permanent }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) }
                            item { OneUiCapsuleSurface(title = "恢复课程调节", subtitle = "恢复此课表由调休、课程调节产生的修改", icon = Icons.Rounded.Restore, onClick = { viewModel.restoreAdjustments(tableId) }, modifier = Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable private fun OccurrencePicker(title: String, items: List<com.hufeng943.timetable.shared.model.ResolvedSchedule>, selected: Long, onSelect: (Long) -> Unit) {
    val state=rememberTransformingLazyColumnState(); val transform=rememberTransformationSpec()
    ScreenScaffold(scrollState=state,timeText={}) { padding -> TransformingLazyColumn(state=state,contentPadding=padding,modifier=Modifier.fillMaxSize()) {
        item { ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(title)} }
        items.forEach { o -> item(key=o.timeSlot.id) { OneUiCapsuleSurface(title=o.course.name,subtitle="${o.startTime.toDisplayString(true)}–${o.endTime.toDisplayString(true)}",selected=o.timeSlot.id==selected,onClick={onSelect(o.timeSlot.id)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) } }
    } }
}

@Composable private fun CoursePicker(title: String, courses: List<com.hufeng943.timetable.shared.model.Course>, selected: Long, onSelect: (Long) -> Unit) {
    val state=rememberTransformingLazyColumnState(); val transform=rememberTransformationSpec(); var expanded by remember{mutableLongStateOf(-1L)}
    ScreenScaffold(scrollState=state,timeText={}) { padding -> TransformingLazyColumn(state=state,contentPadding=padding,modifier=Modifier.fillMaxSize()) {
        item { ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(title)} }
        courses.forEach { c -> item(key=c.id) { OneUiCapsuleSurface(title=c.name,subtitle=if(expanded==c.id)"已展开 · 点按选中课程" else "${c.timeSlots.size} 个课时 · 点按展开，长按选中",selected=c.id==selected,onClick={expanded=if(expanded==c.id)-1 else c.id},onLongClick={onSelect(c.id)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)) }; if(expanded==c.id)c.timeSlots.forEach{slot->item(key=slot.id){OneUiCapsuleSurface(title="${slot.dayOfWeek?.name ?: "未设星期"} ${slot.startTime?.toDisplayString(true) ?: "--:--"}–${slot.endTime?.toDisplayString(true) ?: "--:--"}",subtitle="点按选择 ${c.name}",onClick={onSelect(c.id)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}} }
    } }
}
