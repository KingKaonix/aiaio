package io.github.kingkaonix.aiaio.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergeTest {

    private fun record(
        value: String?,
        updatedAt: Long,
        device: String = "phone",
        deletedAt: Long? = null,
        rev: Long = 1,
    ) = SyncRecord(
        key = "provider:claude",
        value = value,
        meta = SyncMeta(updatedAt = updatedAt, originDevice = device, deletedAt = deletedAt, rev = rev),
    )

    @Test
    fun newerUpdateWins() {
        val local = record("a", updatedAt = 100, device = "desktop")
        val remote = record("b", updatedAt = 200, device = "phone")

        assertEquals(remote, mergeRecords(local, remote))
        // Argument order must not matter: the newer record wins either way.
        assertEquals(remote, mergeRecords(remote, local))
    }

    @Test
    fun tombstoneBeatsOlderUpdate() {
        val local = record("old", updatedAt = 100)
        val remote = record(null, updatedAt = 100, deletedAt = 150)

        val merged = mergeRecords(local, remote)
        assertTrue(merged.isTombstone)
    }

    @Test
    fun newerUpdateResurrectsDeletedRow() {
        val tombstone = record(null, updatedAt = 100, deletedAt = 150)
        val edited = record("new", updatedAt = 200)

        assertEquals(edited, mergeRecords(tombstone, edited))
    }

    @Test
    fun tieIsBrokenByDeviceThenRev() {
        val phone = record("phone", updatedAt = 100, device = "phone")
        val desktop = record("desktop", updatedAt = 100, device = "desktop")

        assertEquals(phone, mergeRecords(phone, desktop))
        assertEquals(phone, mergeRecords(desktop, phone))

        val phoneRev2 = record("phone2", updatedAt = 100, device = "phone", rev = 2)
        assertEquals(phoneRev2, mergeRecords(phone, phoneRev2))
    }

    @Test
    fun tombstoneWinsTie() {
        val update = record("value", updatedAt = 500)
        val delete = record(null, updatedAt = 400, deletedAt = 500)

        assertTrue(mergeRecords(update, delete).isTombstone)
    }

    @Test
    fun mergeIsCommutative() {
        val a = record("a", updatedAt = 10, device = "a")
        val b = record("b", updatedAt = 20, device = "b")

        assertEquals(mergeRecords(a, b), mergeRecords(b, a))
    }
}
