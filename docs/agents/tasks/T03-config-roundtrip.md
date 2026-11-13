# T03 — Config round-trip proof

**Owner:** Core pod · **Depends on:** T01 · **Status:** In progress (blocked for device proof) — `FileBridge` + `fs.*` dispatch commands land in Rust core with tests; Android/Shizuku file-bridge adapter and on-device round-trip/kill-test not evidenced in current build.

## Goal

From the Rust core, read and write `$RUNTIME_HOME/.claude/settings.json` through the runtime
bridge so that a round trip is byte-identical and crash-safe.

## Steps

1. Add `PathProvider` and `FileBridge` ports to `core/crates/core`; implement a desktop/local
   adapter and a bridge adapter that calls the runtime service by ID.
2. Implement `readFile(path)`, `writeFileAtomic(path, bytes, mode)`, `listDir`, `stat`,
   `rename`, `remove` with a path allow-list rooted at `/data/local/tmp/aiaio/home`.
3. Reject `..`, absolute paths outside the root, symlinks, and hard links to outside the root.
4. Write: temp file in the same directory (`.aiaio-tmp-<rand>`) → `fsync` → `rename` → verify.
5. Concurrent writers: serialize per path; last writer wins with an audit log entry.
6. Simulate a process kill between write and rename; original file must be intact.

## Acceptance criteria

- [ ] Byte-identical round trip for JSON, JSONC, TOML and YAML fixtures. *(core `LocalFileBridge`/dispatch tests exist in Rust; device round trip via Android/Shizuku bridge unverified)*
- [ ] Atomicity: killing the app mid-write leaves the previous file valid. *(unverified on device)*
- [ ] Path traversal (`..`, symlink to `/data/data`, absolute path) rejected with a typed error. *(covered in core tests; device adapter unverified)*
- [ ] File mode preserved (0600 for secret-bearing files). *(covered in core tests; device adapter unverified)*
- [ ] 100 concurrent writes to the same path produce one of the complete payloads, never a mix. *(covered in core tests; device adapter unverified)*
- [ ] Broken-file recovery: a corrupt target can be restored from the backup sidecar. *(covered in core tests; device adapter unverified)*

## Evidence

- 2026-11-13 (device/env): app `io.github.kingkaonix.aiaio.debug` running on Motorola moto g play 2024 (Android 14/SDK 34) with Shizuku ready/T01 proven, but `IRuntimeService` (AIDL) currently exposes only `uid/exec/mkdirs/writeFile` (no read/stat/list/rename/remove/restore) and no device-side `FileBridge` adapter/UI harness observed. Core-side implementation/tests exist in `core/crates/core/src/files.rs` and `fs.*` dispatch, however on-device byte-identical round trip, kill-test, traversal rejection through runtime bridge, and concurrency/backup recovery remain **unverified** pending adapter + instrumentation. Fixture hashes, kill-test transcript, rejection cases, and 100-write log to be added.

## Progress (steps 1–6, platform-independent half)

`core/crates/core/src/files.rs` implements the rules once, for every platform:

- `FileBridge` trait (`read`, `write_atomic`, `list`, `stat`, `rename`, `remove`,
  `restore_backup`) and `LocalFileBridge` rooted at a canonical directory.
- Paths are relative to the root; absolute paths, `..`, `RootDir` and `Prefix` components are
  rejected as `PortError::PathEscape`. The deepest existing ancestor is canonicalised, so a
  symlinked directory that leaves the root is rejected even when the target does not exist.
- `write_atomic`: temp file in the target directory → `write_all` → `sync_all` → `chmod` →
  previous content copied to `<name>.aiaio-bak` → `rename` → directory `fsync` → read-back
  byte comparison (`PortError::Verification` on mismatch). Temp files are removed on failure.
- Writes to the same path are serialised through a per-path lock map, so 100 concurrent
  writers always leave exactly one complete payload.
- Dispatch exposes `fs.readFile`, `fs.writeFile` (text, optional `mode`), `fs.stat`,
  `fs.listDir`, `fs.rename`, `fs.remove`, `fs.restoreBackup` with stable error codes
  (`path_escape`, `not_found`, `invalid_args`, `unsupported`, `verification_failed`, `io`).
- Text-only for now: non-UTF-8 payloads return `not_utf8`. Binary assets (skills) need a
  base64/stream encoding in phase 2 — `TODO(phase-2)`.

Tests cover byte-identical round trip, mode `0600`, traversal rejection (absolute, `..`,
symlink out of root), missing files, backup + restore after a corrupt write, 100 concurrent
writers, sorted listing, and rename/remove confinement.

Still device-side: the AIDL adapter that implements `FileBridge` through `IRuntimeService`
(T03 step 1's Android half) and the kill-test on the phone.
