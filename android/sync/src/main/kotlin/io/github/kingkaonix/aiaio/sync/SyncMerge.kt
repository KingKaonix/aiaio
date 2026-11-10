package io.github.kingkaonix.aiaio.sync

/**
 * Merge primitives for multi-device sync (ADR-0003). Kept platform-free so the same rules can
 * be exercised in JVM tests here and mirrored in the Rust core.
 *
 * Rule: last-writer-wins by effective timestamp, where a tombstone's `deletedAt` is its
 * timestamp. Ties are broken deterministically: tombstone first, then originDevice, then rev.
 * A write newer than a tombstone resurrects the row, which is what a user expects when they
 * edit on one device after deleting on another.
 */
data class SyncMeta(
    val updatedAt: Long,
    val originDevice: String,
    val deletedAt: Long? = null,
    val rev: Long = 0,
)

data class SyncRecord<T>(
    val key: String,
    val value: T?,
    val meta: SyncMeta,
) {
    val isTombstone: Boolean
        get() = meta.deletedAt != null
}

fun <T> mergeRecords(local: SyncRecord<T>, remote: SyncRecord<T>): SyncRecord<T> =
    if (compareMeta(local.meta, remote.meta) >= 0) local else remote

/** 1 when [a] wins, -1 when [b] wins, 0 only for byte-identical metadata. */
fun compareMeta(a: SyncMeta, b: SyncMeta): Int {
    val effectiveA = a.deletedAt ?: a.updatedAt
    val effectiveB = b.deletedAt ?: b.updatedAt
    if (effectiveA != effectiveB) return effectiveA.compareTo(effectiveB)

    val aTombstone = a.deletedAt != null
    val bTombstone = b.deletedAt != null
    if (aTombstone != bTombstone) return if (aTombstone) 1 else -1

    val byDevice = a.originDevice.compareTo(b.originDevice)
    if (byDevice != 0) return byDevice

    return a.rev.compareTo(b.rev)
}
