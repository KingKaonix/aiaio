# T03 — Config round-trip proof

**Owner:** Core pod · **Depends on:** T01 · **Status:** In progress (wired, CI green, device proof pending) — core `FileBridge`/`fs.*` + tests exist; Android/Shizuku runtime file-bridge (AIDL v3 append-only, service ops, app adapter), T03 Phase 0 probe harness, and core-service `fs.*` dispatch interception via `RuntimeFileBridge` are implemented and pushed; CI `ci-tooling` green but on-device evidence not yet collected (current vscreen app is older build).

## Goal

From the Rust core, read and write `$RUNTIME_HOME/.claude/settings.json` through the runtime
bridge so that a round trip is byte-identical and crash-safe.

## Steps

1. Add `PathProvider` and `FileBridge` ports to `core/crates/core`; implement a desktop/local
   adapter and a bridge adapter that calls the runtime service by ID. *(core done; Android/Shizuku adapter + AIDL ops added; `CoreBridge`/`CoreClient` now intercept `fs.*` dispatch and route through `RuntimeFileBridge` when runtime attached — device proof pending)*
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
- 2026-11-13 (implementation + wiring, CI green / device unverified): commits `2461837d`, `9b294adf`, `8f7bfe34` on `main` — `IRuntimeService.aidl` (append-only: `readFile/stat/listDir/rename/remove/restoreBackup/writeFileAtomic`, Shizuku user-service v3), `RuntimeFiles` (confined atomic write temp/fsync/chmod/backup `.aiaio-bak`/rename/dir-sync/read-back verify, per-path locking, restore), `RuntimeUserService`/`RuntimeManager`/`RuntimeFileBridge`, Phase 0 "Run T03 file bridge probe" harness, and core-service `CoreBridge` fs.* interception + `CoreClient` runtime attach. CI `ci-tooling` run [43503506432](https://github.com/KingKaonix/aiaio/actions/runs/43503506432) (head `8f7bfe34c5b22ebb743a7291d7d6cb221cfbfa91`) **success**; artifacts: `aiaio-debug-apks` id 14075456485 (digest `sha256:adfa65a3cdb79f88204dd1a8ab702b3c1ca1ebb1e0c955404a7666f63379b15c`), `android-test-lint-reports` id 14075516466. **On-device evidence not collected:** vscreen device still runs older installed build (no T03 probe button visible) and environment cannot install APK/reliably download artifact contents for sideloading; probe run, traversal/mode/concurrency/restore results, 100-write log, and mid-write kill-test remain pending.

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

Still device-side: install CI-built debug APK (run 43503506432, commit `8f7bfe34`) on the T01 device, run the T03 probe and capture round-trip/mode/stat/list/rename/restore, traversal rejection, and concurrency results, then perform mid-write kill-test and gather 100-write/fixture evidence. Consider adding explicit audit-log entry on writes and exercising JSONC/TOML/YAML fixtures + 0600 path in probe for full acceptance.
