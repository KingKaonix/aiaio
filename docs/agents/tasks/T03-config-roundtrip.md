# T03 — Config round-trip proof

**Owner:** Core pod · **Depends on:** T01 · **Status:** In progress (implementation added, device proof pending) — core `FileBridge`/`fs.*` + tests exist; Android/Shizuku runtime file-bridge (AIDL v3, service ops, app adapter) and a T03 device probe harness are implemented in-repo, but on-device round-trip/kill-test evidence not yet collected on a CI-built APK.

## Goal

From the Rust core, read and write `$RUNTIME_HOME/.claude/settings.json` through the runtime
bridge so that a round trip is byte-identical and crash-safe.

## Steps

1. Add `PathProvider` and `FileBridge` ports to `core/crates/core`; implement a desktop/local
   adapter and a bridge adapter that calls the runtime service by ID. *(core done; Android/Shizuku-side adapter + AIDL ops added in-app/runtime — device proof pending)*
2. Implement `readFile(path)`, `writeFileAtomic(path, bytes, mode)`, `listDir`, `stat`,
   `rename`, `remove` with a path allow-list rooted at `/data/local/tmp/aiaio/home` (runtime root confined via `RuntimePaths`). *(implemented in runtime service; atomic write uses backup/read-back verify and per-path serialization)*
3. Reject `..`, absolute paths outside the root, symlinks, and hard links to outside the root. *(enforced by `RuntimePaths`/service path resolution; device rejection evidence pending)*
4. Write: temp file in the same directory (`.aiaio-tmp-<rand>`) → `fsync` → `rename` → verify. *(implemented in `RuntimeFiles.writeFileAtomic`; device evidence pending)*
5. Concurrent writers: serialize per path; last writer wins with an audit log entry. *(per-path lock in service; device probe covers concurrent writes, audit log entry not yet implemented)*
6. Simulate a process kill between write and rename; original file must be intact. *(not yet executed/evidenced on device)*

## Acceptance criteria

- [ ] Byte-identical round trip for JSON, JSONC, TOML and YAML fixtures. *(core `LocalFileBridge`/dispatch tests exist in Rust; Android T03 probe covers JSON round-trip, but full fixture set on-device unverified)*
- [ ] Atomicity: killing the app mid-write leaves the previous file valid. *(unverified on device)*
- [ ] Path traversal (`..`, symlink to `/data/data`, absolute path) rejected with a typed error. *(covered in core tests; runtime service confines via `RuntimePaths`, device adapter rejection evidence pending)*
- [ ] File mode preserved (0600 for secret-bearing files). *(covered in core tests; device evidence pending)*
- [ ] 100 concurrent writes to the same path produce one of the complete payloads, never a mix. *(covered in core tests; device probe does concurrent atomic writes but full 100-write device evidence/log pending)*
- [ ] Broken-file recovery: a corrupt target can be restored from the backup sidecar. *(covered in core tests; device probe exercises restore path, formal corrupt-target evidence pending)*

## Evidence

- 2026-11-13 (device/env, pre-impl): app `io.github.kingkaonix.aiaio.debug` running on Motorola moto g play 2024 (Android 14/SDK 34) with Shizuku ready/T01 proven, but runtime file ops and device harness were missing.
- 2026-11-13 (implementation, CI pending/device unverified): extended `android/runtime` append-only — `IRuntimeService.aidl` adds `readFile/stat/listDir/rename/remove/restoreBackup/writeFileAtomic` (bumped Shizuku user-service `version` to 3 in `RuntimeManager`), `RuntimeFiles` implements confined read/stat/list/rename/remove + atomic write (temp in target dir, `fsync`, chmod, backup `.aiaio-bak`, atomic/non-atomic rename fallback, dir sync, read-back verify) with per-path locking and backup restore, `RuntimeUserService` exposes ops, and `RuntimeFileBridge` + Phase 0 "Run T03 file bridge probe" harness added in app for device checks (round-trip/stat/list/rename/restore, escape rejection, concurrent atomic writes). **No on-device T03 evidence collected yet** (requires CI-built APK install/run and kill-test). Fixture hashes, kill-test transcript, rejection cases, and 100-write log to be added.

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

Still device-side: run the new T03 probe on a CI-built debug APK against the Shizuku runtime (T01 device), capture round-trip/mode/traversal/concurrency/restore results, and perform the mid-write kill-test with evidence. No Rust/JNI wiring of `fs.*` dispatch to this Android bridge yet.
