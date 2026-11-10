# aiaio core (Rust)

Tauri-free core shared by the Android app and the matching desktop build.

```
crates/runtime-proto   wire types: dispatch envelope, sync metadata, oplog
crates/core            AppCore, dispatch façade, platform ports
crates/ffi             JNI exports -> libaiaio_core.so
```

The upstream desktop code in `src-tauri/` is the reference implementation. Phase 1 moves its
services/database/proxy modules here and replaces Tauri types with the ports in
`crates/core/src/ports.rs`.

## Build

```bash
cargo test --workspace
cargo clippy --all-targets -- -D warnings
cargo fmt --all -- --check

# Android (needs NDK 27.2 and cargo-ndk), run from this directory:
# cargo-ndk resolves the workspace from the working directory, and the 16 KB linker
# flags in ../.cargo/config.toml are found by walking up from here.
cargo ndk -t arm64-v8a -t x86_64 -o ../android/app/src/main/jniLibs build --release -p aiaio-ffi
```

## ABI contract

- Library name: `libaiaio_core.so`
- Kotlin class: `io.github.kingkaonix.aiaio.core.NativeCore`
- Methods: `nativeDispatch(String): String`, `nativeProtocolVersion(): Int`

Protocol types and version live in `crates/runtime-proto`. Additive commands do not bump
`PROTOCOL_VERSION`; changed payloads do.

## Commands

| Command | Args | Result |
|---|---|---|
| `ping` | – | `pong`, `protocol`, `coreVersion` |
| `core.info` | – | `protocol`, `coreVersion`, `runtimeRoot` (null without a `PathProvider`) |
| `fs.readFile` | `path` | `path`, `text` (UTF-8 only) |
| `fs.writeFile` | `path`, `text`, `mode`? | `path`, `bytes`, `mode` (default `0600`) |
| `fs.stat` | `path` | `path`, `len`, `mode`, `isFile`, `isDir` |
| `fs.listDir` | `path` | `path`, `entries[]` (`name`, `isDir`, `len`), sorted |
| `fs.rename` | `from`, `to` | `from`, `to` |
| `fs.remove` | `path` | `path` |
| `fs.restoreBackup` | `path` | `path`, `restored` |

`fs.*` paths are **relative to the file bridge root** and cannot escape it (`..`, absolute
paths and symlinks out of the root are rejected with `path_escape`). Writes are atomic and
keep the previous content as `<name>.aiaio-bak`. Without a `FileBridge` the `fs.*` commands
return `unsupported`, so a shell never believes a write happened when it did not.

## Phase 0 scope

The façade answers `ping`, `core.info` and the `fs.*` family. Anything else returns an
`unknown_command` error, which is what the Android Phase 0 status panel shows.
