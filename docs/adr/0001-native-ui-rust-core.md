# ADR-0001 — Native Android UI on the upstream Rust core

**Status:** accepted · 2026-11-10

## Context

Upstream cc-switch is Tauri 2 + React (~164k LOC TS) with a Rust backend (~336k LOC). The
Android app must be native Compose, self-contained, and feature-complete. Rewriting proxy
transforms, 15 tool adapters, usage parsers and quota clients in Kotlin is not realistic.

## Decision

- Extract a **Tauri-free Rust core** (`core/`) exposing one façade:
  `dispatch(requestJson) -> responseJson` plus an event stream.
- Ship the core as `libaiaio_core.so` in `jniLibs`, run it in a separate `:core` process, and
  talk to it from Compose over AIDL/JSON-RPC.
- The desktop app keeps its Tauri shell but uses the same core.
- The React UI is not used on Android.

## Consequences

- Upstream business logic is reused; only the shell and UI are replaced.
- A mechanical but large refactor is required (`State<AppState>` → `AppCore`,
  `AppHandle::emit` → `EventSink`, `dirs::home_dir` → `PathProvider`).
- Golden tests from upstream become the compatibility oracle for the extraction.
- The desktop build is the canary: if it stops building on the shared core, the extraction broke.

## Rejected

- Hidden WebView running the React app and bridging Compose → JS → Tauri IPC (memory, crash
  coupling, second rendering stack).
- Full Kotlin rewrite (12–18+ months, high drift risk).
