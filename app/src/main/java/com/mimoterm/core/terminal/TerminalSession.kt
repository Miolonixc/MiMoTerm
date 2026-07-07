package com.mimoterm.core.terminal

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TerminalSession(
    private val command: String = "/system/bin/sh",
    private val cwd: String? = null,
    private val rows: Int = 24,
    private val cols: Int = 80
) {
    private val pty = Pty()
    val emulator = TerminalEmulator(cols, rows)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var readThread: Job? = null
    private var waitThread: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(): Boolean {
        if (_isRunning.value) return false

        val success = pty.open(command, cwd)
        if (!success) return false

        _isRunning.value = true

        // Reader thread - reads from process stdout/stderr
        readThread = scope.launch {
            val buffer = ByteArray(4096)
            while (isActive && _isRunning.value) {
                val bytesRead = pty.read(buffer)
                if (bytesRead > 0) {
                    val data = buffer.copyOf(bytesRead)
                    withContext(Dispatchers.Main) {
                        emulator.processBytes(data, bytesRead)
                    }
                } else if (bytesRead == -1) {
                    break
                }
            }
            _isRunning.value = false
        }

        // Wait for process to exit
        waitThread = scope.launch {
            pty.waitFor()
            _isRunning.value = false
        }

        return true
    }

    fun sendCommand(command: String) {
        if (!_isRunning.value) return
        scope.launch {
            pty.write(command + "\n")
        }
    }

    fun sendBytes(data: ByteArray) {
        if (!_isRunning.value) return
        scope.launch {
            pty.write(data)
        }
    }

    fun sendText(text: String) {
        sendBytes(text.toByteArray())
    }

    fun resize(newRows: Int, newCols: Int) {
        emulator.resize(newRows, newCols)
    }

    fun stop() {
        _isRunning.value = false
        readThread?.cancel()
        waitThread?.cancel()
        pty.close()
        scope.cancel()
    }
}
