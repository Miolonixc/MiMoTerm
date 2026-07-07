package com.mimoterm.core.filemanager

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.*

data class FtpConfig(
    val host: String,
    val port: Int = 21,
    val username: String = "anonymous",
    val password: String = "",
    val path: String = "/"
)

class FtpClient {
    private var client: FTPClient? = null
    private var connected = false

    fun connect(config: FtpConfig): Boolean {
        return try {
            client = FTPClient()
            client!!.connect(config.host, config.port)
            client!!.login(config.username, config.password)
            client!!.enterLocalPassiveMode()
            client!!.setFileType(FTP.BINARY_FILE_TYPE)
            connected = true
            true
        } catch (e: Exception) {
            connected = false
            false
        }
    }

    fun disconnect() {
        client?.disconnect()
        connected = false
    }

    fun listFiles(path: String): List<FileItem> {
        if (!connected) return emptyList()
        return try {
            val files = client!!.listFiles(path)
            files.map { file ->
                FileItem(
                    name = file.name,
                    path = "$path/${file.name}",
                    isDirectory = file.isDirectory,
                    size = file.size,
                    lastModified = file.timestamp.timeInMillis
                )
            }.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name })
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun downloadFile(remotePath: String, localPath: String): Boolean {
        if (!connected) return false
        return try {
            val outputStream = FileOutputStream(localPath)
            val success = client!!.retrieveFile(remotePath, outputStream)
            outputStream.close()
            success
        } catch (e: Exception) {
            false
        }
    }

    fun uploadFile(localPath: String, remotePath: String): Boolean {
        if (!connected) return false
        return try {
            val inputStream = FileInputStream(localPath)
            val success = client!!.storeFile(remotePath, inputStream)
            inputStream.close()
            success
        } catch (e: Exception) {
            false
        }
    }

    fun deleteFile(path: String): Boolean {
        if (!connected) return false
        return try {
            client!!.deleteFile(path)
        } catch (e: Exception) {
            false
        }
    }

    fun createDirectory(path: String): Boolean {
        if (!connected) return false
        return try {
            client!!.makeDirectory(path)
        } catch (e: Exception) {
            false
        }
    }

    fun isConnected(): Boolean = connected
}
