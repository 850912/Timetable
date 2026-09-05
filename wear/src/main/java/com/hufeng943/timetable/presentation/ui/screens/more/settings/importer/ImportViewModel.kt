package com.hufeng943.timetable.presentation.ui.screens.more.settings.importer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufeng943.timetable.data.LocalBackupManager
import com.hufeng943.timetable.shared.export.BackupManager
import com.hufeng943.timetable.shared.export.CsvImporter
import com.hufeng943.timetable.shared.export.IcsImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromString
import java.io.ByteArrayInputStream
import java.io.File
import javax.inject.Inject

sealed interface ImportState {
    object Idle : ImportState
    object Importing : ImportState
    data class Success(val count: Int) : ImportState
    data class Error(val message: String) : ImportState
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importService: com.hufeng943.timetable.shared.importexport.ImportService
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

                    parseBytes(bytes)
                }

                importTimetables(timetables)
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
                    parseBytes(file.readBytes())
                }

                importTimetables(timetables)
            } catch (e: Exception) {
                _state.value = ImportState.Error(
                    e.message ?: "导入失败"
                )
            }
        }
    }

    private suspend fun importTimetables(
        timetables: List<com.hufeng943.timetable.shared.model.Timetable>
    ) {
        if (timetables.isEmpty()) {
            throw IllegalStateException("未在文件中解析到有效课程")
        }

        importService.importAtomic(timetables)
        _state.value = ImportState.Success(timetables.size)
    }

    private fun parseBytes(bytes: ByteArray): List<com.hufeng943.timetable.shared.model.Timetable> {
        val content = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val trimmed = content.trimStart()

        return when {
            trimmed.startsWith("{") || trimmed.startsWith("[") -> parseJsonBackup(content, bytes)

            content.contains("BEGIN:VCALENDAR", ignoreCase = true) -> {
                IcsImporter.parseIcs(content)
            }

            content.contains("学期,课程名称") || content.contains("星期") -> {
                CsvImporter.parseCsv(content)
            }

            else -> {
                throw IllegalArgumentException(
                    "无法识别的文件格式，仅支持 .json/.ics/.csv"
                )
            }
        }
    }

    private fun parseJsonBackup(
        content: String,
        bytes: ByteArray
    ): List<com.hufeng943.timetable.shared.model.Timetable> {
        val json = Json { ignoreUnknownKeys = true }

        // 2.0.0 格式：版本化容器，内部可以包含多个课表。
        runCatching { BackupManager.restore(ByteArrayInputStream(bytes)) }
            .getOrNull()
            ?.let { return it }

        // 兼容早期直接导出的 Timetable 数组。
        runCatching {
            json.decodeFromString<List<com.hufeng943.timetable.shared.model.Timetable>>(content)
        }.getOrNull()?.let { return it }

        // 再兼容早期只保存单个 Timetable 对象的格式。
        runCatching {
            json.decodeFromString<com.hufeng943.timetable.shared.model.Timetable>(content)
        }.getOrNull()?.let { return listOf(it) }

        throw IllegalArgumentException("JSON 课表文件格式不受支持或文件已损坏")
    }

    fun resetState() {
        _state.value = ImportState.Idle
    }
}
