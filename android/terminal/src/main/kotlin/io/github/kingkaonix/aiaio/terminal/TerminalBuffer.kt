package io.github.kingkaonix.aiaio.terminal

/**
 * Cell grid for the VT screen. Phase 3 adds per-cell attributes (fg/bg/bold/underline),
 * dirty regions, scrollback and reflow; Phase 0 only needs characters, cursor movement and
 * erase operations to validate the parser loop.
 */
class TerminalBuffer(
    var cols: Int,
    var rows: Int,
) {
    private var grid: Array<CharArray> = Array(rows) { CharArray(cols) { ' ' } }

    var cursorX: Int = 0
        private set
    var cursorY: Int = 0
        private set

    fun putChar(char: Char) {
        if (cursorX >= cols) {
            cursorX = 0
            lineFeed()
        }
        grid[cursorY][cursorX] = char
        cursorX++
    }

    fun carriageReturn() {
        cursorX = 0
    }

    fun lineFeed() {
        if (cursorY == rows - 1) {
            scrollUp()
        } else {
            cursorY++
        }
    }

    fun backspace() {
        if (cursorX > 0) cursorX--
    }

    fun tab(stop: Int = 8) {
        if (stop <= 0) return
        val next = ((cursorX / stop) + 1) * stop
        cursorX = next.coerceAtMost(cols - 1)
    }

    fun setCursor(row: Int, column: Int) {
        cursorY = (row - 1).coerceIn(0, rows - 1)
        cursorX = (column - 1).coerceIn(0, cols - 1)
    }

    fun moveCursor(deltaY: Int, deltaX: Int) {
        cursorY = (cursorY + deltaY).coerceIn(0, rows - 1)
        cursorX = (cursorX + deltaX).coerceIn(0, cols - 1)
    }

    fun eraseInDisplay(mode: Int) {
        when (mode) {
            2, 3 -> {
                grid.forEach { it.fill(' ') }
                cursorX = 0
                cursorY = 0
            }

            0 -> {
                eraseInLine(0)
                for (row in cursorY + 1 until rows) {
                    grid[row].fill(' ')
                }
            }

            1 -> {
                eraseInLine(1)
                for (row in 0 until cursorY) {
                    grid[row].fill(' ')
                }
            }
        }
    }

    fun eraseInLine(mode: Int) {
        val line = grid[cursorY]
        when (mode) {
            0 -> for (column in cursorX until cols) line[column] = ' '
            1 -> for (column in 0..cursorX.coerceAtMost(cols - 1)) line[column] = ' '
            2 -> line.fill(' ')
        }
    }

    fun resize(newCols: Int, newRows: Int) {
        if (newCols == cols && newRows == rows) return
        val resized = Array(newRows) { CharArray(newCols) { ' ' } }
        for (row in 0 until minOf(rows, newRows)) {
            for (column in 0 until minOf(cols, newCols)) {
                resized[row][column] = grid[row][column]
            }
        }
        grid = resized
        cols = newCols
        rows = newRows
        cursorY = cursorY.coerceIn(0, newRows - 1)
        cursorX = cursorX.coerceIn(0, newCols - 1)
    }

    /** One string per row, trailing blanks trimmed. */
    fun snapshot(): List<String> = grid.map { line ->
        val end = line.indexOfLast { it != ' ' }
        if (end < 0) "" else String(line, 0, end + 1)
    }

    private fun scrollUp() {
        for (row in 1 until rows) {
            grid[row - 1] = grid[row]
        }
        grid[rows - 1] = CharArray(cols) { ' ' }
    }
}
