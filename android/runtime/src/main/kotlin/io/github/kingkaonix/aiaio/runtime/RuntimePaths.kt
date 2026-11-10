package io.github.kingkaonix.aiaio.runtime

import java.io.File

/**
 * Path policy for the shell-UID service.
 *
 * The service runs as uid 2000 and can write almost anywhere, so every path that crosses
 * AIDL is resolved and confined to [ROOT] first. Symlinks and `..` are resolved with
 * [File.canonicalFile]; relative paths are interpreted under [ROOT].
 */
object RuntimePaths {
    const val ROOT: String = "/data/local/tmp/aiaio"

    fun resolve(path: String): File {
        require(path.isNotBlank()) { "empty path" }
        val root = File(ROOT).canonicalFile
        val candidate = File(path)
        val target = (if (candidate.isAbsolute) candidate else File(root, path)).canonicalFile
        val rootPrefix = root.path + File.separator
        require(target == root || target.path.startsWith(rootPrefix)) {
            "path escapes $ROOT: $path"
        }
        return target
    }
}
