package com.hufeng943.timetable.data

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

object LocalBackupManager {

    fun getPreferredBackupDir(context: Context): File {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            runCatching {
                val appBackupDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Timetable"
                )
                if (appBackupDir.exists() || appBackupDir.mkdirs()) {
                    if (appBackupDir.canWrite()) return appBackupDir
                }
            }
        }

        return context.getExternalFilesDir("backups")
            ?.also { it.mkdirs() }
            ?: File(context.filesDir, "backups").also { it.mkdirs() }
    }

    fun listBackupFiles(context: Context): List<File> {
        val dirs = buildList {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(
                    runCatching {
                        File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "Timetable"
                        )
                    }.getOrNull()
                )
            }
            add(context.getExternalFilesDir("backups"))
            add(File(context.filesDir, "backups"))
        }.filterNotNull()

        return dirs
            .asSequence()
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir ->
                (dir.listFiles()?.asSequence() ?: emptySequence())
                    .filter(File::isFile)
                    .filter { file ->
                        when (file.extension.lowercase()) {
                            "json", "ics", "csv" -> true
                            else -> false
                        }
                    }
            }
            .distinctBy { it.absolutePath }
            .sortedByDescending { it.lastModified() }
            .toList()
    }

}
