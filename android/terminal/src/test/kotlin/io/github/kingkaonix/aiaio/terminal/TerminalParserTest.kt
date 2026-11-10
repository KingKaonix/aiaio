package io.github.kingkaonix.aiaio.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalParserTest {

    private fun parser(cols: Int = 10, rows: Int = 4): Pair<TerminalBuffer, TerminalParser> {
        val buffer = TerminalBuffer(cols, rows)
        return buffer to TerminalParser(buffer)
    }

    @Test
    fun printsTextAndMovesToNextLine() {
        val (buffer, parser) = parser()
        parser.feed("hello\r\nworld")

        assertEquals(listOf("hello", "world", "", ""), buffer.snapshot())
        assertEquals(5, buffer.cursorX)
        assertEquals(1, buffer.cursorY)
    }

    @Test
    fun carriageReturnOverwrites() {
        val (buffer, parser) = parser()
        parser.feed("abcd\reF")

        assertEquals("eFcd", buffer.snapshot()[0])
    }

    @Test
    fun eraseDisplayClearsEverything() {
        val (buffer, parser) = parser()
        parser.feed("one\r\ntwo\u001b[2J")

        assertEquals(listOf("", "", "", ""), buffer.snapshot())
        assertEquals(0, buffer.cursorX)
        assertEquals(0, buffer.cursorY)
    }

    @Test
    fun cursorPositionIsOneBased() {
        val (buffer, parser) = parser()
        parser.feed("\u001b[2;3Hx")

        assertEquals("  x", buffer.snapshot()[1])
    }

    @Test
    fun eraseToEndOfLine() {
        val (buffer, parser) = parser()
        parser.feed("abcdef\r\u001b[3C\u001b[K")

        assertEquals("abc", buffer.snapshot()[0])
    }

    @Test
    fun wrapsAtColumnLimit() {
        val (buffer, parser) = parser(cols = 4, rows = 3)
        parser.feed("abcde")

        assertEquals(listOf("abcd", "e", ""), buffer.snapshot())
    }

    @Test
    fun scrollsWhenBottomLineFills() {
        val (buffer, parser) = parser(cols = 4, rows = 2)
        parser.feed("a\r\nb\r\nc")

        assertEquals(listOf("b", "c"), buffer.snapshot())
    }

    @Test
    fun ignoresOscTitle() {
        val (buffer, parser) = parser()
        parser.feed("\u001b]0;window title\u0007ok")

        assertEquals("ok", buffer.snapshot()[0])
    }

    @Test
    fun tabAdvancesToNextStop() {
        val (buffer, parser) = parser(cols = 20, rows = 2)
        parser.feed("\tX")

        assertEquals(9, buffer.cursorX)
        assertEquals("X", buffer.snapshot()[0].trim())
    }

    @Test
    fun resizeClampsCursorAndKeepsContent() {
        val (buffer, parser) = parser()
        parser.feed("abcdefghij")
        buffer.resize(5, 2)

        assertEquals(listOf("abcde", ""), buffer.snapshot())
        assertEquals(4, buffer.cursorX)
        assertEquals(0, buffer.cursorY)
    }
}
