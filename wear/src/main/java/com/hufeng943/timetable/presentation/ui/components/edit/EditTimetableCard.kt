package com.hufeng943.timetable.presentation.ui.components.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.SurfaceTransformation
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.ui.TimetableUi
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Composable
fun EditTimetableCard(
    onTimetableClick: (Long) -> Unit,
    timetable: TimetableUi,
    onTimetableLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val status = when {
        today < timetable.semesterStart -> stringResource(R.string.timetable_status_not_started)
        (timetable.semesterEnd != null && today > timetable.semesterEnd) -> stringResource(R.string.timetable_status_ended)
        else -> stringResource(R.string.timetable_status_in_progress)
    }
    val subtitle = "$status${stringResource(R.string.info_separator)}${
        stringResource(R.string.edit_timetable_number, timetable.courses.size)
    }"

    OneUiCapsuleSurface(
        title = timetable.displayName,
        subtitle = subtitle,
        accentColor = timetable.displayColor,
        onClick = { onTimetableClick(timetable.timetableId) },
        onLongClick = { onTimetableLongClick(timetable.timetableId) },
        modifier = modifier,
    )
}
