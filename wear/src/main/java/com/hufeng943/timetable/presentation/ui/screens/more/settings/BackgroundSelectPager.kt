package com.hufeng943.timetable.presentation.ui.screens.more.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val BACKGROUND_MAX_SIDE_PX = 720

@Composable
fun BackgroundSelectPager(
    config: AppConfig,
    onBackgroundSelected: (TimetableBackgroundMode, String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()

    // Use the Android photo picker contract. AndroidX automatically falls back to
    // ACTION_OPEN_DOCUMENT on devices where the photo picker is unavailable.
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) {
                    runCatching {
                        val resolver = context.contentResolver
                        val source = ImageDecoder.createSource(resolver, uri)
                        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            val width = info.size.width
                            val height = info.size.height
                            if (width <= 0 || height <= 0) error("Invalid image dimensions")
                            val maxSide = maxOf(width, height)
                            if (maxSide > BACKGROUND_MAX_SIDE_PX) {
                                val scale = BACKGROUND_MAX_SIDE_PX.toFloat() / maxSide.toFloat()
                                decoder.setTargetSize(
                                    (width * scale).toInt().coerceAtLeast(1),
                                    (height * scale).toInt().coerceAtLeast(1),
                                )
                            }
                        }

                        val dir = File(context.filesDir, "backgrounds").apply { mkdirs() }
                        // A unique file name is intentional. Reusing one fixed path makes Compose
                        // see the same mode/path pair and skip reloading when the user changes image.
                        val target = File(dir, "timetable_background_${System.currentTimeMillis()}.jpg")
                        val temp = File(dir, ".${target.name}.tmp")

                        try {
                            temp.outputStream().buffered().use { output ->
                                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
                                    "Failed to encode timetable background"
                                }
                            }
                        } finally {
                            if (!bitmap.isRecycled) bitmap.recycle()
                        }

                        check(temp.isFile && temp.length() > 0L) { "Empty timetable background" }

                        // Verify the file can actually be decoded before it becomes persistent state.
                        val verify = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(temp.absolutePath, verify)
                        check(verify.outWidth > 0 && verify.outHeight > 0) { "Unreadable timetable background" }

                        check(temp.renameTo(target)) { "Failed to finalize timetable background" }

                        // Remove stale copies only after the new file has been fully written/verified.
                        dir.listFiles()?.forEach { file ->
                            if (file != target &&
                                ((file.name.startsWith("timetable_background_") && file.extension == "jpg") ||
                                    file.name == "timetable_background.jpg")
                            ) {
                                runCatching { file.delete() }
                            }
                            if (file.name.startsWith(".timetable_background_") && file.extension == "tmp") {
                                runCatching { file.delete() }
                            }
                        }

                        target.absolutePath
                    }.getOrElse { error ->
                        if (error is CancellationException) throw error
                        null
                    }
                }
                if (path != null) {
                    onBackgroundSelected(TimetableBackgroundMode.IMAGE, path)
                }
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
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transform)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
