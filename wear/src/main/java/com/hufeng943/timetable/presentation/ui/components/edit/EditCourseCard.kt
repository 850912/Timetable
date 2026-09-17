package com.hufeng943.timetable.presentation.ui.components.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.SurfaceTransformation
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.ui.CourseUi
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

@Composable
fun EditCourseCard(
    course: CourseUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null
) {
    val info = listOfNotNull(
        course.location,
        course.teacher,
        stringResource(R.string.edit_course_number, course.timeSlots.size)
    ).joinToString(stringResource(R.string.info_separator))

    OneUiCapsuleSurface(
        title = course.displayName,
        subtitle = info,
        accentColor = course.displayColor,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    )
}
