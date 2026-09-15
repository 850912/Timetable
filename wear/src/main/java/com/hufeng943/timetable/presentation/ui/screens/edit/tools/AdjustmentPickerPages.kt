package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.shared.model.Timetable

/**
 * Hierarchical timetable picker. Only the selected/expanded timetable exposes courses;
 * courses then expand into lesson slots. Selection is committed with the standard bottom
 * EdgeButton, matching the rest of the editor instead of applying while browsing.
 */
@Composable
fun TimetablePickerPage(timetables: List<Timetable>, selectedId: Long, onSelect: (Long) -> Unit) {
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()
    var pendingId by remember(selectedId, timetables) { mutableLongStateOf(selectedId) }
    var expandedTable by remember(selectedId) { mutableLongStateOf(selectedId) }
    var expandedCourse by remember { mutableLongStateOf(-1L) }
    ScreenScaffold(
        scrollState = state,
        timeText = {},
        edgeButton = {
            EdgeButton(enabled = timetables.any { it.timetableId == pendingId }, onClick = { onSelect(pendingId) }) {
                Icon(Icons.Rounded.Check, contentDescription = "确认")
            }
        },
    ) { padding ->
        TransformingLazyColumn(state = state, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text("选择课表") }
            }
            timetables.forEach { table ->
                item(key = "t${table.timetableId}") {
                    val expanded = expandedTable == table.timetableId
                    OneUiCapsuleSurface(
                        title = table.semesterName,
                        subtitle = "${table.semesterStart.toDisplayString()} 起 · ${table.allCourses.size} 门课程${if (expanded) " · 已展开" else ""}",
                        selected = table.timetableId == pendingId,
                        onClick = {
                            pendingId = table.timetableId
                            expandedTable = if (expanded) -1L else table.timetableId
                            expandedCourse = -1L
                        },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    )
                }
                if (expandedTable == table.timetableId) {
                    table.allCourses.forEach { course ->
                        item(key = "c${table.timetableId}_${course.id}") {
                            val expanded = expandedCourse == course.id
                            OneUiCapsuleSurface(
                                title = course.name,
                                subtitle = "${course.timeSlots.size} 个课时${if (expanded) " · 已展开" else " · 点按查看"}",
                                onClick = { expandedCourse = if (expanded) -1L else course.id },
                                modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                            )
                        }
                        if (expandedCourse == course.id) course.timeSlots.forEach { slot ->
                            item(key = "s${slot.id}") {
                                OneUiCapsuleSurface(
                                    title = "${slot.dayOfWeek?.name ?: "未设星期"}  ${slot.startTime?.toDisplayString(true) ?: "--:--"}–${slot.endTime?.toDisplayString(true) ?: "--:--"}",
                                    subtitle = slot.remark ?: "课时",
                                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
