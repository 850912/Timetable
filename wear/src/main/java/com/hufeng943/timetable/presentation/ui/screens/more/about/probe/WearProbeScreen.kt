package com.hufeng943.timetable.presentation.ui.screens.more.about.probe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.probe.WearProbe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WearProbeScreen() {
    val context = LocalContext.current
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    var output by remember { mutableStateOf("点击运行诊断。") }
    val scope = rememberCoroutineScope()

    fun run(block: () -> String) {
        output = "检测中…"
        scope.launch {
            output = withContext(Dispatchers.IO) {
                runCatching(block).getOrElse { "FAIL ${it.javaClass.simpleName}: ${it.message ?: "无详细信息"}" }
            }
        }
    }

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                ) { Text("中国区通信探针") }
            }
            item {
                OneUiCapsuleSurface(
                    title = "运行完整诊断",
                    subtitle = "GMS · NodeClient · connectedNodes",
                    icon = Icons.Rounded.BugReport,
                    emphasize = true,
                    onClick = { run { WearProbe.runDiagnostics(context) } },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = "手表 → 手机 Message",
                    subtitle = "测试 MessageClient 双向通道",
                    icon = Icons.Rounded.Send,
                    onClick = { run { WearProbe.sendMessage(context) } },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = "手表 → 手机 DataItem",
                    subtitle = "测试 DataClient 同步通道",
                    icon = Icons.Rounded.CloudSync,
                    onClick = { run { WearProbe.sendDataItem(context) } },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = "刷新最近接收事件",
                    subtitle = WearProbe.lastEvent(context),
                    icon = Icons.Rounded.Refresh,
                    onClick = { output = WearProbe.lastEvent(context) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                OneUiCapsuleSurface(
                    title = "诊断输出",
                    subtitle = "完整结果见下方，可滚动查看",
                    icon = Icons.Rounded.BugReport,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
            item {
                Text(
                    text = output,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
