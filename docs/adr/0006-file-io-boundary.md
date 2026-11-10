# ADR-0006 — File I/O boundary: Rust owns the rules, Kotlin owns the transport

**Status:** accepted · 2026-11-10

## Context

T03 needs one implementation of "read and write a tool config safely" for two very different
hosts:

- desktop: ordinary filesystem access from the Tauri shell;
- Android: everything under `/data/local/tmp/aiaio` is owned by the shell UID, so every read and
  write must cross `IRuntimeService` (Shizuku user service).

If each host implements the rules (path validation, atomic write, backups), they drift, and the
drift is security-relevant: the path allow-list is the only thing between a buggy caller and
`/data/data/...`.

## Decision

- **The rules live once, in Rust**, in `core/crates/core/src/files.rs` (`FileBridge`,
  `LocalFileBridge`). Path parsing, `..`/absolute/symlink rejection, temp-file + `fsync` +
  `rename` + read-back verification, the `.aiaio-bak` sidecar and per-path serialisation are
  all there.
- **Kotlin only moves bytes.** On Android the JNI layer provides a `RemoteFileBridge` whose
  calls land on a small Kotlin interface (`RemoteFs`) implemented by the `:core` service; that
  implementation translates one-to-one to `IRuntimeService` calls. It does not parse paths,
  choose filenames, or decide when to write.
- **Defence in depth.** `RuntimePaths` on the service side still canonicalises and confines
  every path to `/data/local/tmp/aiaio`, independently of what the caller computed.
- **One JNI upcall per operation is acceptable.** Config reads/writes are low-frequency
  (single-digit per second at worst). Large or high-rate payloads (skills assets, terminal
  output) use dedicated transports, not this path: skills will stream with fd passing or
  chunked calls (`TODO(phase-2)`), the PTY already passes a `ParcelFileDescriptor`.
- **Failures never degrade to success.** A missing bridge, a dead Shizuku service or a
  rejected path returns a typed error (`unsupported`, `path_escape`, `io`, `not_found`), and
  the core re-reads the file after writing to confirm the bytes that landed.
- **Wire changes are append-only.** New AIDL methods go at the end and bump the user-service
  `version` (`2` → `3` when `RemoteFs` lands).

## Consequences

- The AIDL surface grows to the primitives the bridge needs: `readFile`, `listDir`, `stat`,
  `rename`, `remove` (plus the existing `mkdirs`/`writeFile`/`exec`).
- The Kotlin bridge must be tested against a fake `RemoteFs`, and the Rust rules against a
  real temp directory, so both halves are covered without a device.
- Desktop keeps using `LocalFileBridge` unchanged — the same tests prove both platforms.
- A bug in Rust path handling is a security bug on both platforms; that is the intended
  trade (one audited implementation instead of two).

## Rejected

- **Implement file I/O in Kotlin on Android and in Rust on desktop** — two rule sets, silent
  drift, and the device is the place where drift is hardest to test.
- **Run a shell helper (`cat`, `mv`) per operation through `exec`** — quoting and injection
  surface, no atomic rename semantics on all filesystems, and no read-back verification.
- **Expose raw file descriptors to the app process** — Android cannot `open()` the shell-owned
  tree itself; fd passing is reserved for the PTY and bulk transfers where it pays off.
