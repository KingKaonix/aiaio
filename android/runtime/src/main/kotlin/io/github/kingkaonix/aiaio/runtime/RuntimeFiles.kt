package io.github.kingkaonix.aiaio.runtime

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OTHERS_READ
import java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE
import java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE
import java.nio.file.attribute.PosixFilePermission.GROUP_READ
import java.nio.file.attribute.PosixFilePermission.GROUP_WRITE
import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE

/**
 * File operations executed in the shell-UID service. Every path goes through [RuntimePaths]
 * first; the app never gets a raw filesystem handle across the AIDL boundary.
 */
internal object RuntimeFiles {

    fun mkdirs(path: String): String {
        val dir = RuntimePaths.resolve(path)
        if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) {
            error("mkdirs failed: ${dir.path}")
        }
        return RuntimeResult.ok(dir.path)
    }

    fun writeFile(path: String, data: ByteArray, mode: Int): String {
        val file = RuntimePaths.resolve(path)
        val parent = file.parentFile
        if (parent != null && !parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) {
            error("cannot create parent directory: ${parent.path}")
        }
        file.writeBytes(data)
        Files.setPosixFilePermissions(file.toPath(), permissions(mode))
        return RuntimeResult.ok("${file.path} mode=${Integer.toOctalString(mode)} bytes=${data.size}")
    }

    /** Maps a Unix mode to POSIX permissions; only the low nine bits are meaningful. */
    fun permissions(mode: Int): Set<PosixFilePermission> = buildSet {
        if (mode and 0x100 != 0) add(OWNER_READ)
        if (mode and 0x80 != 0) add(OWNER_WRITE)
        if (mode and 0x40 != 0) add(OWNER_EXECUTE)
        if (mode and 0x20 != 0) add(GROUP_READ)
        if (mode and 0x10 != 0) add(GROUP_WRITE)
        if (mode and 0x8 != 0) add(GROUP_EXECUTE)
        if (mode and 0x4 != 0) add(OTHERS_READ)
        if (mode and 0x2 != 0) add(OTHERS_WRITE)
        if (mode and 0x1 != 0) add(OTHERS_EXECUTE)
    }
}
