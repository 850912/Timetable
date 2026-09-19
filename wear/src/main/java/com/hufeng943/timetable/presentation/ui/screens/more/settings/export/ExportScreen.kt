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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalLiquidGlassBackdrop
import com.hufeng943.timetable.presentation.ui.theme.AppTheme
import com.hufeng943.timetable.presentation.ui.components.globalLiquidGlass
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.R
import kotlinx.coroutines.launch

import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val exportState by viewModel.state.collectAsStateWithLifecycle()
    val previewStats by viewModel.previewStats.collectAsStateWithLifecycle()
    val exportDone = stringResource(R.string.export_done)
    val exportFailed = stringResource(R.string.export_failed)

    var selectedFormat by remember { mutableStateOf(ExportFormat.ICS) }
    var selectedScope by remember { mutableStateOf(ExportScope.CURRENT) }


    LaunchedEffect(exportState) {
        when (val s = exportState) {
            is ExportState.Success -> {
                Toast.makeText(context, exportDone, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            is ExportState.Error -> {
                Toast.makeText(context, exportFailed, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    ScreenScaffold(
        scrollState = scrollState,
        timeText = {
            if (config.isShowTopTime) {
                TimeText(backgroundColor = Color.Transparent)
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
            contentPadding = contentPadding,
            rotaryScrollableBehavior = androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults.behavior(scrollState, hapticFeedbackEnabled = false),
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(stringResource(R.string.export_title))
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .globalLiquidGlass(RoundedCornerShape(20.dp), AppTheme.colors.surfaceContainer)
                        .background(AppTheme.colors.surfaceContainer.copy(alpha = if (LocalLiquidGlassBackdrop.current != null && (LocalAppConfig.current.isLiquidGlassEnabled)) 0f else 1f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.export_direct),
                            color = AppTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = (previewStats?.semesterName ?: stringResource(R.string.export_loading)) +
                                " · " + stringResource(R.string.export_choose_then_send),
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
                                Text(stringResource(R.string.export_courses), color = AppTheme.colors.textSecondary, fontSize = 10.sp)
                                Text(stringResource(R.string.export_course_count, previewStats?.totalCourses ?: 0), color = AppTheme.colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(stringResource(R.string.export_sessions), color = AppTheme.colors.textSecondary, fontSize = 10.sp)
                                Text(stringResource(R.string.export_session_count, previewStats?.totalInstances ?: 0), color = AppTheme.colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(stringResource(R.string.export_recurrence), color = AppTheme.colors.textSecondary, fontSize = 10.sp)
                                Text(
                                    if (previewStats?.hasRecurrenceRules == true) stringResource(R.string.export_recurrence_derived) else stringResource(R.string.export_recurrence_weekly),
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
                    title = stringResource(R.string.export_ics),
                    subtitle = stringResource(R.string.export_ics_summary),
                    selected = selectedFormat == ExportFormat.ICS,
                    emphasize = selectedFormat == ExportFormat.ICS,
                    onClick = { selectedFormat = ExportFormat.ICS },
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.export_csv),
                    subtitle = stringResource(R.string.export_csv_summary),
                    selected = selectedFormat == ExportFormat.CSV,
                    onClick = { selectedFormat = ExportFormat.CSV },
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = stringResource(R.string.export_json),
                    subtitle = stringResource(R.string.export_json_summary),
                    selected = selectedFormat == ExportFormat.JSON_BACKUP,
                    onClick = { selectedFormat = ExportFormat.JSON_BACKUP },
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = if (selectedScope == ExportScope.CURRENT) stringResource(R.string.export_current) else stringResource(R.string.export_all),
                    subtitle = if (selectedScope == ExportScope.CURRENT) stringResource(R.string.export_current_summary) else stringResource(R.string.export_all_summary),
                    selected = selectedScope == ExportScope.ALL,
                    onClick = {
                        val newScope = if (selectedScope == ExportScope.CURRENT) ExportScope.ALL else ExportScope.CURRENT
                        selectedScope = newScope
                        viewModel.updatePreview(newScope)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            item {
                OneUiCapsuleSurface(
                    title = if (exportState is ExportState.Exporting) stringResource(R.string.export_sending) else stringResource(R.string.export_send),
                    subtitle = if (exportState is ExportState.Exporting) stringResource(R.string.export_sending_summary) else stringResource(R.string.export_send_summary),
                    icon = Icons.Rounded.FileDownload,
                    emphasize = true,
                    onClick = if (exportState is ExportState.Exporting) null else ({
                        viewModel.executePhoneExport(context, selectedFormat, selectedScope)
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }
        }
    }
}
