package com.hufeng943.timetable.presentation.ui.screens.more.settings.export

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.data.ExportFormatForPhone
import com.hufeng943.timetable.data.WearFileTransfer
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
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
    data object Idle : ExportState
    data object Exporting : ExportState
    data class Success(val message: String) : ExportState
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

    fun exportWithTarget(
        context: Context,
        target: ExportTarget,
        format: ExportFormat,
        scope: ExportScope,
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
                sendToPhone(context, target, format, targets)
                _state.value = ExportState.Success(
                    when (target) {
                        ExportTarget.PHONE_APP -> "已发送到手机 App"
                        ExportTarget.PHONE_FILE -> "已请求保存文件到手机"
                        ExportTarget.BOTH -> "已请求导入手机 App 并保存文件到手机"
                    }
                )
            } catch (e: Exception) {
                _state.value = ExportState.Error(e.message ?: "导出失败")
            }
        }
    }

    private suspend fun sendToPhone(
        context: Context,
        target: ExportTarget,
        format: ExportFormat,
        timetables: List<Timetable>,
    ) {
        val transferFormat = when (format) {
            ExportFormat.ICS -> ExportFormatForPhone.ICS
            ExportFormat.CSV -> ExportFormatForPhone.CSV
            ExportFormat.JSON_BACKUP -> ExportFormatForPhone.JSON_BACKUP
        }
        withContext(Dispatchers.IO) {
            WearFileTransfer.exportToPhone(context, transferFormat, target, timetables)
        }
    }

    fun resetState() { _state.value = ExportState.Idle }
}
