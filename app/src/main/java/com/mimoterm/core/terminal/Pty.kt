package com.mimoterm.core.terminal

import android.util.Log
import java.io.*

class Pty {
    companion object {
        private const val TAG = "Pty"
    }

    private var process: Process? = null
    var inputStream: InputStream? = null
        private set
    var outputStream: OutputStream? = null
        private set
    var errorStream: InputStream? = null
        private set

    var isRunning: Boolean = false
        private set

    fun open(command: String, cwd: String? = null): Boolean {
        return try {
            val pb = ProcessBuilder(command.split(" "))
            cwd?.let { pb.directory(File(it)) }
            pb.redirectErrorStream(false)

            process = pb.start()
            inputStream = process!!.inputStream
            outputStream = process!!.outputStream
            errorStream = process!!.errorStream
            isRunning = true

            Log.i(TAG, "Process started: $command")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start process: ${e.message}")
            false
        }
    }

    fun write(data: ByteArray) {
        try {
            outputStream?.write(data)
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Write failed: ${e.message}")
        }
    }

    fun write(text: String) {
        write(text.toByteArray())
    }

    fun read(): Int {
        return try {
            inputStream?.read() ?: -1
        } catch (e: IOException) {
            -1
        }
    }

    fun read(buffer: ByteArray): Int {
        return try {
            inputStream?.read(buffer) ?: -1
        } catch (e: IOException) {
            -1
        }
    }

    fun readError(): String? {
        return try {
            errorStream?.bufferedReader()?.readText()
        } catch (e: IOException) {
            null
        }
    }

    fun close() {
        try {
            outputStream?.close()
            inputStream?.close()
            errorStream?.close()
            process?.destroy()
            isRunning = false
        } catch (e: Exception) {
            Log.e(TAG, "Close failed: ${e.message}")
        }
    }

    fun waitFor(): Int {
        return try {
            process?.waitFor() ?: -1
        } catch (e: InterruptedException) {
            -1
        }
    }
}
