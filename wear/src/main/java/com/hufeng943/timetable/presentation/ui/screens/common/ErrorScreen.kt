package com.hufeng943.timetable.presentation.ui.screens.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ScreenScaffold
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.viewmodel.AppError

@Composable
fun ErrorScreen(throwable: Throwable) {
    ScreenScaffold(timeText = {}) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            OneUiCapsuleSurface(
                title = stringResource(R.string.apperror),
                subtitle = throwable.asMessage(),
                icon = Icons.Rounded.ErrorOutline,
                destructive = true,
                emphasize = true
            )
        }
    }
}

@Composable
fun Throwable.asMessage(): String = when (this) {
    is AppError.TimetableNotFound -> stringResource(R.string.apperror_timetablenotfound, id ?: stringResource(R.string.unknown))
    is AppError.CourseNotFound -> stringResource(R.string.apperror_coursenotfound, id ?: stringResource(R.string.unknown))
    is AppError.TimeSlotNotFound -> stringResource(R.string.apperror_timeslotnotfound, id ?: stringResource(R.string.unknown))
    is AppError.InvalidParameter -> stringResource(R.string.apperror_invalidparameter, navArgs)
    is AppError.UnexpectedEmpty -> stringResource(R.string.apperror_unexpectedempty)
    is AppError.Unknown -> stringResource(R.string.apperror_unknown, original.message ?: stringResource(R.string.unknown))
    else -> this.message ?: stringResource(R.string.apperror_unknown, this.javaClass.simpleName)
}
