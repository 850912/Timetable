package com.hufeng943.timetable.presentation.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleButton
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

@Composable
fun EmptyPager(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OneUiCapsuleSurface(
            title = stringResource(R.string.home_no_timetable_hint),
            subtitle = stringResource(R.string.home_add_timetable),
            icon = Icons.Rounded.CalendarMonth,
            emphasize = true,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        OneUiCapsuleButton(
            icon = Icons.Rounded.Add,
            label = stringResource(R.string.home_add_timetable),
            onClick = onAddClick,
            emphasize = true,
        )
    }
}
