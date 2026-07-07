package com.mimoterm.core.filemanager

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String? = null
) {
    val extension: String get() = name.substringAfterLast('.', "").lowercase()
    val isHidden: Boolean get() = name.startsWith('.')
}

class FileManager(private val context: Context) {
    fun listFiles(path: String): List<FileItem> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles()?.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 0 else file.length(),
                lastModified = file.lastModified()
            )
        }?.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name })
            ?: emptyList()
    }

    fun getStorageRoots(): List<FileItem> {
        val roots = mutableListOf<FileItem>()

        context.getExternalFilesDir(null)?.let {
            roots.add(
                FileItem(
                    name = "Internal Storage",
                    path = it.absolutePath,
                    isDirectory = true,
                    size = 0,
                    lastModified = 0
                )
            )
        }

        Environment.getExternalStorageDirectory()?.let {
            if (it.exists()) {
                roots.add(
                    FileItem(
                        name = "External Storage",
                        path = it.absolutePath,
                        isDirectory = true,
                        size = 0,
                        lastModified = 0
                    )
                )
            }
        }

        return roots
    }

    fun createDirectory(path: String, name: String): Boolean {
        return File(path, name).mkdirs()
    }

    fun deleteFile(path: String): Boolean {
        return File(path).deleteRecursively()
    }

    fun renameFile(path: String, newName: String): Boolean {
        val file = File(path)
        return file.renameTo(File(file.parent, newName))
    }

    fun copyFile(source: String, destination: String): Boolean {
        return try {
            File(source).copyTo(File(destination), overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun moveFile(source: String, destination: String): Boolean {
        return try {
            File(source).copyTo(File(destination), overwrite = true)
            File(source).delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getFile(path: String): FileItem? {
        val file = File(path)
        if (!file.exists()) return null
        return FileItem(
            name = file.name,
            path = file.absolutePath,
            isDirectory = file.isDirectory,
            size = if (file.isDirectory) 0 else file.length(),
            lastModified = file.lastModified()
        )
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
