# Companion-Mode Fallback Scope

**Purpose:** Define an actionable fallback if Phase 0/self-contained kill criteria trigger (e.g. Shizuku exec/PTY/bottle/proxy proofs fail) so pivot is explicit, not ad-hoc. Parallel scope to self-contained mode (CAP-1, CAP-6). Status: **planned fallback (not activated, evidence-backed activation required)**.

## Trigger and activation
- **Triggers (per PLAN/SPEC kill criteria):**
  - **Full self-contained viability blocked (primary Companion trigger):** P0 proofs/evidence show Shizuku-mediated exec, runtime/bottle execution in `/data/local/tmp/aiaio/`, required file-bridge behavior, or loopback proxy/PTY foundations are non-viable on supported devices after mitigation attempts.
  - **Targeted mitigations (not automatic full Companion activation):** terminal TUI not acceptable by ~W12 → ship non-interactive auth path first and terminal beta with tracked gaps; sync fails 3-device CI by ~W14 → temporary explicit single-writer snapshot fallback (labeled) while merge work continues.
- **Activation gate:** Companion mode is activated only by a recorded decision in `docs/agents/STATE.md` citing device/runtime/task evidence (T01–T07 as applicable), affected capabilities, and unsupported actions. No implied activation.
- **Invariants preserved in all modes:** no root, no Termux dependency, no GPLv3 linkage, no Play-only required deps, no secrets leakage (logs/backups/deep links/sync), no execution from app-private storage, no alternate privileged path outside Shizuku if/when privileged ops are used.

## Responsibilities and feature matrix (planned fallback)

Status keys: **Local** = implemented/usable in-app on device; **Delegated** = performed by external host/runtime or remote session; **Unavailable** = not provided in Companion v1 fallback; **TBD** = deferred to pivot, must be resolved with rationale before activation/release claim. All rows are **planned** until evidenced.

| In-scope area | Companion status | Local in app (target) | Delegated / unavailable | Rationale / notes |
|---|---|---|---|---|
| Providers, presets, endpoints, routing config, profiles, settings | **Local** | Store/edit/switch provider metadata, presets, endpoint/routing/profile/settings data via core DB + schema-driven UI; import/export of config where supported. | Live local CLI process control and runtime-applied tool mutations **Unavailable** without a reachable runtime; remote application **TBD**. | Core value (manage/sync configs) remains useful without local tool exec. UI must label actions that require a runtime as unavailable/delegated. |
| MCP / skills / prompts | **Local** | Own-workspace browse/create/edit/delete/import-export of MCP servers, skills, and prompts as managed assets/config. | In-runtime install/execution/discovery that depends on local CLIs **Unavailable**; delegated host execution **TBD**. | Asset/config management is separable from execution; no claim that a local tool consumed the asset unless verified. |
| Import/export, backups (SAF) | **Local** | SAF-based import/export/backup of app-managed data and supported artifacts. | — | Independent of local runtime/PTY; must follow secret-handling and license/provenance rules. |
| Sync (merge-based) | **Local** | Merge-based sync engine/protocol, device identity, conflict/Sync Doctor UX, WebDAV/S3/LAN transports as implemented and capability-gated (CAP-3). | — | Multi-device config/asset consistency remains first-class in fallback. Snapshot-only only as explicit, time-bounded, labeled kill-criterion mitigation if 3-device CI fails. |
| Proxy (local routing layer) | **Local (control-plane) / TBD (end-to-end)** | In-app/core loopback proxy config/control, auth/peer protections, and routing policy management where feasible without local CLI spawn. | End-to-end routing to locally spawned CLIs **Unavailable** in Companion fallback; routing to a delegated/remote runtime endpoint **TBD** (requires explicit auth/transport contract). | Useful for config parity and future re-entry; must not imply live local tool traffic without a target runtime. |
| Usage / quota views | **Local** (data-dependent) | Display stored/imported/remote-sourced usage, quota, logs, pricing views available to core/app. | Live local CLI log/PTY-derived collection **Unavailable**; delegated host telemetry ingestion **TBD**. | Only show data with known provenance/freshness; mark unavailable live sources explicitly. |
| Runtime / bottles (install, exec, update, rollback) | **Unavailable** | Status, configuration intent, and rights/provenance metadata presentation may be shown. | Self-contained local install/exec/update/rollback in `/data/local/tmp/aiaio/` **Unavailable**; delegated host/runtime management **TBD** (v1 may omit). | No self-contained runtime claims without P0 evidence and rights-gated artifacts. No execution from app-private storage. |
| Terminal / PTY (local CLI/TUI sessions) | **Unavailable** | Session/history metadata views only if backed by stored data (no live claim). | Local PTY/TUI sessions to on-device CLIs **Unavailable**; remote terminal/session integration **TBD** (explicit contract/threat model required). | Native terminal remains a product goal; fallback must not promise usable local TUI/auth flows. |

