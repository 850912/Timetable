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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
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
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import kotlinx.coroutines.launch

@Composable
fun ExportScreen(
    viewModel: ExportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val scope = rememberCoroutineScope()
    val config = LocalAppConfig.current
    val exportState by viewModel.state.collectAsState()
    val previewStats by viewModel.previewStats.collectAsState()

    var selectedFormat by remember { mutableStateOf(ExportFormat.ICS) }
    var selectedScope by remember { mutableStateOf(ExportScope.CURRENT) }


    LaunchedEffect(exportState) {
        when (val s = exportState) {
            is ExportState.Success -> {
                Toast.makeText(context, "导出成功: ${s.fileName}", Toast.LENGTH_LONG).show()
                viewModel.resetState()
                onNavigateBack()
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
        timeText = {
            if (config.isShowTopTime) {
                TimeText()
            }
        },
        edgeButton = {
            EdgeButton(onClick = { scope.launch { scrollState.animateScrollToItem(0) } }) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
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
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text("导出课表")
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppTheme.colors.surfaceContainer)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "当前学期数据透视",
                            color = AppTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = previewStats?.semesterName ?: "正在读取...",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("课程", color = AppTheme.colors.textSecondary, fontSize = 10.sp)
                                Text("${previewStats?.totalCourses ?: 0} 门", color = AppTheme.colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("总节次", color = AppTheme.colors.textSecondary, fontSize = 10.sp)
                                Text("${previewStats?.totalInstances ?: 0} 节", color = AppTheme.colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("单双周", color = AppTheme.colors.textSecondary, fontSize = 10.sp)
                                Text(
                                    if (previewStats?.hasRecurrenceRules == true) "✓ 已推导" else "每周",
                                    color = if (previewStats?.hasRecurrenceRules == true) AppTheme.colors.badgeActive else AppTheme.colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                OneUiCapsuleSurface(
                    title = "日历 (.ics)",
                    subtitle = "流式实例导出 · 可导入系统日历",
                    selected = selectedFormat == ExportFormat.ICS,
                    emphasize = selectedFormat == ExportFormat.ICS,
                    onClick = { selectedFormat = ExportFormat.ICS },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = "表格 (.csv)",
                    subtitle = "UTF-8 BOM · 适合 Excel / Numbers",
                    selected = selectedFormat == ExportFormat.CSV,
                    onClick = { selectedFormat = ExportFormat.CSV },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = "一键备份 (.json)",
                    subtitle = "完整课表数据备份 · 推荐迁移时使用",
                    selected = selectedFormat == ExportFormat.JSON_BACKUP,
                    onClick = { selectedFormat = ExportFormat.JSON_BACKUP },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = if (selectedScope == ExportScope.CURRENT) "当前学期" else "全部学期",
                    subtitle = if (selectedScope == ExportScope.CURRENT) "仅导出当前活跃学期 · 点击切换" else "包含所有历史学期 · 点击切换",
                    selected = selectedScope == ExportScope.ALL,
                    onClick = {
                        val newScope = if (selectedScope == ExportScope.CURRENT) ExportScope.ALL else ExportScope.CURRENT
                        selectedScope = newScope
                        viewModel.updatePreview(newScope)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = if (exportState is ExportState.Exporting) "正在发送到手机…" else "发送到 Galaxy 手机",
                    subtitle = if (exportState is ExportState.Exporting) "请保持 Watch7 与手机连接" else "通过 Wear Data Layer 安全传输",
                    icon = Icons.Rounded.FileDownload,
                    emphasize = true,
                    onClick = if (exportState is ExportState.Exporting) null else ({
                        viewModel.executePhoneExport(context, selectedFormat, selectedScope)
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }
        }
    }
}
