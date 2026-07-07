package com.mimoterm.core.terminal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object TerminalRenderer {
    val monospaceFamily = FontFamily.Monospace
    val fontSize = 14.sp

    fun DrawScope.renderTerminal(
        emulator: TerminalEmulator,
        textMeasurer: TextMeasurer,
        backgroundColor: Color = Color(0xFF1E1E1E)
    ) {
        drawRect(
            color = backgroundColor,
            topLeft = Offset.Zero,
            size = Size(size.width, size.height)
        )

        val rows = emulator.getVisibleRows().size
        val cols = if (emulator.screenBuffer.isNotEmpty()) emulator.screenBuffer[0].columns.size else 80
        val charWidth = size.width / cols
        val charHeight = size.height / rows

        for (row in 0 until minOf(rows, emulator.screenBuffer.size)) {
            val terminalRow = emulator.screenBuffer[row]
            for (col in 0 until minOf(cols, terminalRow.columns.size)) {
                val terminalChar = terminalRow.columns[col]
                val bgColor = Color(terminalChar.style.background)
                val fgColor = Color(terminalChar.style.foreground)

                if (terminalChar.char == ' ' && bgColor == Color.Transparent) continue

                val x = col * charWidth
                val y = row * charHeight

                if (bgColor != Color.Transparent) {
                    drawRect(
                        color = bgColor,
                        topLeft = Offset(x, y),
                        size = Size(charWidth, charHeight)
                    )
                }

                if (terminalChar.char != ' ') {
                    val style = TextStyle(
                        color = fgColor,
                        fontSize = fontSize,
                        fontFamily = monospaceFamily,
                        fontWeight = if (terminalChar.style.bold) FontWeight.Bold else FontWeight.Normal
                    )

                    val textLayoutResult = textMeasurer.measure(
                        text = terminalChar.char.toString(),
                        style = style
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(x + 1, y + 1)
                    )

                    if (terminalChar.style.underline) {
                        drawLine(
                            color = fgColor,
                            start = Offset(x, y + charHeight - 2),
                            end = Offset(x + charWidth, y + charHeight - 2),
                            strokeWidth = 1f
                        )
                    }

                    if (terminalChar.style.strikethrough) {
                        drawLine(
                            color = fgColor,
                            start = Offset(x, y + charHeight / 2),
                            end = Offset(x + charWidth, y + charHeight / 2),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }

        if (emulator.cursorVisible) {
            val cursorX = emulator.cursorCol * charWidth
            val cursorY = emulator.cursorRow * charHeight
            drawRect(
                color = Color(0xFF00FF00).copy(alpha = 0.7f),
                topLeft = Offset(cursorX, cursorY),
                size = Size(charWidth, charHeight),
                style = Fill
            )
        }
    }
}
