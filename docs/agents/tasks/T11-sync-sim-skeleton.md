# T11 — Merge-based sync engine & 3-device CI simulation skeleton (Story 4 prep)

**Owner:** Sync + Core pod · **Depends on:** T10 schema/contracts · **Status:** Not started
**Spec:** `_bmad-output/specs/spec-aiaio-completion/` (CAP-3) · **Story:** 4

## Goal

Stand up correctness scaffolding for merge-based sync before/alongside transports.

## Scope (initial)

- Deterministic merge rules (LWW by effective timestamp, tombstones win over older updates, deterministic tie-break) implemented consistently (Rust core + any Android mirror as needed) with tests.
- 3-device simulation harness in CI covering: concurrent edits, offline/reconnect, clock skew, tombstone GC, interrupted upload, corrupt remote, large payload (100 MB class as feasible).
- Oplog + snapshot protocol design; conflict/Sync Doctor data model hooks.

## Acceptance criteria

- [ ] 3-device sim suite runs in CI and fails on known-bad merge cases (red/green proven).
- [ ] Merge behavior matches ADR-0003/SPEC and is portable across core/Android implementations.
- [ ] Transport work (WebDAV/S3/LAN) blocked on passing sim baseline where applicable.

## Evidence

CI job links, test cases, design notes (to be added).

## Notes

- Current `android/sync/SyncMerge.kt` is a minimal primitive — extend/align with core model and schema.
