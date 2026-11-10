# ADR-0002 — Self-contained runtime via Shizuku

**Status:** accepted · 2026-11-10

## Context

The app manages tools (Claude Code, Codex, Gemini CLI, OpenCode, …) and their configs. On
Android those tools do not exist unless we provide them. Self-contained is a hard requirement;
Termux is excluded; root is excluded.

Android blocks `execve()` on files in app-private storage for `targetSdk >= 29`. Only
`nativeLibraryDir` and system-owned exec-capable paths (e.g. `/data/local/tmp`) work.

## Decision

- Runtime home: `/data/local/tmp/aiaio/` (shell-owned).
- A **Shizuku user service** (shell UID, bound over Binder) owns file I/O, process spawning and
  PTY allocation.
- App-side native code ships in `jniLibs` and runs as the app (core process, proxy).
- No-runtime fallback: store, presets, MCP/skills/prompts editing, proxy and sync remain usable.

## Consequences

- Requires Shizuku setup (ADB/wireless debugging) for the full experience; onboarding is a
  first-class screen.
- `/data/local/tmp` may be cleared by system updates → bootstrap must be re-runnable and cheap.
- All runtime file access goes through the bridge with allow-lists, atomic writes and symlink
  rejection.
- Shell UID is powerful; the service must never be exposed to third-party apps.

## Rejected

- proot rootfs in app storage (blocked by W^X; only works at `targetSdk 28`).
- Termux RUN_COMMAND (violates self-contained).
- Root (violates decision), AVF (not broadly available), nodejs-mobile (no child processes/PTY).
