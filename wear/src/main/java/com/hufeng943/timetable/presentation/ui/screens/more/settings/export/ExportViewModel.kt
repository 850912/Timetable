package com.hufeng943.timetable.presentation.ui.screens.more.settings.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.data.ExportFormatForPhone
import com.hufeng943.timetable.data.WearFileTransfer
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.export.BackupManager
import com.hufeng943.timetable.shared.export.CsvExporter
import com.hufeng943.timetable.shared.export.ExportPreviewStats
import com.hufeng943.timetable.shared.export.ExportTarget
import com.hufeng943.timetable.shared.export.IcsExporter
import com.hufeng943.timetable.shared.model.Timetable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import javax.inject.Inject

enum class ExportFormat { ICS, CSV, JSON_BACKUP }

enum class ExportScope { CURRENT, ALL }

sealed interface ExportState {
    object Idle : ExportState
    object Exporting : ExportState
    data class Success(val fileName: String) : ExportState
    data class Error(val message: String) : ExportState
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val repository: TimetableRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state.asStateFlow()

    private val _previewStats = MutableStateFlow<ExportPreviewStats?>(null)
    val previewStats: StateFlow<ExportPreviewStats?> = _previewStats.asStateFlow()

    init { loadPreview() }

    fun loadPreview() = updatePreview(ExportScope.CURRENT)

    fun updatePreview(scope: ExportScope) {
        viewModelScope.launch {
            runCatching {
                val timetables = withContext(Dispatchers.IO) {
                    repository.getAllTimetables().firstOrNull() ?: emptyList()
                }
                val targets = resolveExportTargets(scope, timetables)
                withContext(Dispatchers.Default) { IcsExporter.calculateStats(targets) }
            }.onSuccess { _previewStats.value = it }
                .onFailure { _previewStats.value = null }
        }
    }

    private fun resolveCurrentTimetable(list: List<Timetable>): Timetable? {
        if (list.isEmpty()) return null
        val now = java.time.LocalDate.now()
        val today = LocalDate(now.year, now.monthValue, now.dayOfMonth)
        return list.filter { timetable ->
            val end = timetable.semesterEnd ?: LocalDate.fromEpochDays(timetable.semesterStart.toEpochDays() + 140)
            today >= timetable.semesterStart && today <= end
        }.maxByOrNull { it.semesterStart }
    }

    private fun resolveExportTargets(scope: ExportScope, timetables: List<Timetable>): List<Timetable> =
        when (scope) {
            ExportScope.CURRENT -> listOfNotNull(resolveCurrentTimetable(timetables))
            ExportScope.ALL -> timetables
        }

    fun executePhoneExport(context: Context, format: ExportFormat, scope: ExportScope) =
        exportWithTarget(context, ExportTarget.PHONE_APP, format, scope)

    fun executeDirectExport(context: Context, uri: Uri, format: ExportFormat, scope: ExportScope) =
        exportWithTarget(context, ExportTarget.DOWNLOAD, format, scope, uri)

    /**
     * One coroutine owns the whole operation. BOTH therefore cannot report success
     * after the phone path while silently skipping the local path.
     */
    fun exportWithTarget(
        context: Context,
        target: ExportTarget,
        format: ExportFormat,
        scope: ExportScope,
        uri: Uri? = null,
    ) {
        viewModelScope.launch {
            _state.value = ExportState.Exporting
            try {
                val timetables = withContext(Dispatchers.IO) {
                    repository.getAllTimetables().firstOrNull() ?: emptyList()
                }
                if (timetables.isEmpty()) throw IllegalStateException("未找到可导出的课表数据")
                val targets = resolveExportTargets(scope, timetables)
                if (scope == ExportScope.CURRENT && targets.isEmpty()) {
                    throw IllegalStateException("当前日期未处于任何有效学期内")
                }
                when (target) {
                    ExportTarget.PHONE_APP -> sendToPhone(context, format, targets)
                    ExportTarget.DOWNLOAD -> saveToUri(context, uri ?: error("请选择保存位置"), format, targets)
                    ExportTarget.BOTH -> {
                        val localUri = uri ?: error("请选择保存位置")
                        sendToPhone(context, format, targets)
                        saveToUri(context, localUri, format, targets)
                    }
                }
                _state.value = ExportState.Success(
                    when (target) {
                        ExportTarget.PHONE_APP -> "手机导出请求已提交"
                        ExportTarget.DOWNLOAD -> "本地导出完成"
                        ExportTarget.BOTH -> "手机发送与本地保存均已完成"
                    }
                )
            } catch (e: Exception) {
                _state.value = ExportState.Error(e.message ?: "导出失败")
            }
        }
    }

    private suspend fun sendToPhone(context: Context, format: ExportFormat, targets: List<Timetable>) {
        val transferFormat = when (format) {
            ExportFormat.ICS -> ExportFormatForPhone.ICS
            ExportFormat.CSV -> ExportFormatForPhone.CSV
            ExportFormat.JSON_BACKUP -> ExportFormatForPhone.JSON_BACKUP
        }
        withContext(Dispatchers.IO) {
            WearFileTransfer.exportToPhone(context, transferFormat, targets)
        }
    }

    private suspend fun saveToUri(context: Context, uri: Uri, format: ExportFormat, targets: List<Timetable>) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                when (format) {
                    ExportFormat.ICS -> IcsExporter.streamIcs(output, targets)
                    ExportFormat.CSV -> CsvExporter.streamCsv(output, targets)
                    ExportFormat.JSON_BACKUP -> BackupManager.backup(output, targets)
                }
            } ?: throw IllegalStateException("无法打开导出文件")
        }
    }

    fun resetState() { _state.value = ExportState.Idle }
}
