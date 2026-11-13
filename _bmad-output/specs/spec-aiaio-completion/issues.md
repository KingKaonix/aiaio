# Issues and Gaps (aiaio completion)

Source basis: `docs/PLAN.md`, `docs/agents/STATE.md` (2026-11-10, baseline provisional/unverified), `AGENTS.md`, ADRs, and sampled code in `core/` + `android/` (reviewed 2026-11-13). User update (2026-11-13): no new P0 device evidence since 2026-11-10; runtime repo/signing/rights status unknown; Companion-mode fallback scope to be prepared in parallel. Load-bearing for CAP-1..CAP-6.

## 1. Phase 0 go/no-go incomplete and baseline unverified (highest risk) — CAP-1
- **Status (provisional):** T06 Done (CI build/16 KB). T01 probe green on device but **30-min screen-off soak pending** (foreground-service replacement built, device run not evidenced). T02/T04/T05/T07 Not started. T03 In progress (`FileBridge`/`fs.*` with tests; device adapter pending). **No new P0 evidence reported since 2026-11-10.**
- **Impact:** P0 gates self-contained viability; unproven Shizuku reliability, PTY, bottle exec, proxy SSE risk foundation rework. Lack of evidence refresh means plan confidence is low.
- **Blockers/constraints:** real device required (emulators insufficient), no local JDK/SDK (CI-only APK), `aiaio-runtime` repo absent.
- **Action:** refresh evidence and complete T01–T07 with device/runtime artifacts in `docs/agents/STATE.md` and task specs; record pass/fail and kill-criterion decisions. **In parallel, define Companion-mode fallback scope** so pivot is actionable.

## 2. Core extraction is scaffold-level, not P1 — CAP-2
- **Observed:** `core/crates/core` provides dispatch + `fs.*` + ports/file bridge; `runtime-proto` provides envelopes/sync meta types; `ffi` exists. Extracted `services/`, `database/` (DB v18 + sync columns/devices/oplog), `proxy/`, `deeplink/`, `session_manager/`, `live/`, `mode/` and shared-core desktop canary/golden parity wiring are not yet evidenced.
- **Impact:** largest effort/schedule risk; without incremental extraction + golden oracle, behavior drifts from upstream.
- **Action:** contract-first `dispatch`/events/schema; incremental extraction with upstream golden/parity tests; DB schema designed with sync metadata from start; keep desktop building on shared core.

## 3. Merge-based sync largely unimplemented — CAP-3
- **Observed:** `android/sync` contains basic in-memory LWW merge primitives/tests only.
- **Missing:** real DB per-entity `updated_at/origin_device/deleted_at/rev`, `devices`, append-only oplog, snapshot+oplog protocol, WebDAV/S3 hardening (ETag/If-Match), LAN direct (QR pairing, TLS+pinned cert, mDNS), WorkManager/resumable/background, Sync Doctor/conflict UI, optional encryption (age/libsodium)+Keystore, 3-device CI simulation (concurrency/offline/clock skew/tombstone GC/interruption/corruption/100MB).
- **Impact:** trust-destroying data loss if incorrect; must be designed/tested early with schema.
- **Action:** land schema+merge+oplog and 3-device CI sim early; layer transports/UX/encryption against tested model.

## 4. Native terminal far from TUI-conformance — CAP-4
- **Observed:** `android/terminal` has parser/buffer/session/view + basic tests; parser handles subset (ground/escape/CSI moves/erase) with SGR/modes/OSC largely stubbed/ignored.
- **Missing vs plan:** alt screen, scroll regions, mouse reporting, bracketed paste, OSC 8/52, robust wide-char/emoji/combining, scrollback/reflow, IME/modifier toolbar polish, PTY-over-Shizuku fd integration proven (T04), vttest+recorded-stream golden conformance per target CLI, 60fps dirty-region perf validation.
- **Impact:** broken auth/TUI UX; kill/mitigation trigger if not acceptable by ~W12.
- **Action:** build conformance harness from recorded CLI streams early; iterate parser/screen/renderer against fixtures; device-test keyboard/IME/feel.

## 5. Runtime, bottles, proxy, security not end-to-end proven — CAP-5
- **Observed:** Shizuku/runtime scaffolding exists; no evidenced T02 bottle install or T04 PTY proof; proxy SSE/security behaviors not device-evidenced (T05).
- **Unknown:** status/timeline for `aiaio-runtime` repo, bottle signing keys, and per-tool redistribution rights (needed before `full`/`libre` publish). **Currently unknown.**
- **Missing/risks:** signed manifest+SHA256+license/provenance per tool, rights-gated install for proprietary CLIs, allow-lists/no-interpolation enforcement, Keystore/secret storage hardening, loopback+token+peer-UID+FGS/SSE soak behavior.
- **Impact:** cannot claim self-contained/no-Termux runtime; legal/takedown and F-Droid risks if redistribution mishandled.
- **Action:** define manifest/signing/licensing pipeline and resolve rights; implement/verify install/verify/run/rollback and proxy/runtime security properties with device evidence.

## 6. Android UI/product parity at placeholder stage — CAP-6
- **Observed:** Compose app shows placeholder tabs with Phase 0 screen; no schema-driven form renderer/editors tied to core schemas, no real data flows for providers/MCP/skills/sessions/usage/quotas/sync/runtime/settings.
- **Available (confirmed):** UX design artifacts (`_bmad-output/planning-artifacts/ux-designs/ux-aiaio-2026-11-13/` DESIGN/EXPERIENCE + key screens/wolf demo, translucent panels, tunables) define visual/behavior direction but are not yet implemented.
- **Action:** wire core client/AIDL, schema renderer, and M1 real-provider flow against UX contracts; execute parity waves A–G against fixtures and device evidence (scoped for self-contained or Companion mode as decided).

## 7. Companion-mode fallback undefined — CAP-1/CAP-6 (new scope decision)
- **Status:** user directed preparing Companion-mode fallback scope in parallel with self-contained path.
- **Gap:** concrete scope, boundaries, UX, and degraded-capability contract for Companion mode are not yet defined/documented.
- **Action:** produce Companion-mode scope/plan (what remains local vs delegated, what still requires Shizuku/runtime/terminal/sync, security/licensing implications) so pivot is actionable without ad-hoc re-planning.

## 8. Delivery/process and quality infrastructure gaps — cross-cutting
- **Gaps:** `docs/agents/tasks/*.md` coverage not evident; many changes lack device evidence; golden/parity, terminal conformance, 3-device sync sim, screenshot/a11y test suites not yet standing; no local JDK/SDK constrains iteration (CI-only APK).
- **Upstream drift:** forked at v4.0.28 (`5e9812e`) with large pending extraction; monthly rebase/parity discipline needed.
- **Action:** add task specs/ADRs, enforce evidence-based DoD, stand test harnesses early, record weekly rebase/parity status in `STATE.md`.
