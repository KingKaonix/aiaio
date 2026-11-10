package io.github.kingkaonix.aiaio.runtime

import java.io.File
import org.json.JSONObject

/**
 * Minimal process runner for the shell-UID service. Phase 0 intentionally has no shell
 * interpolation: callers pass an argv array. A path allow-list and read-only root come with
 * T02/T03, before any user-controlled path reaches this code.
 */
internal object ShellRunner {
    fun run(argv: Array<String>, cwd: String): String {
        if (argv.isEmpty()) {
            return error("empty argv")
        }

        return try {
            val builder = ProcessBuilder(argv.toList()).redirectErrorStream(true)
            val directory = cwd.takeIf { it.isNotBlank() }?.let(::File)
            if (directory != null && directory.isDirectory) {
                builder.directory(directory)
            }

            val process = builder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()

            JSONObject()
                .put("code", code)
                .put("output", output)
                .toString()
        } catch (t: Throwable) {
            error(t.message ?: t.javaClass.name)
        }
    }

    private fun error(message: String): String =
        JSONObject()
            .put("code", -1)
            .put("error", message)
            .toString()
}
