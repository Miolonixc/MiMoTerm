package com.mimoterm.core.filemanager

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class FileShareHelper(private val context: Context) {
    fun shareFile(file: File, mimeType: String = "*/*") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share file via"))
    }

    fun shareText(text: String, title: String = "Share") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun openFile(file: File, mimeType: String? = null) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: getMimeType(file.name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // No app can handle this file type
        }
    }

    private fun getMimeType(filename: String): String {
        val ext = filename.substringAfterLast('.', '').lowercase()
        return when (ext) {
            "txt" -> "text/plain"
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "zip" -> "application/zip"
            "json" -> "application/json"
            "xml" -> "text/xml"
            "html" -> "text/html"
            "kt", "java" -> "text/plain"
            else -> "*/*"
        }
    }
}
