package com.hufeng943.timetable.presentation.ui.screens.more.settings.export

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalLiquidGlassBackdrop
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.globalLiquidGlass
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
import com.hufeng943.timetable.shared.export.ExportTarget
import kotlinx.coroutines.launch

@Composable
fun ExportScreen(
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val config = LocalAppConfig.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val previewStats by viewModel.previewStats.collectAsStateWithLifecycle()
    var selectedFormat by remember { mutableStateOf(ExportFormat.ICS) }
    var selectedScope by remember { mutableStateOf(ExportScope.CURRENT) }
    var selectedTarget by remember { mutableStateOf(ExportTarget.PHONE_APP) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state) {
        when (val s = state) {
            is ExportState.Success -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            is ExportState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    ScreenScaffold(
        scrollState = scrollState,
        timeText = { if (config.isShowTopTime) TimeText(backgroundColor = Color.Transparent) },
        edgeButton = {
            EdgeButton(onClick = { scope.launch { scrollState.animateScrollToItem(0) } }) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(state = scrollState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                ) { Text("导出") }
            }
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .globalLiquidGlass(RoundedCornerShape(20.dp), AppTheme.colors.surfaceContainer)
                        .background(AppTheme.colors.surfaceContainer.copy(alpha = if (LocalLiquidGlassBackdrop.current != null && config.isLiquidGlassEnabled) 0f else 1f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("导出目标（手机）", color = AppTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OneUiCapsuleSurface(
                                title = "App", subtitle = "导入手机", modifier = Modifier.weight(1f),
                                selected = selectedTarget == ExportTarget.PHONE_APP,
                                emphasize = selectedTarget == ExportTarget.PHONE_APP,
                                icon = Icons.Rounded.PhoneAndroid,
                                onClick = { selectedTarget = ExportTarget.PHONE_APP },
                            )
                            OneUiCapsuleSurface(
                                title = "文件", subtitle = "保存手机", modifier = Modifier.weight(1f),
                                selected = selectedTarget == ExportTarget.PHONE_FILE,
                                icon = Icons.Rounded.Save,
                                onClick = { selectedTarget = ExportTarget.PHONE_FILE },
                            )
                            OneUiCapsuleSurface(
                                title = "两者", subtitle = "App + 文件", modifier = Modifier.weight(1f),
                                selected = selectedTarget == ExportTarget.BOTH,
                                onClick = { selectedTarget = ExportTarget.BOTH },
                            )
                        }
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .globalLiquidGlass(RoundedCornerShape(20.dp), AppTheme.colors.surfaceContainer)
                        .background(AppTheme.colors.surfaceContainer.copy(alpha = if (LocalLiquidGlassBackdrop.current != null && config.isLiquidGlassEnabled) 0f else 1f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(previewStats?.semesterName?.takeIf { it.isNotBlank() } ?: "正在读取课表…",
                            color = AppTheme.colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text("${previewStats?.totalCourses ?: 0} 门课程 · ${previewStats?.totalInstances ?: 0} 节", color = AppTheme.colors.textSecondary, fontSize = 10.sp)
                    }
                }
            }
            item { OneUiCapsuleSurface(title = "日历 (.ics)", subtitle = "保存到手机时可导入系统日历", modifier = Modifier.fillMaxWidth(), selected = selectedFormat == ExportFormat.ICS, emphasize = selectedFormat == ExportFormat.ICS, onClick = { selectedFormat = ExportFormat.ICS }) }
            item { OneUiCapsuleSurface(title = "表格 (.csv)", subtitle = "保存到手机，适合 Excel / Numbers", modifier = Modifier.fillMaxWidth(), selected = selectedFormat == ExportFormat.CSV, onClick = { selectedFormat = ExportFormat.CSV }) }
            item { OneUiCapsuleSurface(title = "完整备份 (.json)", subtitle = "完整课表备份", modifier = Modifier.fillMaxWidth(), selected = selectedFormat == ExportFormat.JSON_BACKUP, onClick = { selectedFormat = ExportFormat.JSON_BACKUP }) }
            item {
                OneUiCapsuleSurface(
                    title = if (selectedScope == ExportScope.CURRENT) "当前学期" else "全部学期",
                    subtitle = if (selectedScope == ExportScope.CURRENT) "点击切换到全部学期" else "点击切换到当前学期",
                    modifier = Modifier.fillMaxWidth(),
                    selected = selectedScope == ExportScope.ALL,
                    onClick = {
                        selectedScope = if (selectedScope == ExportScope.CURRENT) ExportScope.ALL else ExportScope.CURRENT
                        viewModel.updatePreview(selectedScope)
                    },
                )
            }
            item {
                val busy = state is ExportState.Exporting
                OneUiCapsuleSurface(
                    title = if (busy) "正在发送到手机…" else when (selectedTarget) {
                        ExportTarget.PHONE_APP -> "导出到手机 App"
                        ExportTarget.PHONE_FILE -> "导出文件到手机"
                        ExportTarget.BOTH -> "同时导出到手机 App 与文件"
                    },
                    subtitle = if (busy) "请保持手表与手机连接" else "文件由手机端写入 Downloads/Timetable，不会保存在手表",
                    icon = Icons.Rounded.FileDownload,
                    emphasize = true,
                    onClick = if (busy) null else ({
                        viewModel.exportWithTarget(context, selectedTarget, selectedFormat, selectedScope)
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
