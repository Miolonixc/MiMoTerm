package com.mimoterm.core.terminal

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

object TerminalRenderer {
    val monospaceFamily = FontFamily.Monospace
    val fontSize = 14.sp
    val lineHeight = 18.sp

    fun DrawScope.renderTerminal(
        emulator: TerminalEmulator,
        textMeasurer: TextMeasurer,
        backgroundColor: Color = Color(0xFF1E1E1E)
    ) {
        // Draw background
        drawRect(
            color = backgroundColor,
            topLeft = Offset.Zero,
            size = Size(size.width, size.height)
        )

        val charWidth = size.width / emulator.cols
        val charHeight = size.height / emulator.rows

        for (row in 0 until minOf(emulator.rows, emulator.screenBuffer.size)) {
            val terminalRow = emulator.screenBuffer[row]
            for (col in 0 until minOf(emulator.cols, terminalRow.columns.size)) {
                val terminalChar = terminalRow.columns[col]
                if (terminalChar.char == ' ' && terminalChar.style.background == Color.Transparent) {
                    continue
                }

                val x = col * charWidth
                val y = row * charHeight

                // Draw background if set
                if (terminalChar.style.background != Color.Transparent) {
                    drawRect(
                        color = terminalChar.style.background,
                        topLeft = Offset(x, y),
                        size = Size(charWidth, charHeight)
                    )
                }

                // Draw character
                if (terminalChar.char != ' ') {
                    val textColor = terminalChar.style.foreground
                    val style = TextStyle(
                        color = textColor,
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

                    // Draw underline
                    if (terminalChar.style.underline) {
                        drawLine(
                            color = textColor,
                            start = Offset(x, y + charHeight - 2),
                            end = Offset(x + charWidth, y + charHeight - 2),
                            strokeWidth = 1f
                        )
                    }

                    // Draw strikethrough
                    if (terminalChar.style.strikethrough) {
                        drawLine(
                            color = textColor,
                            start = Offset(x, y + charHeight / 2),
                            end = Offset(x + charWidth, y + charHeight / 2),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }

        // Draw cursor
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
