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
import androidx.wear.compose.material3.Button
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
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
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

    val createDocLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) { viewModel.executeDirectExport(context, uri, selectedFormat, selectedScope) }
    }


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
                TitleCard(
                    onClick = { selectedFormat = ExportFormat.ICS },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedFormat == ExportFormat.ICS) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = AppTheme.colors.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("日历 (.ics)")
                        }
                    }
                ) {
                    Text("流式实例导出")
                }
            }

            item {
                TitleCard(
                    onClick = { selectedFormat = ExportFormat.CSV },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedFormat == ExportFormat.CSV) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = AppTheme.colors.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("表格 (.csv)")
                        }
                    }
                ) {
                    Text("UTF-8 BOM 表格")
                }
            }

            item {
                TitleCard(
                    onClick = { selectedFormat = ExportFormat.JSON_BACKUP },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedFormat == ExportFormat.JSON_BACKUP) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = AppTheme.colors.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("一键备份 (.json)")
                        }
                    }
                ) {
                    Text("完整数据 DTO 备份")
                }
            }

            item {
                TitleCard(
                    onClick = {
                        selectedScope = if (selectedScope == ExportScope.CURRENT) ExportScope.ALL else ExportScope.CURRENT
                        viewModel.updatePreview(selectedScope)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = {
                        Text(if (selectedScope == ExportScope.CURRENT) "范围: 当前学期" else "范围: 全部学期")
                    }
                ) {
                    Text(if (selectedScope == ExportScope.CURRENT) "仅导出当前活跃学期" else "导出所有历史学期")
                }
            }

            item {
                Button(
                    onClick = {
                        val ext = when (selectedFormat) { ExportFormat.ICS -> "ics"; ExportFormat.CSV -> "csv"; ExportFormat.JSON_BACKUP -> "json" }
                        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                        createDocLauncher.launch("Timetable_Export_${timeStamp}.${ext}")
                    },
                    enabled = exportState !is ExportState.Exporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                    icon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) }
                ) {
                    Text(if (exportState is ExportState.Exporting) "正在导出..." else "直接保存到本机")
                }
            }
        }
    }
}
