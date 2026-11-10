# ADR-0003 — Merge-based multi-device sync

**Status:** accepted · 2026-11-10

## Context

Upstream sync (`services/sync_protocol.rs`) uploads a whole snapshot: `db.sql`, `skills.zip`,
`manifest.json`. It has no per-row timestamps, tombstones or merge. With a desktop and a phone
both editing, the last upload silently destroys the other side's changes. Sync is
non-negotiable, so this must be fixed at the data model, not patched in the UI.

## Decision

- Add sync metadata to every synced entity: `updated_at`, `origin_device`, `deleted_at`, `rev`.
- Add a `devices` table and a per-device append-only `oplog` (JSONL) uploaded next to the
  snapshot for backward compatibility.
- Merge: last-writer-wins per row by `(updated_at, origin_device)`; tombstones beat older
  updates; deterministic tie-break. Settings merge per key. Skills are content-addressed by
  SHA-256.
- Transports: WebDAV, S3 (with ETag/`If-Match`), and LAN direct sync (QR pairing, pinned TLS).
- Keep the legacy snapshot format readable so upstream desktop v4 can import; our desktop
  build uses the merge engine.
- DB migration is versioned (`v18+`) and shared by desktop and Android.

## Consequences

- DAO updates, migration work and a conflict UI ("Sync Doctor") are part of Phase 1/5.
- 3-device simulation (desktop + 2 Android) is required in CI from the first sync commit.
- Snapshot-only mode remains available as a degraded "single writer" option.

## Rejected

- Keep snapshot sync (data loss).
- CRDT for everything (overkill; LWW + tombstones is sufficient for config rows).
- Cloud-only sync (LAN is required for offline/local-first use).
