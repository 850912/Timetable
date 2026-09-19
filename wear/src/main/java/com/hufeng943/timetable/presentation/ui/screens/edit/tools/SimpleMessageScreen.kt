package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

@Composable
internal fun SimpleMessageScreen(title: String, message: String) {
    val state = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(
            state = state,
            rotaryScrollableBehavior = androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults.behavior(state, hapticFeedbackEnabled = false),
            contentPadding = padding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item { ListHeader(modifier = Modifier.fillMaxWidth()) { Text(title) } }
            item { OneUiCapsuleSurface(title = message, modifier = Modifier.fillMaxWidth()) }
        }
    }
}
