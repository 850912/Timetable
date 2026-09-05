package com.hufeng943.timetable.presentation.ui.components.edit

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.DynamicSubTheme
import com.hufeng943.timetable.presentation.ui.common.ui.TimetableUi
import com.hufeng943.timetable.presentation.ui.components.ColorBox

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

    DynamicSubTheme(seedColor = timetable.color) {
        TitleCard(
            onClick = { onTimetableClick(timetable.timetableId) },
            onLongClick = { onTimetableLongClick(timetable.timetableId) },
            transformation = transformation,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (timetable.color != Color.Unspecified) {
                        ColorBox(color = timetable.color)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        timetable.displayName, maxLines = 1, modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE
                        )
                    )
                }
            },
            subtitle = {
                Text(
                    text = "$status${stringResource(R.string.info_separator)}${
                        stringResource(
                            R.string.edit_timetable_number, timetable.courses.size
                        )
                    }",
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE
                    )
                )
            },
            modifier = modifier
        )
    }
}