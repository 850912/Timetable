package com.hufeng943.timetable.presentation.ui.screens.edit.timeslot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

@Composable
fun GroupSyncConfirmScreen(
    siblingCount: Int,
    onSync: () -> Unit,
    onCurrentOnly: () -> Unit,
) {
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()
    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(
                    state = state,
                    flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
                    rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
                    contentPadding = padding,
                    modifier = Modifier.fillMaxSize(),
                ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text("同步时间修改？") }
            }
            item {
                OneUiCapsuleSurface(
                    title = "同步到其他 $siblingCount 天",
                    subtitle = "保持这次多日期创建的课时时间一致",
                    icon = Icons.Rounded.Link,
                    emphasize = true,
                    onClick = onSync,
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = "仅修改当前日期",
                    subtitle = "其他日期保持原来的时间",
                    icon = Icons.Rounded.LinkOff,
                    onClick = onCurrentOnly,
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
