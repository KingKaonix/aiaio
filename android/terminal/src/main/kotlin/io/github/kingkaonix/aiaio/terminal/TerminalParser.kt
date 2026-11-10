package io.github.kingkaonix.aiaio.terminal

/**
 * ECMA-48 / xterm subset parser. Phase 0 covers what the terminal view needs to prove the
 * pipeline; Phase 3 adds SGR attributes, alternate screen, scroll regions, mouse reporting,
 * bracketed paste and OSC 8/52 handling.
 *
 * The parser is deliberately allocation-light: state is held in fields and CSI parameters
 * accumulate in a reusable builder.
 */
class TerminalParser(private val buffer: TerminalBuffer) {

    private enum class State { GROUND, ESCAPE, CSI, OSC, OSC_ESCAPE }

    private var state = State.GROUND
    private val params = StringBuilder()

    fun feed(text: CharSequence) {
        for (index in text.indices) {
            consume(text[index])
        }
    }

    private fun consume(char: Char) {
        when (state) {
            State.GROUND -> ground(char)
            State.ESCAPE -> escape(char)
            State.CSI -> csi(char)
            State.OSC -> osc(char)
            State.OSC_ESCAPE -> oscEscape(char)
        }
    }

    private fun ground(char: Char) {
        when (char) {
            '\u001b' -> state = State.ESCAPE
            '\r' -> buffer.carriageReturn()
            '\n' -> buffer.lineFeed()
            '\b' -> buffer.backspace()
            '\t' -> buffer.tab()
            '\u0007' -> Unit // bell
            else -> if (char.code >= 0x20) buffer.putChar(char)
        }
    }

    private fun escape(char: Char) {
        when (char) {
            '[' -> {
                params.setLength(0)
                state = State.CSI
            }

            ']' -> state = State.OSC
            else -> state = State.GROUND
        }
    }

    private fun csi(char: Char) {
        if (char in '@'..'~') {
            applyCsi(params.toString(), char)
            state = State.GROUND
        } else {
            params.append(char)
        }
    }

    private fun osc(char: Char) {
        when (char) {
            '\u0007' -> state = State.GROUND
            '\u001b' -> state = State.OSC_ESCAPE
            else -> Unit
        }
    }

    private fun oscEscape(char: Char) {
        state = if (char == '\\') State.GROUND else State.OSC
    }

    private fun applyCsi(raw: String, final: Char) {
        val private = raw.startsWith("?")
        val body = if (private) raw.substring(1) else raw

        when (final) {
            'A' -> buffer.moveCursor(-number(body, 0, 1), 0)
            'B' -> buffer.moveCursor(number(body, 0, 1), 0)
            'C' -> buffer.moveCursor(0, number(body, 0, 1))
            'D' -> buffer.moveCursor(0, -number(body, 0, 1))
            'H', 'f' -> buffer.setCursor(
                row = number(body, 0, 1),
                column = number(body, 1, 1),
            )

            'J' -> buffer.eraseInDisplay(number(body, 0, 0))
            'K' -> buffer.eraseInLine(number(body, 0, 0))
            // 'm' (SGR), 'h'/'l' (modes) and the rest are accepted and ignored until Phase 3.
            else -> Unit
        }
    }

    private fun number(body: String, index: Int, default: Int): Int {
        if (body.isEmpty()) return default
        val parts = body.split(';')
        val token = parts.getOrNull(index)?.substringBefore(':').orEmpty()
        return token.toIntOrNull() ?: default
    }
}
