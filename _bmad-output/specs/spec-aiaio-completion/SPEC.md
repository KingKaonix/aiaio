---
id: SPEC-aiaio-completion
companions:
  - issues.md
  - completion-plan.md
  - companion-mode.md
  - ../../planning-artifacts/ux-designs/ux-aiaio-2026-11-13/DESIGN.md
  - ../../planning-artifacts/ux-designs/ux-aiaio-2026-11-13/EXPERIENCE.md
sources:
  - ../../../docs/PLAN.md
  - ../../../docs/agents/STATE.md
  - ../../../AGENTS.md
  - ../../../docs/adr/0001-native-ui-rust-core.md
  - ../../../docs/adr/0002-shizuku-runtime.md
  - ../../../docs/adr/0003-sync-merge.md
  - ../../../docs/adr/0004-native-terminal.md
  - ../../../docs/adr/0005-distribution-and-bottles.md
  - ../../../docs/adr/0006-file-io-boundary.md
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate. Source documents listed in frontmatter are for traceability — consult them only if you need narrative rationale or prose color this contract intentionally omits.

# aiaio Project Completion

## Why

**Mandate + vision + opportunity (combined):** Complete `aiaio` as a self-contained native Android app (Jetpack Compose, no root, no Termux) that manages AI coding tools by reusing the upstream cc-switch Rust core behind a transport-agnostic façade, with a Shizuku-mediated runtime (`/data/local/tmp/aiaio/`), a native terminal, and **merge-based** multi-device sync. The project pursues the self-contained path gated by Phase 0 proofs while preparing a Companion-mode fallback scope in parallel, so work can pivot without losing progress if kill criteria trigger. Outcome: demonstrable, evidence-backed parity/release quality (MVP → parity → hardened release) and a trustworthy app for real providers, runtimes, and synced data.

## Capabilities

- **CAP-1: Phase 0 go/no-go proofs and fallback readiness**
  - **intent:** User/maintainer can determine whether the self-contained Shizuku/runtime/proxy/PTY path is viable on real devices and have a prepared Companion-mode fallback scope if not.
  - **success:** Tasks T01–T07 (exec soak, bottle install, config round-trip, PTY, proxy SSE, 16 KB/build, device baseline) have device/runtime evidence recorded in `docs/agents/STATE.md` (and task specs) with pass/fail; kill-criterion decisions are documented; Companion-mode scope/plan is defined in parallel so pivot is actionable.

- **CAP-2: Tauri-free shared core extraction**
  - **intent:** Android and desktop can use the same upstream-derived core via `dispatch` + event ports to perform provider/config/proxy/session/usage/deeplink operations without Tauri coupling.
  - **success:** `core/` implements the façade and ports with extracted services/db/proxy/parsers as planned (incl. DB schema supporting sync metadata), upstream golden/parity tests pass as compatibility oracle, and desktop canary builds on the shared core.

- **CAP-3: Correct merge-based multi-device sync**
  - **intent:** User can sync data across devices/transports (WebDAV, S3, LAN) without silent data loss from concurrent/offline edits.
  - **success:** Per-entity sync metadata/tombstones/devices/oplog and deterministic merge are implemented with snapshot+oplog protocol, conflict/Sync Doctor UX, optional encryption, and a 3-device CI simulation (concurrent edits, offline/reconnect, clock skew, tombstone GC, interruption, corrupt remote, large payload) passes.

- **CAP-4: Native terminal for CLI/TUI workflows**
  - **intent:** User can install, authenticate, run, and interact with target AI CLIs (e.g. Claude Code, Codex, OpenCode) inside the app via a native Compose terminal.
  - **success:** PTY from Shizuku service is bridged to a VT parser/screen/renderer with required input/IME behavior and conformance (vttest subset + recorded CLI streams); target TUIs are usable and performance meets the plan target (60 fps at 80×24) or a documented, time-bounded mitigation is applied per kill criteria.

- **CAP-5: Runtime, proxy, and security baseline**
  - **intent:** User can manage and run tool runtimes and route traffic locally within a hardened, least-privilege boundary (self-contained when proofs pass).
  - **success:** Bottles install/verify (signed manifest, SHA-256)/run/update/rollback in `/data/local/tmp/aiaio/`; Shizuku service enforces path/command allow-lists with no shell interpolation; local proxy binds loopback with token+peer-UID checks, supports required routing/FGS behavior (incl. SSE), and secrets are protected (Keystore/encrypted storage) with no leakage in logs/backups/deep links/sync.

