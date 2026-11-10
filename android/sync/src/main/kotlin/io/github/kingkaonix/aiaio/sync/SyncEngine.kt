package io.github.kingkaonix.aiaio.sync

/**
 * Transport boundary for sync. Implementations: WebDAV, S3, LAN direct (pinned TLS).
 * `etag` carries the remote version so an upload can use `If-Match` and never overwrite a
 * newer remote state silently.
 */
interface SyncTransport {
    val id: String

    suspend fun pull(): RemoteSnapshot?

    suspend fun push(payload: ByteArray, etag: String?)
}

data class RemoteSnapshot(
    val payload: ByteArray,
    val etag: String?,
)

sealed interface SyncOutcome {
    data object UpToDate : SyncOutcome
    data class Merged(val pushed: Boolean, val conflicts: Int) : SyncOutcome
    data class Failed(val reason: String, val retryable: Boolean) : SyncOutcome
}

/**
 * Phase 5 fills this in: pull the remote oplog + snapshot, merge into the local database with
 * [mergeRecords], write tombstones, then push with `If-Match`. Phase 0 only fixes the surface.
 */
interface SyncEngine {
    suspend fun sync(transport: SyncTransport): SyncOutcome
}
