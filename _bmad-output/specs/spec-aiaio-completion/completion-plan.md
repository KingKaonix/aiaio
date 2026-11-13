# Completion Plan (aiaio)

Phased, evidence-gated plan to reach a releasable aiaio. Aligned to `docs/PLAN.md` milestones (M1–M5) and kill criteria; assumes 1 human + agent pods, device access for proofs, and contract-first parallelism. Schedule is directional but usable. **Scope decision:** pursue self-contained path gated by P0 proofs while preparing Companion-mode fallback scope in parallel.

## Phase A — Unblock & de-risk (P0, immediate) → go/no-go + fallback readiness
**Goal:** prove/disprove self-contained viability and have Companion fallback ready (CAP-1). **Gate:** T01–T07 evidenced (or pivot decision recorded) and Companion scope defined.
- **Evidence refresh & proofs:** baseline in `STATE.md` (2026-11-10) is provisional; no new P0 evidence reported since 2026-11-10. Complete with device/runtime artifacts in `docs/agents/STATE.md` + task specs:
  - T01: 30-min screen-off foreground-service soak (`ticks/after/done`, wake behavior).
  - T02: bottle download → verify (manifest/signature/SHA-256) → unpack to `/data/local/tmp/aiaio/` → exec/health.
  - T03: config round-trip via file bridge/device adapter.
  - T04: PTY via Shizuku `forkpty`, fd passed to app, basic interactive I/O.
  - T05: proxy loopback bind, token + peer-UID, SSE stream behavior.
  - T07: device baseline bench.
- **Companion-mode fallback (parallel track):** define and document Companion-mode scope/contract (what stays local: store/presets/MCP-skills-prompts workspace/proxy/sync/import-export as applicable vs delegated runtime/terminal; Shizuku/tooling implications; UX/degradation; security/licensing boundaries) so pivot is actionable per kill criteria.
- Create/maintain `docs/agents/tasks/T0x*.md` (acceptance, test cmds, evidence links); add ADRs for irreversible choices.
- Stand CI gates: `cargo test/clippy/fmt`, `gradle test/lint`, 16 KB; wire scaffolding for golden parity, terminal stream snapshots, 3-device sync sim.
- **Decision:** if exec/Shizuku proofs fail → activate Companion-mode scope (re-plan against it). Record outcome.

## Phase B — Foundations (P1, early P3/P4/P5) → M1, M2
**Goal:** stable contracts and correctness-critical cores (CAP-2,3,4,5). **Parallel pods, contract-first.**
1. **Core extraction (Core):** lock `dispatch`/event/schema; extract services/db/proxy/parsers incrementally from `src-tauri/`; DB v18 with sync columns/devices/oplog hooks; port upstream golden/fixtures as oracle; keep desktop Tauri shell building on shared core as canary.
2. **Sync model early (Sync):** implement metadata/tombstones/deterministic merge + oplog in core; **3-device CI simulation from day 1** (concurrency, offline/reconnect, clock skew, tombstone GC, interruption, corruption, 100MB); then transports (WebDAV/S3 w/ ETag, LAN pairing/TLS/mDNS) and Sync Doctor/conflict UX + optional encryption.
3. **Terminal conformance (Terminal):** record ANSI/TUI streams for target CLIs; implement parser/screen/renderer (SGR/attrs, alt screen, scroll regions, mouse, bracketed paste, OSC 8/52, wide/combining, scrollback/reflow) with IME/modifier UX and perf validation (60fps@80x24); gate per ~W12 kill/mitigation.
4. **Runtime & security (Runtime):** hardened Shizuku service (path/command allow-lists, no interpolation), PTY fd passing, bottle lifecycle (verify/install/update/rollback/health), `aiaio-runtime` manifest/signing/licensing/provenance; proxy hardening (loopback, token, peer-UID, FGS, SSE). Secrets via Keystore/encrypted storage. **Resolve/open:** runtime repo, signing keys, per-tool redistribution rights (status unknown).
5. **Compose foundation & M1 (UI):** `:core` AIDL client, schema-driven renderer/editors, theme/nav/i18n/a11y scaffold; integrate confirmed UX contracts (`_bmad-output/planning-artifacts/ux-designs/ux-aiaio-2026-11-13/` DESIGN/EXPERIENCE, D1/O1, Stealth Slate v5, V1 buttons, ambient wolf, translucent panels); **M1:** real provider list/CRUD from real DB on device.

**M2 exit (~W10 target, directional):** on device, install a tool, launch in native terminal, switch provider, proxy routes — no Termux, evidenced (or equivalent Companion-mode milestone if pivoted).

## Phase C — Feature parity waves (P6) → M3, M4
**Goal:** in-scope parity with merge-based sync in lockstep (CAP-3,4,5,6). Each wave gated by fixtures + device evidence, scoped to active mode.
- **Waves:** A providers/presets/endpoints/modes/failover → B MCP/skills/prompts → C proxy modes/rectifier/optimizer → D sessions/usage/heatmap/logs/pricing → E quotas/balance/OAuth → F import/export/backups/SAF/deeplinks → G profiles/settings.
- Evolve sync: transports, conflict UI/Sync Doctor, encryption, LAN; align matching desktop build on shared core + merge engine at M3 for cross-device validation.
- Terminal/runtime hardening continues against recorded streams and real CLIs.
- Maintain Companion-mode scope artifacts in sync if not yet activated (avoid drift).

**M3:** MCP/skills/prompts + sync desktop↔phone. **M4:** full parity (fixtures green per wave).

## Phase D — Hardening & release (P7, P8) → M5
**Goal:** production quality and distributable artifacts (CAP-5,6).
- **Hardening:** device matrix, proxy/sync soak (8h screen-off, 100 streams), perf/memory/cold-start budgets, fuzz/security review (Binder perms, UID checks, allow-lists, secret handling), a11y, screenshot tests.
- **Release:** `full` and `libre` flavors (FOSS-only/anti-features, proprietary tools hidden in `libre`), signed GitHub Releases/Obtainium, onboarding/docs, `aiaio-runtime` repo+mirrors with license metadata and rights compliance (rights/signing status currently unknown—must resolve); name clearance; F-Droid submission prep.
- **Desktop:** ship aligned desktop build with merge engine.

## Cross-cutting controls (continuous)
- **Evidence & DoD:** no “done” without runtime evidence or explicit `unverified`; task specs + STATE updated each merge.
- **Testing:** golden/parity (core), vttest/recorded-stream conformance (terminal), 3-device sim (sync), screenshot/a11y, 16 KB/build gates in CI.
- **Security/legal:** no GPLv3 linkage, no secret leakage, rights-checked bottles, allow-listed mediation.
- **Upstream hygiene:** nightly parity, weekly rebase/triage recorded in `STATE.md`.

## Kill/mitigation triggers (act explicitly)
- P0 exec/Shizuku proofs fail → activate Companion-mode fallback (scope prepared in parallel).
- Terminal not acceptable by ~W12 → ship non-interactive auth path first; terminal as beta with tracked gaps.
- Sync fails 3-device CI by ~W14 → temporary explicit single-writer snapshot fallback (labeled), keep merge work prioritized.

## Immediate next actions (ordered)
1. Refresh/record P0 evidence (T01 soak, T04 PTY, T05 proxy SSE) on device in `STATE.md` + task specs; draft Companion-mode fallback scope/contract.
2. Add task specs for T01–T07 and first P1 extraction slice; port initial upstream golden tests.
3. Draft core DB v18 sync-columns/devices/oplog schema + 3-device sim skeleton; define (or spike) `aiaio-runtime` manifest/licensing/signing model despite unknown status.
