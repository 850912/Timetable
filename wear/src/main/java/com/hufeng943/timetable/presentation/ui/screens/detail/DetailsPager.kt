package com.hufeng943.timetable.presentation.ui.screens.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextDefaults
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.DynamicSubTheme
import com.hufeng943.timetable.presentation.ui.common.ui.CourseUi
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.ColorBox
import com.hufeng943.timetable.presentation.ui.components.TimeText
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import java.time.format.TextStyle

@Composable
fun DetailsPager(
    courseUi: CourseUi, onCourseClick: () -> Unit, onCourseLongClick: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    DynamicSubTheme(seedColor = courseUi.color) {
        ScreenScaffold(
            scrollState = scrollState,
        ) { contentPadding ->
            TransformingLazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            
                            .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(text = stringResource(R.string.course_details_title))
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ColorBox(color = courseUi.displayColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = courseUi.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = Int.MAX_VALUE,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    val dayStr =
                        courseUi.timeSlot.dayOfWeek?.toDisplayString(TextStyle.FULL_STANDALONE)
                            ?: ""

                    if (dayStr.isEmpty() && courseUi.timeSlot.startTime == null) {
                        Text(
                            text = stringResource(R.string.not_set),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                
                                .minimumVerticalContentPadding(TextDefaults.minimumTopListContentPadding)
                                .padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom
                        ) {
                            if (dayStr.isNotEmpty()) {
                                Text(
                                    text = dayStr,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            TimeText(
                                time = courseUi.timeSlot.startTime,
                                style = MaterialTheme.typography.bodyLarge,
                                isVertical = false
                            )

                            Text(
                                text = " - ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            TimeText(
                                time = courseUi.timeSlot.endTime,
                                style = MaterialTheme.typography.bodyLarge,
                                isVertical = false
                            )
                        }
                    }
                }

                if (!courseUi.location.isNullOrBlank()) {
                    item {
                        DetailListItem(
                            icon = Icons.Rounded.Place,
                            text = courseUi.location,
                            modifier = Modifier
                                
                                .minimumVerticalContentPadding(TextDefaults.minimumTopListContentPadding)
                        )
                    }
                }

                if (!courseUi.teacher.isNullOrBlank()) {
                    item {
                        DetailListItem(
                            icon = Icons.Rounded.Person,
                            text = courseUi.teacher,
                            modifier = Modifier
                                
                                .minimumVerticalContentPadding(TextDefaults.minimumTopListContentPadding)
                        )
                    }
                }

                val remark = courseUi.timeSlot.remark
                if (!remark.isNullOrBlank()) {
                    item {
                        DetailListItem(
                            icon = Icons.AutoMirrored.Rounded.Notes,
                            text = remark,
                            modifier = Modifier
                                
                                .minimumVerticalContentPadding(TextDefaults.minimumTopListContentPadding)
                        )
                    }
                }

                item {
                    DetailListItem(
                        icon = Icons.Rounded.Sync,
                        text = courseUi.timeSlot.recurrence.toDisplayString(),
                        modifier = Modifier
                            
                            .minimumVerticalContentPadding(TextDefaults.minimumTopListContentPadding)
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    OneUiCapsuleSurface(
                        title = stringResource(R.string.edit_course_edit),
                        subtitle = stringResource(R.string.edit_course_long_press_hint),
                        icon = Icons.Rounded.Edit,
                        emphasize = true,
                        onClick = onCourseClick,
                        onLongClick = onCourseLongClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailListItem(
    icon: ImageVector, text: String, modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(28.dp))
        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = Int.MAX_VALUE,
            modifier = Modifier.weight(1f))
    }
}
