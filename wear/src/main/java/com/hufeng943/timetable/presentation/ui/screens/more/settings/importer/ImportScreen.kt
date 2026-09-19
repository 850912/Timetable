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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.R
import kotlinx.coroutines.launch

import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    val importState by viewModel.state.collectAsStateWithLifecycle()
    val backupFiles by viewModel.backupFiles.collectAsStateWithLifecycle()
    val phoneImportSuccess = stringResource(R.string.import_phone_success)
    val importFailed = stringResource(R.string.import_failed)
    val phoneUnavailable = stringResource(R.string.import_phone_unavailable)

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != com.hufeng943.timetable.sync.WearOsSyncReceiverService.ACTION_IMPORT_RESULT) return
                val success = intent.getBooleanExtra(
                    com.hufeng943.timetable.sync.WearOsSyncReceiverService.EXTRA_SUCCESS,
                    false
                )
                if (success) {
                    Toast.makeText(context, phoneImportSuccess, Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                } else {
                    Toast.makeText(
                        context,
                        importFailed,
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
                Toast.makeText(context, context.getString(R.string.import_success, s.count), Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onNavigateBack()
            }
            is ImportState.Error -> {
                Toast.makeText(context, importFailed, Toast.LENGTH_LONG).show()
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
        }
    ) { contentPadding ->
        TransformingLazyColumn(state = scrollState, contentPadding = contentPadding, rotaryScrollableBehavior = androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults.behavior(scrollState, hapticFeedbackEnabled = false)) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text(stringResource(R.string.import_title)) }
            }

            item {
                OneUiCapsuleSurface(
                    title = if (importState is ImportState.Importing) stringResource(R.string.importing) else stringResource(R.string.import_from_phone),
                    subtitle = stringResource(R.string.import_from_phone_summary),
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
                                        phoneUnavailable,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                )
            }

            if (backupFiles.isNotEmpty()) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            ,
                        transformation = SurfaceTransformation(transformationSpec)
                    ) { Text(stringResource(R.string.import_local_backups)) }
                }

                items(backupFiles, key = { it.absolutePath }) { file ->
                    OneUiCapsuleSurface(
                        title = file.name,
                        subtitle = "${file.extension.uppercase()} · ${(file.length() / 1024).coerceAtLeast(1)} KB",
                        icon = Icons.Rounded.FileOpen,
                        onClick = { viewModel.importFromFile(file) },
                        modifier = Modifier
                            .fillMaxWidth()
                            
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                    )
                }
            }
        }
    }
}
