package com.hufeng943.timetable.presentation.ui.screens.more.settings.importer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
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

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != com.hufeng943.timetable.sync.WearOsSyncReceiverService.ACTION_IMPORT_RESULT) return
                val success = intent.getBooleanExtra(
                    com.hufeng943.timetable.sync.WearOsSyncReceiverService.EXTRA_SUCCESS,
                    false
                )
                if (success) {
                    Toast.makeText(context, "成功导入手机中的课表", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                } else {
                    Toast.makeText(
                        context,
                        intent.getStringExtra(com.hufeng943.timetable.sync.WearOsSyncReceiverService.EXTRA_MESSAGE)
                            ?: "导入失败",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(com.hufeng943.timetable.sync.WearOsSyncReceiverService.ACTION_IMPORT_RESULT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(Unit) { viewModel.loadBackupFiles(context) }

    LaunchedEffect(importState) {
        when (val s = importState) {
            is ImportState.Success -> {
                Toast.makeText(context, "成功导入 ${s.count} 门课表！", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onNavigateBack()
            }
            is ImportState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    ScreenScaffold(
        scrollState = scrollState,
        timeText = { if (config.isShowTopTime) TimeText() },
        edgeButton = {
            EdgeButton(onClick = { scope.launch { scrollState.animateScrollToItem(0) } }) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(state = scrollState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text("导入课表") }
            }

            item {
                OneUiCapsuleSurface(
                    title = if (importState is ImportState.Importing) "正在导入…" else "从 Galaxy 手机选择文件",
                    subtitle = "在 S25+ 上选择 .json / .ics / .csv，自动发送到 Watch7",
                    icon = Icons.Rounded.PhoneAndroid,
                    emphasize = true,
                    onClick = {
                        if (importState !is ImportState.Importing) {
                            scope.launch {
                                val launched = com.hufeng943.timetable.data.WearFileTransfer
                                    .requestImportFromPhone(context)
                                if (!launched) {
                                    Toast.makeText(
                                        context,
                                        "无法连接手机，请确认手机端 Timetable 已安装",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            if (backupFiles.isNotEmpty()) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) { Text("手表本地备份") }
                }

                items(backupFiles) { file ->
                    OneUiCapsuleSurface(
                        title = file.name,
                        subtitle = "${file.extension.uppercase()} · ${(file.length() / 1024).coerceAtLeast(1)} KB",
                        icon = Icons.Rounded.FileOpen,
                        onClick = { viewModel.importFromFile(file) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                    )
                }
            }
        }
    }
}
