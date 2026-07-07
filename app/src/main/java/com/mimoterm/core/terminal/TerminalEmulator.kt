package com.mimoterm.core.terminal

import android.graphics.Color

data class TextStyle(
    val foreground: Int = Color.WHITE,
    val background: Int = Color.TRANSPARENT,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false
)

data class TerminalChar(
    val char: Char = ' ',
    val style: TextStyle = TextStyle()
)

data class TerminalRow(
    val columns: MutableList<TerminalChar> = MutableList(80) { TerminalChar() }
)

class TerminalEmulator(
    private var cols: Int = 80,
    private var rows: Int = 24
) {
    val screenBuffer: MutableList<TerminalRow> = MutableList(rows) { TerminalRow(MutableList(cols) { TerminalChar() }) }

    var cursorRow: Int = 0
        private set
    var cursorCol: Int = 0
        private set
    var cursorVisible: Boolean = true
        private set

    private var currentStyle = TextStyle()
    private var scrollRegionTop = 0
    private var scrollRegionBottom = rows - 1

    // ANSI color palette (standard 16 colors)
    private val ansiColors = intArrayOf(
        Color.rgb(0, 0, 0),         // Black
        Color.rgb(170, 0, 0),       // Red
        Color.rgb(0, 170, 0),       // Green
        Color.rgb(170, 85, 0),      // Yellow
        Color.rgb(0, 0, 170),       // Blue
        Color.rgb(170, 0, 170),     // Magenta
        Color.rgb(0, 170, 170),     // Cyan
        Color.rgb(170, 170, 170),   // White
        Color.rgb(85, 85, 85),      // Bright Black
        Color.rgb(255, 85, 85),     // Bright Red
        Color.rgb(85, 255, 85),     // Bright Green
        Color.rgb(255, 255, 85),    // Bright Yellow
        Color.rgb(85, 85, 255),     // Bright Blue
        Color.rgb(255, 85, 255),    // Bright Magenta
        Color.rgb(85, 255, 255),    // Bright Cyan
        Color.rgb(255, 255, 255)    // Bright White
    )

    fun resize(newRows: Int, newCols: Int) {
        val oldBuffer = screenBuffer.toList()
        rows = newRows
        cols = newCols
        screenBuffer.clear()
        repeat(rows) { r ->
            val row = TerminalRow(MutableList(cols) { TerminalChar() })
            if (r < oldBuffer.size) {
                val oldRow = oldBuffer[r]
                for (c in 0 until minOf(cols, oldRow.columns.size)) {
                    row.columns[c] = oldRow.columns[c]
                }
            }
            screenBuffer.add(row)
        }
        scrollRegionBottom = rows - 1
        cursorRow = minOf(cursorRow, rows - 1)
        cursorCol = minOf(cursorCol, cols - 1)
    }

    fun processBytes(data: ByteArray, length: Int) {
        var i = 0
        while (i < length) {
            val b = data[i].toInt() and 0xFF
            when {
                b == 0x1B -> {
                    // ESC sequence
                    i = processEscapeSequence(data, i + 1, length)
                }
                b == 0x08 -> {
                    // Backspace
                    if (cursorCol > 0) cursorCol--
                }
                b == 0x09 -> {
                    // Tab
                    cursorCol = minOf((cursorCol / 8 + 1) * 8, cols - 1)
                }
                b == 0x0A || b == 0x0B || b == 0x0C -> {
                    // Line feed
                    lineFeed()
                }
                b == 0x0D -> {
                    // Carriage return
                    cursorCol = 0
                }
                b == 0x07 -> {
                    // Bell - ignore
                }
                b == 0x00 -> {
                    // Null - ignore
                }
                else -> {
                    // Regular character
                    if (cursorCol < cols) {
                        screenBuffer[cursorRow].columns[cursorCol] = TerminalChar(
                            char = b.toChar(),
                            style = currentStyle
                        )
                        cursorCol++
                        if (cursorCol >= cols) {
                            cursorCol = 0
                            lineFeed()
                        }
                    }
                }
            }
            i++
        }
    }

    private fun processEscapeSequence(data: ByteArray, start: Int, length: Int): Int {
        if (start >= length) return start

        var i = start
        val b = data[i].toInt() and 0xFF

        when (b) {
            '[' -> {
                // CSI sequence
                i = processCsiSequence(data, i + 1, length)
            }
            ']' -> {
                // OSC sequence - skip until BEL or ST
                while (i < length) {
                    if (data[i] == 0x07.toByte()) {
                        i++
                        break
                    }
                    i++
                }
            }
            '(' -> {
                // Character set - skip next char
                if (i + 1 < length) i += 2
            }
            ')' -> {
                if (i + 1 < length) i += 2
            }
            'M' -> {
                // Reverse index
                if (cursorRow > 0) {
                    cursorRow--
                } else {
                    scrollDown()
                }
            }
            'D' -> {
                // Index (line feed)
                lineFeed()
                i++
            }
            'E' -> {
                // Next line
                cursorCol = 0
                lineFeed()
                i++
            }
            '7' -> {
                // Save cursor - simplified
                i++
            }
            '8' -> {
                // Restore cursor - simplified
                i++
            }
            'c' -> {
                // Reset
                reset()
                i++
            }
        }

        return i
    }

    private fun processCsiSequence(data: ByteArray, start: Int, length: Int): Int {
        var i = start
        val params = mutableListOf<Int>()
        var currentParam = 0
        var hasParam = false

        // Parse parameters
        while (i < length) {
            val b = data[i].toInt() and 0xFF
            when {
                b in '0'.code..'9'.code -> {
                    currentParam = currentParam * 10 + (b - '0'.code)
                    hasParam = true
                }
                b == ';'.code -> {
                    params.add(if (hasParam) currentParam else 0)
                    currentParam = 0
                    hasParam = false
                }
                else -> break
            }
            i++
        }

        if (hasParam) params.add(currentParam)
        if (params.isEmpty()) params.add(0)

        val command = if (i < length) data[i].toInt() and 0xFF else 0

        when (command) {
            'A'.code -> {
                // Cursor Up
                cursorRow = maxOf(0, cursorRow - params[0].coerceAtLeast(1))
            }
            'B'.code -> {
                // Cursor Down
                cursorRow = minOf(rows - 1, cursorRow + params[0].coerceAtLeast(1))
            }
            'C'.code -> {
                // Cursor Forward
                cursorCol = minOf(cols - 1, cursorCol + params[0].coerceAtLeast(1))
            }
            'D'.code -> {
                // Cursor Back
                cursorCol = maxOf(0, cursorCol - params[0].coerceAtLeast(1))
            }
            'E'.code -> {
                // Cursor Next Line
                cursorCol = 0
                cursorRow = minOf(rows - 1, cursorRow + params[0].coerceAtLeast(1))
            }
            'F'.code -> {
                // Cursor Previous Line
                cursorCol = 0
                cursorRow = maxOf(0, cursorRow - params[0].coerceAtLeast(1))
            }
            'G'.code -> {
                // Cursor Horizontal Absolute
                cursorCol = (params[0].coerceAtLeast(1) - 1).coerceIn(0, cols - 1)
            }
            'H'.code, 'f'.code -> {
                // Cursor Position
                cursorRow = (params.getOrElse(0) { 1 }.coerceAtLeast(1) - 1).coerceIn(0, rows - 1)
                cursorCol = (params.getOrElse(1) { 1 }.coerceAtLeast(1) - 1).coerceIn(0, cols - 1)
            }
            'J'.code -> {
                when (params[0]) {
                    0 -> {
                        // Clear from cursor to end
                        for (c in cursorCol until cols) {
                            screenBuffer[cursorRow].columns[c] = TerminalChar()
                        }
                        for (r in cursorRow + 1 until rows) {
                            screenBuffer[r] = TerminalRow(MutableList(cols) { TerminalChar() })
                        }
                    }
                    1 -> {
                        // Clear from start to cursor
                        for (r in 0 until cursorRow) {
                            screenBuffer[r] = TerminalRow(MutableList(cols) { TerminalChar() })
                        }
                        for (c in 0..cursorCol) {
                            screenBuffer[cursorRow].columns[c] = TerminalChar()
                        }
                    }
                    2, 3 -> {
                        // Clear entire screen
                        for (r in 0 until rows) {
                            screenBuffer[r] = TerminalRow(MutableList(cols) { TerminalChar() })
                        }
                        cursorRow = 0
                        cursorCol = 0
                    }
                }
            }
            'K'.code -> {
                when (params[0]) {
                    0 -> {
                        for (c in cursorCol until cols) {
                            screenBuffer[cursorRow].columns[c] = TerminalChar()
                        }
                    }
                    1 -> {
                        for (c in 0..cursorCol) {
                            screenBuffer[cursorRow].columns[c] = TerminalChar()
                        }
                    }
                    2 -> {
                        screenBuffer[cursorRow] = TerminalRow(MutableList(cols) { TerminalChar() })
                    }
                }
            }
            'L'.code -> {
                // Insert Lines
                val count = params[0].coerceAtLeast(1)
                for (i in 0 until count) {
                    scrollUp()
                }
            }
            'M'.code -> {
                // Delete Lines
                val count = params[0].coerceAtLeast(1)
                for (i in 0 until count) {
                    scrollDown()
                }
            }
            'P'.code -> {
                // Delete Characters
                val count = params[0].coerceAtLeast(1)
                val row = screenBuffer[cursorRow]
                for (c in cursorCol until (cols - count).coerceAtLeast(0)) {
                    row.columns[c] = row.columns[c + count]
                }
                for (c in (cols - count).coerceAtLeast(0) until cols) {
                    row.columns[c] = TerminalChar()
                }
            }
            'S'.code -> {
                // Scroll Up
                val count = params[0].coerceAtLeast(1)
                for (i in 0 until count) scrollUp()
            }
            'T'.code -> {
                // Scroll Down
                val count = params[0].coerceAtLeast(1)
                for (i in 0 until count) scrollDown()
            }
            'X'.code -> {
                // Erase Characters
                val count = params[0].coerceAtLeast(1)
                for (c in cursorCol until (cursorCol + count).coerceAtMost(cols)) {
                    screenBuffer[cursorRow].columns[c] = TerminalChar()
                }
            }
            'd'.code -> {
                // Vertical Absolute
                cursorRow = (params[0].coerceAtLeast(1) - 1).coerceIn(0, rows - 1)
            }
            'm'.code -> {
                // SGR - Select Graphic Rendition
                processSgr(params)
            }
            'r'.code -> {
                // Set Scrolling Region
                scrollRegionTop = (params.getOrElse(0) { 1 }.coerceAtLeast(1) - 1).coerceIn(0, rows - 1)
                scrollRegionBottom = (params.getOrElse(1) { rows }.coerceAtLeast(1) - 1).coerceIn(0, rows - 1)
                cursorRow = 0
                cursorCol = 0
            }
            'h'.code, 'l'.code -> {
                // Set/Reset Mode - simplified
            }
            'n'.code -> {
                // Device Status Report - ignore
            }
            'c'.code -> {
                // Device Attributes - ignore
            }
            '@'.code -> {
                // Insert Characters
                val count = params[0].coerceAtLeast(1)
                val row = screenBuffer[cursorRow]
                for (c in (cols - 1) downTo (cursorCol + count)) {
                    row.columns[c] = row.columns[c - count]
                }
                for (c in cursorCol until (cursorCol + count).coerceAtMost(cols)) {
                    row.columns[c] = TerminalChar()
                }
            }
        }

        return i + 1
    }

    private fun processSgr(params: List<Int>) {
        if (params.isEmpty()) return

        var i = 0
        while (i < params.size) {
            when (params[i]) {
                0 -> currentStyle = TextStyle()
                1 -> currentStyle = currentStyle.copy(bold = true)
                2 -> {} // Dim
                3 -> currentStyle = currentStyle.copy(italic = true)
                4 -> currentStyle = currentStyle.copy(underline = true)
                5, 6 -> {} // Blink
                7 -> {
                    // Reverse video
                    val tmp = currentStyle.foreground
                    currentStyle = currentStyle.copy(
                        foreground = currentStyle.background,
                        background = tmp
                    )
                }
                8 -> {} // Hidden
                9 -> currentStyle = currentStyle.copy(strikethrough = true)
                22 -> currentStyle = currentStyle.copy(bold = false)
                23 -> currentStyle = currentStyle.copy(italic = false)
                24 -> currentStyle = currentStyle.copy(underline = false)
                25 -> {} // Not blinking
                27 -> {
                    val tmp = currentStyle.foreground
                    currentStyle = currentStyle.copy(
                        foreground = currentStyle.background,
                        background = tmp
                    )
                }
                28 -> {} // Not hidden
                29 -> currentStyle = currentStyle.copy(strikethrough = false)
                in 30..37 -> {
                    val colorIdx = params[i] - 30
                    currentStyle = currentStyle.copy(
                        foreground = if (currentStyle.bold) ansiColors[colorIdx + 8] else ansiColors[colorIdx]
                    )
                }
                38 -> {
                    // Extended foreground color
                    i++
                    if (i < params.size) {
                        when (params[i]) {
                            5 -> {
                                i++
                                if (i < params.size) {
                                    currentStyle = currentStyle.copy(
                                        foreground = ansiColor256(params[i])
                                    )
                                }
                            }
                            2 -> {
                                i++
                                if (i + 2 < params.size) {
                                    currentStyle = currentStyle.copy(
                                        foreground = Color.rgb(params[i], params[i + 1], params[i + 2])
                                    )
                                    i += 2
                                }
                            }
                        }
                    }
                }
                39 -> currentStyle = currentStyle.copy(foreground = Color.WHITE)
                in 40..47 -> {
                    val colorIdx = params[i] - 40
                    currentStyle = currentStyle.copy(background = ansiColors[colorIdx])
                }
                48 -> {
                    // Extended background color
                    i++
                    if (i < params.size) {
                        when (params[i]) {
                            5 -> {
                                i++
                                if (i < params.size) {
                                    currentStyle = currentStyle.copy(
                                        background = ansiColor256(params[i])
                                    )
                                }
                            }
                            2 -> {
                                i++
                                if (i + 2 < params.size) {
                                    currentStyle = currentStyle.copy(
                                        background = Color.rgb(params[i], params[i + 1], params[i + 2])
                                    )
                                    i += 2
                                }
                            }
                        }
                    }
                }
                49 -> currentStyle = currentStyle.copy(background = Color.TRANSPARENT)
                in 90..97 -> {
                    val colorIdx = params[i] - 90
                    currentStyle = currentStyle.copy(foreground = ansiColors[colorIdx + 8])
                }
                in 100..107 -> {
                    val colorIdx = params[i] - 100
                    currentStyle = currentStyle.copy(background = ansiColors[colorIdx + 8])
                }
            }
            i++
        }
    }

    private fun ansiColor256(n: Int): Int {
        if (n < 16) return ansiColors[n]
        if (n < 232) {
            val idx = n - 16
            val r = idx / 36
            val g = (idx / 6) % 6
            val b = idx % 6
            return Color.rgb(
                if (r == 0) 0 else 55 + r * 40,
                if (g == 0) 0 else 55 + g * 40,
                if (b == 0) 0 else 55 + b * 40
            )
        }
        val gray = 8 + (n - 232) * 10
        return Color.rgb(gray, gray, gray)
    }

    private fun lineFeed() {
        if (cursorRow == scrollRegionBottom) {
            scrollUp()
        } else {
            cursorRow = minOf(cursorRow + 1, rows - 1)
        }
    }

    private fun scrollUp() {
        if (scrollRegionTop == 0 && scrollRegionBottom == rows - 1) {
            screenBuffer.removeAt(0)
            screenBuffer.add(TerminalRow(MutableList(cols) { TerminalChar() }))
        } else {
            screenBuffer.removeAt(scrollRegionTop)
            screenBuffer.add(scrollRegionBottom, TerminalRow(MutableList(cols) { TerminalChar() }))
        }
    }

    private fun scrollDown() {
        if (scrollRegionTop == 0 && scrollRegionBottom == rows - 1) {
            screenBuffer.add(0, TerminalRow(MutableList(cols) { TerminalChar() }))
            screenBuffer.removeAt(rows)
        } else {
            screenBuffer.add(scrollRegionTop, TerminalRow(MutableList(cols) { TerminalChar() }))
            screenBuffer.removeAt(scrollRegionBottom + 1)
        }
    }

    private fun reset() {
        for (r in 0 until rows) {
            screenBuffer[r] = TerminalRow(MutableList(cols) { TerminalChar() })
        }
        cursorRow = 0
        cursorCol = 0
        currentStyle = TextStyle()
        scrollRegionTop = 0
        scrollRegionBottom = rows - 1
    }

    fun getVisibleRows(topRow: Int = 0): List<TerminalRow> {
        return screenBuffer.subList(topRow, minOf(topRow + rows, screenBuffer.size))
    }
}
