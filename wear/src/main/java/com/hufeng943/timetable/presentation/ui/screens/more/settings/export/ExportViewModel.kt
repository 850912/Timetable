package com.hufeng943.timetable.presentation.ui.screens.more.settings.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.export.BackupManager
import com.hufeng943.timetable.shared.export.CsvExporter
import com.hufeng943.timetable.shared.export.ExportPreviewStats
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

    private val _previewStats =
        MutableStateFlow<ExportPreviewStats?>(null)
    val previewStats: StateFlow<ExportPreviewStats?> =
        _previewStats.asStateFlow()


    init {
        loadPreview()
    }

    fun loadPreview() {
        updatePreview(ExportScope.CURRENT)
    }

    fun updatePreview(scope: ExportScope) {
        viewModelScope.launch {
            try {
                val timetables = withContext(Dispatchers.IO) {
                    repository.getAllTimetables().firstOrNull() ?: emptyList()
                }

                val targets = resolveExportTargets(scope, timetables)

                _previewStats.value = withContext(Dispatchers.Default) {
                    IcsExporter.calculateStats(targets)
                }
            } catch (_: Exception) {
                _previewStats.value = null
            }
        }
    }

    private fun resolveCurrentTimetable(
        list: List<Timetable>
    ): Timetable? {
        if (list.isEmpty()) return null

        val javaNow = java.time.LocalDate.now()
        val today = LocalDate(
            javaNow.year,
            javaNow.monthValue,
            javaNow.dayOfMonth
        )

        return list
            .filter { timetable ->
                val end = timetable.semesterEnd
                    ?: LocalDate.fromEpochDays(
                        timetable.semesterStart.toEpochDays() + 140
                    )

                today >= timetable.semesterStart &&
                    today <= end
            }
            .maxByOrNull { it.semesterStart }
    }

    private fun resolveExportTargets(
        scope: ExportScope,
        timetables: List<Timetable>
    ): List<Timetable> {
        return when (scope) {
            ExportScope.CURRENT ->
                listOfNotNull(resolveCurrentTimetable(timetables))

            ExportScope.ALL ->
                timetables
        }
    }

    fun executeDirectExport(
        context: Context,
        uri: Uri,
        format: ExportFormat,
        scope: ExportScope
    ) {
        viewModelScope.launch {
            _state.value = ExportState.Exporting

            try {
                val timetables = withContext(Dispatchers.IO) {
                    repository.getAllTimetables().firstOrNull() ?: emptyList()
                }

                if (timetables.isEmpty()) {
                    throw IllegalStateException("未找到可导出的课表数据")
                }

                val targets = resolveExportTargets(scope, timetables)

                if (scope == ExportScope.CURRENT && targets.isEmpty()) {
                    throw IllegalStateException("当前日期未处于任何有效学期内")
                }

                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("无法打开导出文件")

                    output.use {
                        when (format) {
                            ExportFormat.ICS ->
                                IcsExporter.streamIcs(it, targets)

                            ExportFormat.CSV ->
                                CsvExporter.streamCsv(it, targets)

                            ExportFormat.JSON_BACKUP ->
                                BackupManager.backup(it, targets)
                        }
                    }
                }

                _state.value = ExportState.Success("导出完成")
            } catch (e: Exception) {
                _state.value =
                    ExportState.Error(e.message ?: "导出失败")
            }
        }
    }

    fun resetState() {
        _state.value = ExportState.Idle
    }
}
