package com.hufeng943.timetable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.SurfaceTransformation
import com.hufeng943.timetable.R

@Composable
fun ColorPickerCard(
    label: String,
    color: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isNull: Boolean,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null
) {
    // transformation is retained in the signature for call-site compatibility.
    OneUiCapsuleSurface(
        title = label,
        subtitle = if (!isNull) stringResource(R.string.clear_long_press) else "点击选择课程强调色",
        accentColor = color,
        selected = !isNull,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
    )
}
