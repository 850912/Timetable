package com.hufeng943.timetable.presentation.ui.screens.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

@Composable
fun DeleteConfirmScreen(
    detail: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            contentPadding = contentPadding,
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(scrollState),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollState)
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text(stringResource(R.string.delete_confirm)) }
            }

            item {
                OneUiCapsuleSurface(
                    title = detail,
                    subtitle = stringResource(R.string.delete_confirm_hint, detail),
                    icon = Icons.Rounded.DeleteForever,
                    destructive = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = "我再想想",
                    icon = Icons.Rounded.Close,
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = "确认删除",
                    subtitle = "该操作无法撤销",
                    icon = Icons.Rounded.Check,
                    destructive = true,
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }
        }
    }
}
