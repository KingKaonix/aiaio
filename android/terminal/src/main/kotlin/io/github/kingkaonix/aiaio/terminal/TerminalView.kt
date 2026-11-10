package io.github.kingkaonix.aiaio.terminal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TerminalBackground = Color(0xFF0B0E14)
private val TerminalForeground = Color(0xFFE6E6E6)

/**
 * Phase 0 renderer: one `drawText` per visible row, trimmed. Phase 3 replaces this with a
 * cell/glyph-cache renderer driven by dirty regions, but the public shape stays the same.
 */
@Composable
fun TerminalView(
    buffer: TerminalBuffer,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
) {
    val textMeasurer = rememberTextMeasurer()
    val style = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize,
        color = TerminalForeground,
    )

    Canvas(modifier = modifier.background(TerminalBackground)) {
        val padding = 8.dp.toPx()
        val lineHeight = fontSize.toPx() * 1.25f
        val lines = buffer.snapshot()

        lines.forEachIndexed { index, line ->
            if (line.isNotEmpty()) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = line,
                    topLeft = Offset(padding, padding + index * lineHeight),
                    style = style,
                )
            }
        }
    }
}
