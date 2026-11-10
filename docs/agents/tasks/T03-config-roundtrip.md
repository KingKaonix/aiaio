# T03 — Config round-trip proof

**Owner:** Core pod · **Depends on:** T01 · **Status:** Not started

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

- [ ] Byte-identical round trip for JSON, JSONC, TOML and YAML fixtures.
- [ ] Atomicity: killing the app mid-write leaves the previous file valid.
- [ ] Path traversal (`..`, symlink to `/data/data`, absolute path) rejected with a typed error.
- [ ] File mode preserved (0600 for secret-bearing files).
- [ ] 100 concurrent writes to the same path produce one of the complete payloads, never a mix.
- [ ] Broken-file recovery: a corrupt target can be restored from the backup sidecar.

## Evidence

Fixture hashes before/after, kill-test transcript, rejection cases, and a log of the 100-write
concurrency run.