- **CAP-6: Feature parity, UX, hardening, and release**
  - **intent:** User can perform the full in-scope cc-switch workflows on Android (providers/MCP/skills/prompts/sessions/usage/quotas/profiles/deeplinks/import-export/settings, sync/runtime/terminal) through a coherent Compose UI and obtain supported builds.
  - **success:** Schema-driven UI and feature waves A–G meet parity fixtures and device-verified acceptance (scoped to chosen mode where applicable); hardening targets (soak/perf/memory/cold-start/security/a11y) pass; `full` and `libre` flavors build/release via supported channels (GitHub/Obtainium first) with licensing/manifest compliance and docs/onboarding; desktop aligned on merge engine per plan.

## Constraints

- No root and no Termux dependency in self-contained mode; **Shizuku is the only privileged path**. Runtime/process/files for privileged ops live in `/data/local/tmp/aiaio/` mediated by a shell-UID user service; never execute binaries from app-private storage (`targetSdk >= 29` W^X). Companion-mode fallback, if activated, must be explicitly scoped and documented without violating these invariants for the app itself.
- **Native terminal only:** no WebView, no xterm.js, no GPLv3 terminal linkage.
- **Sync is merge-based** (per-entity metadata, tombstones, deterministic merge, conflict handling) — snapshot-only/last-writer-wins is not acceptable for multi-device as an end state.
- Distribution: GitHub Releases/Obtainium first, `full` and `libre` flavors; no Play-only services/Integrity/Firebase as hard deps. Redistribute only with rights; otherwise install from official source with in-app consent and recorded license/provenance.
- License hygiene: app code MIT-compatible; no GPLv3 linkage; runtime bottles are separate programs with recorded license/redistribution status.
- Security baseline: loopback-bound proxy with token + peer-UID checks, allow-listed/no-interpolation command/file mediation, Keystore-backed secrets, `allowBackup=false`, no secrets in logs/backups/deep links/sync payloads without encryption.
- Device floor and build: `minSdk 26`, `compileSdk/targetSdk 36`, `arm64-v8a` + `x86_64`, 16 KB page alignment; AndroidX/AGP pinned to compatible set until deliberate upgrade.
- Evidence-driven delivery: device/runtime claims require logs/APK/recording or are `unverified`; CI gates and parity/golden/conformance/simulation tests required for core, sync, terminal changes (per `AGENTS.md`).

## Non-goals

- Claude Desktop, WSL paths, macOS terminal launching, Windows registry/env cleanup, desktop tray, desktop auto-update, portable mode (not applicable on Android).
- Play Store distribution or Play-only integrations as required dependencies.
- Bundling/redistributing proprietary CLIs without redistribution rights.
- Implementing WebView-based or GPLv3-linked terminal solutions.
- Snapshot-only multi-device sync as an end state (only a time-bounded, explicitly labeled fallback if merge CI kill criterion triggers).

## Success signal

- On a real device (no root, no Termux, in self-contained mode) a user can install a supported tool, launch and interact with it in the native terminal, switch providers, and have traffic routed by the local proxy; and can sync changes with another device/client via merge-based sync with conflicts surfaced (not silently lost). This is verified by passing P0 proofs, parity/conformance/simulation tests, and releaseable `full`/`libre` artifacts with documented licenses. If a P0 kill criterion triggers, a documented Companion-mode fallback scope is ready and the pivot decision/evidence is recorded.

## Assumptions

- State baseline from `docs/agents/STATE.md` (2026-11-10; T06 done, T01 probe green but 30-min soak pending, T02/T04/T05/T07 not started, T03 partial) is **provisional/unverified** pending evidence refresh; user reports no new P0 evidence since 2026-11-10.
- Milestones in `docs/PLAN.md` (M1~W6, M2~W10, M3~W16, M4~W24, M5~W28–30) are directional but usable; schedule depends on device access, P0 outcomes, upstream churn, and legal/runtime-repo readiness.
- UX vision artifacts in `_bmad-output/planning-artifacts/ux-designs/ux-aiaio-2026-11-13/` (`DESIGN.md`, `EXPERIENCE.md`) are the confirmed visual/behavior reference for Compose UI work unless superseded.

## Open Questions

- What is the status and timeline for the `aiaio-runtime` repo, bottle signing keys, and per-tool redistribution rights required before publishing `full`/`libre` artifacts? (currently unknown)
