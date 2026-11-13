package io.github.kingkaonix.aiaio.runtime

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE
import java.nio.file.attribute.PosixFilePermission.GROUP_READ
import java.nio.file.attribute.PosixFilePermission.GROUP_WRITE
import java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OTHERS_READ
import java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE
import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import org.json.JSONArray
import org.json.JSONObject

internal object RuntimeFiles {
    const val BACKUP_SUFFIX = ".aiaio-bak"
    private const val TEMP_PREFIX = ".aiaio-tmp-"

    private val pathLocks = ConcurrentHashMap<String, ReentrantLock>()

    fun mkdirs(path: String): String {
        val dir = RuntimePaths.resolve(path)
        if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) {
            error("mkdirs failed: ${dir.path}")
        }
        return RuntimeResult.ok(dir.path)
    }

    fun writeFile(path: String, data: ByteArray, mode: Int): String {
        val file = RuntimePaths.resolve(path)
        ensureParent(file)
        file.writeBytes(data)
        Files.setPosixFilePermissions(file.toPath(), permissions(mode))
        return RuntimeResult.ok("${file.path} mode=${Integer.toOctalString(mode)} bytes=${data.size}")
    }

    fun readFile(path: String): String {
        val file = RuntimePaths.resolve(path)
        if (!file.exists()) throw NoSuchElementException("not found: ${file.path}")
        if (file.isDirectory) throw IllegalArgumentException("is a directory: ${file.path}")
        val bytes = file.readBytes()
        return JSONObject()
            .put("code", 0)
            .put("path", file.path)
            .put("data", java.util.Base64.getEncoder().encodeToString(bytes))
            .put("len", bytes.size)
            .toString()
    }

    fun stat(path: String): String {
        val file = RuntimePaths.resolve(path)
        val exists = file.exists()
        val mode = if (exists) modeOf(file) else 0
        return JSONObject()
            .put("code", 0)
            .put("path", file.path)
            .put("exists", exists)
            .put("isFile", file.isFile)
            .put("isDir", file.isDirectory)
            .put("len", if (exists && file.isFile) file.length() else 0L)
            .put("mode", mode)
            .toString()
    }

    fun listDir(path: String): String {
        val dir = RuntimePaths.resolve(path)
        if (!dir.exists()) throw NoSuchElementException("not found: ${dir.path}")
        if (!dir.isDirectory) throw IllegalArgumentException("not a directory: ${dir.path}")
        val entries = JSONArray()
        (dir.listFiles() ?: emptyArray())
            .sortedBy { it.name }
            .forEach { child ->
                entries.put(
                    JSONObject()
                        .put("name", child.name)
                        .put("isDir", child.isDirectory)
                        .put("len", if (child.isFile) child.length() else 0L),
                )
            }
        return JSONObject()
            .put("code", 0)
            .put("path", dir.path)
            .put("entries", entries)
            .toString()
    }

    fun rename(from: String, to: String): String {
        val src = RuntimePaths.resolve(from)
        val dst = RuntimePaths.resolve(to)
        if (!src.exists()) throw NoSuchElementException("not found: ${src.path}")
        ensureParent(dst)
        try {
            Files.move(
                src.toPath(),
                dst.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        syncDir(dst.parentFile)
        return JSONObject().put("code", 0).put("from", src.path).put("to", dst.path).toString()
    }

    fun remove(path: String): String {
        val file = RuntimePaths.resolve(path)
        if (!file.exists()) throw NoSuchElementException("not found: ${file.path}")
        val deleted = file.delete()
        if (!deleted) error("remove failed: ${file.path}")
        return JSONObject().put("code", 0).put("path", file.path).toString()
    }

    fun restoreBackup(path: String): String {
        val file = RuntimePaths.resolve(path)
        val backup = backupFile(file)
        if (!backup.exists()) throw NoSuchElementException("backup not found: ${backup.path}")
        val bytes = backup.readBytes()
        val mode = modeOf(backup)
        writeFileAtomic(file.path, bytes, mode)
        return JSONObject().put("code", 0).put("path", file.path).put("restored", true).toString()
    }

    fun writeFileAtomic(path: String, data: ByteArray, mode: Int): String {
        val file = RuntimePaths.resolve(path)
        ensureParent(file)
        val lock = pathLocks.computeIfAbsent(file.path) { ReentrantLock() }
        lock.lock()
        var temp: File? = null
        try {
            temp = File(file.parentFile, "$TEMP_PREFIX${UUID.randomUUID()}")
            java.io.FileOutputStream(temp).use { out ->
                out.write(data)
                out.flush()
                try {
                    out.fd.sync()
                } catch (_: Throwable) {
                }
            }
            Files.setPosixFilePermissions(temp.toPath(), permissions(mode))

            if (file.exists()) {
                val backup = backupFile(file)
                val backupTemp = File(file.parentFile, "$TEMP_PREFIX${UUID.randomUUID()}")
                try {
                    Files.copy(file.toPath(), backupTemp.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    Files.move(
                        backupTemp.toPath(),
                        backup.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    backupTemp.delete()
                } catch (t: Throwable) {
                    backupTemp.delete()
                    throw t
                }
            }

            try {
                Files.move(
                    temp.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            syncDir(file.parentFile)

            val written = file.readBytes()
            if (!written.contentEquals(data)) {
                throw IllegalStateException("verification failed: ${file.path}")
            }
            return JSONObject()
                .put("code", 0)
                .put("path", file.path)
                .put("bytes", data.size)
                .put("mode", mode)
                .toString()
        } catch (t: Throwable) {
            temp?.delete()
            throw t
        } finally {
            lock.unlock()
        }
    }

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

    private fun backupFile(file: File): File = File(file.parentFile, file.name + BACKUP_SUFFIX)

    private fun ensureParent(file: File) {
        val parent = file.parentFile
        if (parent != null && !parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) {
            error("cannot create parent directory: ${parent.path}")
        }
    }

    private fun modeOf(file: File): Int = try {
        val perms = Files.getPosixFilePermissions(file.toPath())
        var mode = 0
        if (OWNER_READ in perms) mode = mode or 0x100
        if (OWNER_WRITE in perms) mode = mode or 0x80
        if (OWNER_EXECUTE in perms) mode = mode or 0x40
        if (GROUP_READ in perms) mode = mode or 0x20
        if (GROUP_WRITE in perms) mode = mode or 0x10
        if (GROUP_EXECUTE in perms) mode = mode or 0x8
        if (OTHERS_READ in perms) mode = mode or 0x4
        if (OTHERS_WRITE in perms) mode = mode or 0x2
        if (OTHERS_EXECUTE in perms) mode = mode or 0x1
        mode
    } catch (_: Throwable) {
        0
    }

    private fun syncDir(dir: File?) {
        if (dir == null) return
        try {
            java.io.FileInputStream(dir).fd.sync()
        } catch (_: Throwable) {
        }
    }
}
