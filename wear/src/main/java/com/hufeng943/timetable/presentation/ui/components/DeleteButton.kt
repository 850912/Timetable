package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.SurfaceTransformation

@Composable
fun DeleteButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null
) {
    // Keep the transformation parameter in the public API so existing callers remain unchanged.
    OneUiCapsuleSurface(
        title = label,
        subtitle = "此操作不可撤销",
        icon = Icons.Rounded.Delete,
        destructive = true,
        onClick = onClick,
        modifier = modifier
    )
}
