package com.hufeng943.timetable.presentation.ui.screens.more.settings.importer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.data.LocalBackupManager
import com.hufeng943.timetable.shared.data.repository.TimetableRepository
import com.hufeng943.timetable.shared.importexport.ImportPreview
import com.hufeng943.timetable.shared.importexport.ImportPreviewBuilder
import com.hufeng943.timetable.shared.importexport.TimetableFileParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed interface ImportState {
    object Idle : ImportState
    object Importing : ImportState
    data class Preview(val preview: ImportPreview) : ImportState
    data class Success(val count: Int) : ImportState
    data class Error(val message: String) : ImportState
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importService: com.hufeng943.timetable.shared.importexport.ImportService,
    private val repository: TimetableRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    private val _backupFiles = MutableStateFlow<List<File>>(emptyList())
    val backupFiles: StateFlow<List<File>> = _backupFiles.asStateFlow()

    fun loadBackupFiles(context: Context) {
        viewModelScope.launch {
            _backupFiles.value = withContext(Dispatchers.IO) {
                LocalBackupManager.listBackupFiles(context)
            }
        }
    }

    fun importFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = ImportState.Importing

            try {
                val timetables = withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver
                        .openInputStream(uri)
                        ?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("无法读取所选文件")

                    TimetableFileParser.parse(bytes)
                }

                showPreview(timetables)
            } catch (e: Exception) {
                _state.value = ImportState.Error(
                    e.message ?: "导入失败"
                )
            }
        }
    }

    fun importFromFile(file: File) {
        viewModelScope.launch {
            _state.value = ImportState.Importing

            try {
                val timetables = withContext(Dispatchers.IO) {
                    TimetableFileParser.parse(file.readBytes())
                }

                showPreview(timetables)
            } catch (e: Exception) {
                _state.value = ImportState.Error(
                    e.message ?: "导入失败"
                )
            }
        }
    }

    private suspend fun showPreview(timetables: List<com.hufeng943.timetable.shared.model.Timetable>) {
        val existing = repository.getAllTimetables().first()
        _state.value = ImportState.Preview(ImportPreviewBuilder.build(timetables, existing))
    }

    fun confirmImport(replaceExisting: Boolean) {
        val preview = (_state.value as? ImportState.Preview)?.preview ?: return
        val selected = preview.selected(replaceExisting)
        if (selected.isEmpty()) {
            _state.value = ImportState.Idle
            return
        }
        viewModelScope.launch {
            _state.value = ImportState.Importing
            runCatching {
                withContext(Dispatchers.IO) { importService.importReplacingMatchesAtomic(selected) }
            }.onSuccess { _state.value = ImportState.Success(selected.size) }
                .onFailure { _state.value = ImportState.Error(it.message ?: "导入失败") }
        }
    }

    fun resetState() {
        _state.value = ImportState.Idle
    }
}
