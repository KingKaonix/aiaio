# AGENTS.md — aiaio

This file is the operating contract for any agent (human or AI) working in this repository.
Read it before touching code. Update `docs/agents/STATE.md` after every merged task.

## What this repo is

`aiaio` is a native Android app (Jetpack Compose) for managing AI coding tools — providers,
MCP servers, skills, prompts, sessions, usage, quotas and a local routing proxy — plus a
matching desktop build. It is a fork of [cc-switch](https://github.com/farion1231/cc-switch)
(MIT). The upstream Rust core is reused; the React desktop UI is **not** used on Android.

The canonical plan is `docs/PLAN.md` (identical to the project-root `aiaio-plan.md`).

## Non-negotiables

1. **No root. No Termux dependency.** The app must be self-contained.
2. **Shizuku is the only privileged path.** Runtime files and processes live in
   `/data/local/tmp/aiaio/` and are mediated by a Shizuku user service (shell UID).
3. **Never try to execute binaries from app-private storage.** Android blocks `execve()` there
   for `targetSdk >= 29` (W^X). App-side native code is shipped as `lib*.so` in `jniLibs`.
4. **Native terminal.** No WebView, no xterm.js, no GPLv3 terminal library.
5. **Sync is merge-based.** Upstream snapshot sync (`db.sql` + `skills.zip`) is not acceptable
   for multi-device. Every synced table needs `updated_at`, `origin_device`, `deleted_at`, `rev`.
6. **No Play Store assumptions.** GitHub Releases/Obtainium first, F-Droid `libre` flavor later.
   Do not add Play-only services, Play Integrity, or Firebase as hard dependencies.
7. **License hygiene.** App code is MIT. Do not link GPLv3 code. Runtime bottles are separate
   programs, never linked; each must record its license and redistribution status.

## Repository map

| Path | What | Owner pod |
|---|---|---|
| `src/`, `src-tauri/`, `tests/` | Upstream desktop app. Keep building; do not restyle. | Desktop |
| `core/` | Tauri-free Rust workspace: `runtime-proto`, `core`, `ffi` | Core |
| `android/` | Gradle multi-module project (`app`, `core-service`, `terminal`, `runtime`, `sync`) | Android |
| `android/app` | Compose UI, navigation, design system | Android UI |
| `android/core-service` | `:core` process, JNI bridge, AIDL, foreground service | Core/Android |
| `android/terminal` | VT parser, screen model, Compose renderer, PTY client | Terminal |
| `android/runtime` | Shizuku client, user service, bottle registry, package install | Runtime |
| `android/sync` | WorkManager, transports, merge UI (Sync Doctor) | Sync |
| `tools/bottles` | Bottle manifest schema and recipes | Runtime |
| `docs/adr` | Architecture decision records (append-only) | All |
| `docs/agents` | Task specs, state, playbooks | All |

## Build and test

The Termux dev box has Rust (1.99.1 locally, 1.95 in CI) but **no JDK/Android SDK**. Use CI
for APK builds; write code that a CI run can verify. Local checks that do work: `cargo` for
the Rust workspace, `node`/`pnpm` for upstream desktop, `python3` for scripts, `git`.

```bash
# Rust core (needs cargo)
cd core && cargo test && cargo clippy --all-targets -- -D warnings && cargo fmt --check

# Android (needs JDK 21, Android SDK 36, NDK 27.2, Rust android targets)
bash scripts/build-android.sh       # builds Rust core + debug APK
# or
cd android && gradle :app:assembleDebug
```

Scripts use the standard `#!/usr/bin/env bash` shebang, which does not exist on Termux; run
them as `bash scripts/<name>.sh` there. `scripts/check-upstream-parity.sh` is the only one
that works on the dev box.

CI workflows: `.github/workflows/android.yml` (Rust + APK + 16 KB check), `.github/workflows/parity.yml`
(upstream compatibility).

## Definition of done (every task)

- [ ] Acceptance criteria in the task file are met and demonstrated (log, screenshot, APK, or test).
- [ ] `cargo test` / `cargo clippy -D warnings` / `cargo fmt --check` pass for touched Rust.
- [ ] `gradle test lint` passes for touched Android modules.
- [ ] New public API/contract is documented in an ADR or the module README.
- [ ] `docs/agents/STATE.md` updated (status, next step, blockers).
- [ ] No secrets, tokens, keystores, or personal data committed.
- [ ] Upstream files untouched unless the task explicitly says otherwise.

## Agent working rules

- **Contract first.** When two pods touch the same feature, land the proto/IDL/schema before
  the implementation.
- **Small diffs.** One concern per change. Do not reformat upstream code.
- **No silent stubs.** A placeholder must be named `TODO(phase-N)` and have a task file.
- **Device claims need evidence.** "Works on device" requires a log or recording attached to
  the task; otherwise label it `unverified`.
- **Never invent Android/Shizuku APIs.** Verify against the SDK or the Shizuku docs; if unsure,
  write the task blocker instead of code.
- **Upstream rebase:** monthly. Resolve conflicts in `core/` first, then Android. Never rewrite
  upstream history.
- **Commits:** `area: summary` (e.g. `core: add dispatch façade`, `android: provider list`).
- **Secrets:** never in logs, never in deep links, never in sync payloads without encryption.

## Current focus

**Phase 0 — proofs and go/no-go.** See `docs/agents/STATE.md` and
`docs/agents/tasks/T00-phase0-proofs.md`. Do not build feature UI until the Shizuku exec proof
(T01) passes on a real device.
