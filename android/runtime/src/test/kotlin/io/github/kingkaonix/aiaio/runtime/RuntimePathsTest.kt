package io.github.kingkaonix.aiaio.runtime

import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimePathsTest {

    @Test
    fun acceptsPathsUnderTheRuntimeRoot() {
        assertEquals(
            "/data/local/tmp/aiaio/probe/probe.sh",
            RuntimePaths.resolve("/data/local/tmp/aiaio/probe/probe.sh").path,
        )
        assertEquals(
            "/data/local/tmp/aiaio/bottles/node/bin/node",
            RuntimePaths.resolve("bottles/node/bin/node").path,
        )
        assertEquals(RuntimePaths.ROOT, RuntimePaths.resolve(RuntimePaths.ROOT).path)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSiblingWithTheSamePrefix() {
        // "/data/local/tmp/aiaioevil" starts with the root string but is not inside it.
        RuntimePaths.resolve("/data/local/tmp/aiaioevil/payload")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsParentTraversal() {
        RuntimePaths.resolve("/data/local/tmp/aiaio/../../../data/adb/modules")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPathsOutsideTheRoot() {
        RuntimePaths.resolve("/system/bin/sh")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyPath() {
        RuntimePaths.resolve("")
    }

    @Test
    fun mapsUnixModesToPosixPermissions() {
        assertEquals(
            setOf(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE),
            RuntimeFiles.permissions(0x1C0),
        )
        assertTrue(RuntimeFiles.permissions(0x0).isEmpty())
        assertEquals(9, RuntimeFiles.permissions(0x1FF).size)
    }
}
