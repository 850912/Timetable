package com.hufeng943.timetable.presentation.ui.screens.more.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.common.TimetableBackgroundMode
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun BackgroundSelectPager(
    config: AppConfig,
    onBackgroundSelected: (TimetableBackgroundMode, String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) {
                    runCatching {
                        val source = context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                            ?: return@runCatching null
                        val maxSide = 512
                        val scale = minOf(1f, maxSide.toFloat() / maxOf(source.width, source.height))
                        val bitmap = if (scale < 1f) {
                            Bitmap.createScaledBitmap(
                                source,
                                (source.width * scale).toInt().coerceAtLeast(1),
                                (source.height * scale).toInt().coerceAtLeast(1),
                                true,
                            ).also { if (it !== source) source.recycle() }
                        } else source
                        val target = File(context.filesDir, "timetable_background.webp")
                        target.outputStream().buffered().use { output ->
                            @Suppress("DEPRECATION")
                            bitmap.compress(Bitmap.CompressFormat.WEBP, 82, output)
                        }
                        bitmap.recycle()
                        target.absolutePath
                    }.getOrNull()
                }
                if (path != null) onBackgroundSelected(TimetableBackgroundMode.IMAGE, path)
            }
        }
    }

    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(state = state, contentPadding = padding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transform),
                ) { Text(stringResource(R.string.settings_timetable_background)) }
            }
            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.settings_background_solid),
                    subtitle = stringResource(R.string.settings_background_solid_summary),
                    icon = Icons.Rounded.RadioButtonChecked,
                    selected = config.timetableBackgroundMode == TimetableBackgroundMode.SOLID,
                    onClick = { onBackgroundSelected(TimetableBackgroundMode.SOLID, null) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.settings_background_theme),
                    subtitle = stringResource(R.string.settings_background_theme_summary),
                    icon = Icons.Rounded.ColorLens,
                    selected = config.timetableBackgroundMode == TimetableBackgroundMode.THEME,
                    onClick = { onBackgroundSelected(TimetableBackgroundMode.THEME, null) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.settings_background_image),
                    subtitle = stringResource(R.string.settings_background_image_summary),
                    icon = Icons.Rounded.Image,
                    selected = config.timetableBackgroundMode == TimetableBackgroundMode.IMAGE,
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
