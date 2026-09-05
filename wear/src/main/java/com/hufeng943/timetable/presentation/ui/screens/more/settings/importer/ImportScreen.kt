package com.hufeng943.timetable.presentation.ui.screens.more.settings.importer

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import kotlinx.coroutines.launch

@Composable
fun ImportScreen(
    viewModel: ImportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val scope = rememberCoroutineScope()
    val config = LocalAppConfig.current

    val importState by viewModel.state.collectAsState()
    val backupFiles by viewModel.backupFiles.collectAsState()

    val openDocLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                viewModel.importFromUri(context, uri)
            }
        }

    LaunchedEffect(Unit) {
        viewModel.loadBackupFiles(context)
    }

    LaunchedEffect(importState) {
        when (val s = importState) {
            is ImportState.Success -> {
                Toast.makeText(
                    context,
                    "成功导入 ${s.count} 门课表！",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetState()
                onNavigateBack()
            }

            is ImportState.Error -> {
                Toast.makeText(
                    context,
                    s.message,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetState()
            }

            else -> Unit
        }
    }

    ScreenScaffold(
        scrollState = scrollState,
        timeText = {
            if (config.isShowTopTime) {
                TimeText()
            }
        },
        edgeButton = {
            EdgeButton(
                onClick = {
                    scope.launch {
                        scrollState.animateScrollToItem(0)
                    }
                }
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowUp,
                    contentDescription = null
                )
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding
                        ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text("导入本地课表")
                }
            }

            item {
                TitleCard(
                    onClick = {
                        openDocLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        ),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = {
                        Text("打开系统文件浏览器")
                    }
                ) {
                    Text("支持 .json / .ics / .csv；重装后请用此入口选择文件")
                }
            }

            if (backupFiles.isNotEmpty()) {
                items(backupFiles) { file ->
                    TitleCard(
                        onClick = {
                            viewModel.importFromFile(file)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            ),
                        transformation = SurfaceTransformation(transformationSpec),
                        title = {
                            Text(file.name)
                        }
                    ) {
                        Text(
                            "${file.extension.uppercase()} 格式 • " +
                                "${(file.length() / 1024).coerceAtLeast(1)} KB"
                        )
                    }
                }
            }
        }
    }
}
