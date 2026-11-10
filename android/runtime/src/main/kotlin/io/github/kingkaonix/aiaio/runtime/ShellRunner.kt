package io.github.kingkaonix.aiaio.runtime

import java.io.File
import org.json.JSONObject

/**
 * Minimal process runner for the shell-UID service. Phase 0 intentionally has no shell
 * interpolation: callers pass an argv array. The path allow-list in [RuntimePaths] governs
 * what the service is asked to touch; a read-only runtime root comes with T02/T03.
 */
internal object ShellRunner {
    fun run(argv: Array<out String>, cwd: String): String {
        if (argv.isEmpty()) {
            return RuntimeResult.error("empty argv")
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
            RuntimeResult.error(t.message ?: t.javaClass.name)
        }
    }
}