> At pivot time, refine any **TBD** rows to Local/Delegated/Unavailable with rationale, implementation/UX notes, and `verified/unverified` evidence in `STATE.md` and relevant task specs.

## UX and degradation
- **Mode clarity:** explicit status/banner and Settings state indicating active mode (self-contained/Companion), evidence-linked reason when in Companion mode, and per-feature availability.
- **Graceful degradation:** hide or disable actions requiring local runtime/PTY/bottles/local CLI spawn with concrete rationale; never present stubbed, simulated, or delegated-but-unconfigured execution as working.
- **Onboarding:** mark Shizuku setup + runtime/bottle install + terminal prerequisites as optional/not required in Companion mode; preserve a visible, safe path to retry self-contained enablement when environment/proofs change.
- **Parity messaging:** scope parity claims to the active mode and verified evidence (avoid implying M2/M4 local-runtime behaviors in Companion mode).

## Security and licensing
- **Security baseline unchanged:** loopback-only proxy binds where enabled, token + peer-UID checks, allow-listed/no-interpolation mediation for any privileged/file/process operations, Keystore-backed secret handling, `allowBackup=false`, no secrets in logs/backups/deep links/sync payloads without encryption.
- **Privileged path:** Shizuku remains the only privileged path if used; Companion mode must not add root/Termux/adb-assist or other alternate privileged execution paths.
- **Licensing/rights:** no redistribution or execution claims for tools not installed/cleared locally; ADR-0005 rights/provenance/manifest rules apply to any offered artifacts. `libre` flavor constraints remain enforced.

## Data and sync
- Core DB/config/sync model (CAP-2/CAP-3) remains source of truth for manageable data; merge-based sync remains required for multi-device config/asset consistency (no silent loss).
- Device identity, tombstones/oplog semantics, and conflict handling unchanged; any companion host integration (if added) uses explicit sync/transport/auth contracts, not ad-hoc file copies.
- Local-vs-remote data provenance must be attributable in UI/logging where relevant (stored/imported/remote-sourced).

## Migration / pivot path
- **To Companion (pivot):** record trigger evidence and kill-criterion mapping; freeze self-contained-only claims; enable Companion mode flags/UI; publish per-area support matrix (resolving TBDs in scope for v1 pivot) and unsupported actions; update task statuses/evidence.
- **To self-contained (re-entry):** when proofs later pass on supported devices with evidence, allow opt-in Shizuku/runtime setup and enable runtime/terminal/bottle/local-exec/proxy-local-target features incrementally, each gated by verification and documented in `STATE.md`.
- **Mode transitions:** transitions must be user-visible, reversible where safe, and must not discard or corrupt synced/local data.

## Open items (must be resolved and recorded at pivot/spec refinement)
1. **Host integration in v1:** Companion mode is "local management + sync only" vs integrates with a desktop/remote host/runtime; define minimum viable delegated workflows if any.
2. **Remote auth/transport contract:** if delegated host/remote terminal/proxy target is supported, specify auth, transport, pairing/trust, threat model, failure modes, and secret handling.
3. **Per-feature mode support details:** finalize TBD rows (proxy end-to-end, delegated runtime/terminal, remote usage telemetry, remote config application) with rationale, UX behavior, and release gating.
4. **Rights/runtime-repo dependencies:** confirm `aiaio-runtime` repo, signing keys, and per-tool redistribution rights status and whether they affect any Companion-mode artifact/download behavior.
5. **ADR linkage:** add/update ADR if Companion activation introduces new cross-cutting decisions beyond current ADRs (none required for this planning-only scope draft).
