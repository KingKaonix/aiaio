# aiaio

Native Android app for managing AI coding tools — providers, MCP, skills, prompts, sessions,
usage, quotas, and a local routing proxy — self-contained, no root, no Termux.

> **Status: Phase 0 (proofs).** Nothing is shippable yet. See `docs/agents/STATE.md`.

- **UI:** Jetpack Compose (Material 3), native VT terminal
- **Core:** reused from [cc-switch](https://github.com/farion1231/cc-switch) (MIT), compiled as a
  Rust JNI library running in a `:core` process
- **Runtime:** Shizuku user service (shell UID) hosting tools in `/data/local/tmp/aiaio/`
- **Sync:** merge-based multi-device sync (WebDAV, S3, LAN pairing)
- **Distribution:** GitHub Releases / Obtainium; F-Droid `libre` flavor planned

## Repo layout

```
core/            Rust workspace (runtime-proto, core, ffi)
android/         Gradle project (app, core-service, terminal, runtime, sync)
tools/bottles/   runtime bottle manifest + recipes
docs/            PLAN.md, ADRs, agent task specs
src/ src-tauri/  upstream desktop app (kept for the matching desktop build)
```

## Build

CI is the source of truth (`.github/workflows/android.yml`). On a machine with JDK 21,
Android SDK 36, NDK 27.2 and Rust:

```bash
scripts/build-android.sh
```

## License

MIT. See `LICENSE` and `NOTICE`. Upstream copyright: Jason Young and cc-switch contributors.
`aiaio` is an independent fork and is not affiliated with the upstream project or its official
website.
