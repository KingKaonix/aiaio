# Completion Status (execution tracker)

Updated: 2026-11-13 · Spec: `SPEC.md` · Stories: `stories.yaml` (all `spec_checkpoint=true`, `done_checkpoint=true`)

This tracks what can be completed in-repo now vs what remains blocked on device/human/legal/long-running implementation. **Project is not fully complete**: P0 proofs unverified on device, core extraction/terminal/sync parity/UI/hardening/release are multi-phase efforts.

## Completed now (this pass)
- [x] Formal spec workspace: `SPEC.md`, `issues.md`, `completion-plan.md`, `companion-mode.md`, `stories.yaml`, `.memlog.md` (coherence/preservation/story validation pass).
- [x] Companion-mode fallback scope drafted (`companion-mode.md`, Task T08).
- [x] P0 evidence refresh tracking task created (T09); STATE updated to 2026-11-13 with provisional baseline, T08/T09, blockers, next actions.
- [x] Foundation prep tasks scaffolded: T10 (core/DB18 sync foundations), T11 (sync 3-device sim skeleton).

## Blocked / requires external action
- [ ] **Device proofs & evidence (CAP-1):** T01 soak, T02, T03 device adapter/kill-test, T04, T05 (device), T07 — require real device, CI APK, and (T02) runtime artifacts/rights.
- [ ] **Runtime distribution/legal (CAP-5/CAP-6):** `aiaio-runtime` repo, signing keys, per-tool redistribution rights/provenance — status unknown.
- [ ] **CI/device build loop:** no local JDK/Android SDK here; APK builds in GitHub Actions only.

## Major implementation remaining (by story/phase)
- [ ] **Story 1 (CAP-1):** complete T01–T07 evidence, record go/no-go/pivot.
- [ ] **Story 2 (CAP-1,CAP-6):** finalize Companion-mode feature matrix, UX, ADR/STATE linkage; keep pivot-ready.
- [ ] **Story 3 (CAP-2,CAP-3):** Tauri-free core extraction, dispatch/events/schema contracts, DB v18 sync metadata/devices/oplog, golden/parity oracle, desktop canary.
- [ ] **Story 4 (CAP-3):** deterministic merge+oplog+snapshot, 3-device CI sim, transports (WebDAV/S3/LAN), Sync Doctor/conflict UI, optional encryption.
- [ ] **Story 5 (CAP-5,CAP-1):** Shizuku runtime hardening, bottle lifecycle/manifest/security, proxy loopback/token/peer-UID/FGS/SSE, secrets handling.
- [ ] **Story 6 (CAP-4):** PTY-to-VT pipeline, TUI conformance (vttest/recorded streams), IME/modifiers, perf 60fps@80x24 or documented mitigation.
- [ ] **Story 7 (CAP-6,CAP-2):** core client/AIDL, schema-driven Compose UI per UX refs, real provider list/CRUD (M1).
- [ ] **Story 8 (CAP-6,3,4,5):** parity waves A–G, hardening (soak/perf/security/a11y), `full`/`libre` releases, docs/onboarding, desktop alignment (M2–M5).

## How to measure “complete”
Per SPEC success signal: on a real device (self-contained mode) install/launch/interact with a supported tool in native terminal, switch providers, route via local proxy, and merge-sync with another device/client with conflicts surfaced (not lost) — verified by P0 proofs, parity/conformance/simulation tests, and releaseable `full`/`libre` artifacts with documented licenses. If P0 kill criterion triggers, documented Companion-mode fallback scope activated and recorded.
