package io.github.kingkaonix.aiaio.terminal

/** Byte transport for a terminal session. Implemented by the PTY client in Phase 3. */
interface TerminalTransport {
    fun write(bytes: ByteArray)

    fun resize(cols: Int, rows: Int)

    fun close()
}

/**
 * Binds a [TerminalBuffer] to a transport. Output bytes are decoded and fed to the parser;
 * input is passed through to the PTY.
 */
class TerminalSession(
    val buffer: TerminalBuffer,
    private val transport: TerminalTransport?,
) {
    private val parser = TerminalParser(buffer)

    /** For tests and recorded-stream replay. */
    fun feed(text: CharSequence) = parser.feed(text)

    fun onOutput(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size) {
        // TODO(phase-3): use an incremental UTF-8 decoder so multi-byte chars split across
        // reads are not replaced. Fine for Phase 0 probes.
        parser.feed(String(bytes, offset, length, Charsets.UTF_8))
    }

    fun send(text: String) {
        transport?.write(text.toByteArray(Charsets.UTF_8))
    }

    fun sendBytes(bytes: ByteArray) {
        transport?.write(bytes)
    }

    fun resize(cols: Int, rows: Int) {
        buffer.resize(cols, rows)
        transport?.resize(cols, rows)
    }

    fun close() {
        transport?.close()
    }
}
