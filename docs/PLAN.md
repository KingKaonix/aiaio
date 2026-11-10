# aiaio — Native Android App for CC Switch

**Plan v3 (final) — 2026-11-10**
Source: https://github.com/farion1231/cc-switch (v4.0.28, MIT)
Target: self-contained APK, no root, Shizuku allowed, Jetpack Compose UI, native terminal, full parity, multi-device sync.

---

## 0. Decisions (locked)

| Decision | Value |
|---|---|
| App name | **aiaio** |
| Package id | `io.github.kingkaonix.aiaio` (stable forever). No Play Store dependency → no domain ownership requirement. |
| Distribution | **GitHub Releases / Obtainium first**, F-Droid later. No Play Store (incompatible with privileged helper + runtime downloads). |
| Build flavors | `full` (GitHub: bottles may include proprietary CLIs) and `libre` (F-Droid: FOSS-only runtime, anti-features declared). |
| `libre` scope | FOSS tools only (Codex, Gemini CLI, OpenCode, Pi, Hermes, OMP, …). Proprietary tools (e.g. Claude Code) are hidden in this flavor with a short explainer, not prompted for install. |
| UI | Jetpack Compose + Material 3. React UI is dropped for Android. |
| Terminal | **Native Compose terminal** (no WebView, no xterm.js, no GPLv3 library). |
| Backend | Reuse upstream Rust core behind a transport-agnostic `dispatch()` façade. |
| Privileged helper | Shizuku user service (shell UID) for runtime, files, PTY. Optional at runtime; app degrades gracefully. |
| Runtime | **Bottles hosted in our own repo/CDN**, signed manifest, per-tool recipes. |
| Sync | **Non-negotiable, first-class**: WebDAV + S3 **plus LAN direct sync**, with a real merge engine (upstream is snapshot-only — see §3.7). |
| Sync defaults | LAN pairing offered first; WebDAV/S3 optional and set up explicitly (no cloud required to use the app). |
| Desktop | Matching `aiaio` desktop build shipped with the merge engine; upstream v4 snapshots importable read-only. |
| Bottle rights | Redistribute only tools we are cleared to redistribute; otherwise the manifest points to the official source and the app installs with in-app consent. |
| Team | 1 human (device validation, legal, release, UX calls) + agent pods orchestrated by the assistant. See §12. |
| Device floor | `minSdk 26`, `compileSdk/targetSdk 36`, 16 KB page-size compliant. |
| License | MIT (inherit upstream) + Apache-2.0 for new modules as convenient. Avoid GPLv3 linkage. |

---

## 1. TL;DR

- Feasible, but it is a **new product on top of the upstream Rust core**, not a port.
- Three hard problems, in order: **(1)** Android W^X + sandbox (runtime must live in `/data/local/tmp` via Shizuku), **(2)** a **real multi-device merge layer** (upstream sync overwrites the whole DB), **(3)** a **native VT terminal** good enough for Claude Code/Codex/OpenCode TUIs.
- With agent pods: **MVP ≈ 3–4 months**, **full parity ≈ 8–12 months** wall-clock, assuming daily access to a test device and that Shizuku proofs pass in Phase 0.
- If Phase 0 Shizuku proofs fail: pivot to Companion mode (no local runtime) and re-scope.
- Agent velocity is real for code volume; it does **not** remove device testing, Shizuku validation, licensing, or release work. Plan for a human bottleneck, not a code bottleneck.

---

## 2. Constraints that shape everything

### 2.1 Android W^X and sandbox
| Location | Exec allowed | Owner |
|---|---|---|
| `nativeLibraryDir` (`lib/arm64-v8a/*.so`) | Yes | app |
| App home (`/data/data/<pkg>/files`) | **No** (`targetSdk ≥ 29`) | app |
| `/data/local/tmp` | Yes | `shell` |
| `/sdcard/Android/data/<pkg>` | No (`noexec`) | app |

→ Runtime cannot live in the sandbox. It lives in `/data/local/tmp/aiaio/` and is driven by a Shizuku user service (shell UID). App-side Rust core ships in `jniLibs` and runs as the app.

### 2.2 F-Droid implications
- F-Droid builds `libre` from source, reproducible, FOSS-only. Proprietary tool bottles cannot be part of that flavor.
- `bash`, `git`, `ripgrep`, `node` are redistributable (GPL/MIT/Apache) but **executed as separate programs**, not linked → no license contagion to our MIT app.
- F-Droid reviewers will flag runtime downloads as non-free add-ons. Ship `libre` with FOSS tools only (Codex, Gemini CLI, OpenCode, etc.) and a clear anti-feature note; proprietary tools (Claude Code) remain a `full`-flavor/GitHub feature.

### 2.3 Runtime hosting and legality
- Bottles in a separate repo `aiaio-runtime` (release assets, signed `manifest.json` with SHA-256, size, license, source URL, redistribution status).
- **Legal gate:** only redistribute binaries we have rights to. Proprietary CLIs (e.g. Anthropic's) may forbid redistribution. If permission is absent, the manifest must point to the official download URL and the app must fetch at install time with explicit consent — still "from our repo" in the sense of our manifest/CDN, without us being the redistributor.
- GitHub release asset limit is fine (2 GB/file); keep bottles split per tool so users download only what they use.

### 2.4 What "all features" excludes on Android
Claude Desktop (no Android build), WSL paths, macOS terminal launching, Windows registry/env-var cleanup, desktop tray, desktop auto-update, portable mode. Everything else is in scope, including sessions, usage, quotas, skills, MCP, prompts, profiles, deep links, sync.

---

## 3. Architecture

### 3.1 Components

```
┌───────────────────────────────────────────────────────────────┐
│ app (Compose, app UID, targetSdk 36)                          │
│  UI · :terminal (VT emulator) · SAF · intents · notifications │
│  RuntimeManager ── bind ──► Shizuku user service (shell UID)  │
│  CoreClient ───── bind ──► :core process                      │
│  ProxyForegroundService (specialUse) · WorkManager (sync)     │
└──────────────┬──────────────────────────────┬─────────────────┘
               │ Binder/JSON-RPC              │ Binder/AIDL + PTY fd
┌──────────────▼──────────────┐   ┌───────────▼─────────────────┐
│ :core (app UID)             │   │ Shizuku user service         │
│  Rust core (libaiaio_core)  │   │  files · exec · forkpty      │
│  DB · adapters · proxy      │   │  /data/local/tmp/aiaio/      │
│  usage · quotas · sync      │   │   runtime/ home/ tools/      │
└──────────────┬──────────────┘   └──────────────────────────────┘
               │ 127.0.0.1:PORT (token + peer UID)
┌──────────────▼──────────────┐
│ Local CLIs in runtime       │
│ Desktop aiaio via WebDAV/S3 │
│ or LAN direct sync (TLS)    │
└─────────────────────────────┘
```

### 3.2 Core extraction
- New `core/crates/core`: move `services/`, `database/`, `proxy/`, `deeplink/`, `session_manager/`, `live/`, `mode/` out of `src-tauri`.
- Remove Tauri coupling: `State<AppState>` → `AppCore`; `AppHandle::emit` → `EventSink`; `dirs::home_dir` → `PathProvider`; process spawn → `ProcessHost`; keychain → `SecretStore`.
- Single façade:
  ```rust
  fn dispatch(req_json: &str) -> String;         // command + args → result
  fn subscribe(event_sink: EventSink);            // status/progress/log streams
  ```
- Keep the desktop Tauri shell building on the same core as a canary (also gives desktop the sync engine for free).
- Port upstream `tests/golden` + fixtures unchanged as the compatibility oracle.

### 3.3 Runtime (self-contained, no Termux)
- Shizuku onboarding: detect installed/running/permission, guided wireless-debugging setup, persistent status.
- Bottle lifecycle: download → verify manifest signature + SHA-256 → unpack to `/data/local/tmp/aiaio/runtime` → per-tool install/link → health check.
- Package registry: per-tool recipe (source, version, deps, post-install, capabilities), pinned versions, update checks, rollback.
- Runtime home: `/data/local/tmp/aiaio/home` holds `.claude`, `.codex`, `.config/opencode`, etc. All file I/O mediated by the service with path allow-lists, atomic writes, and symlink rejection.
- PTY: `forkpty` in a small native helper launched by the service; the master fd is passed to the app as `ParcelFileDescriptor` (no byte copying through Binder).
- No-runtime fallback: store, presets, MCP/skills/prompts (own workspace), proxy, sync, import/export all remain usable.

### 3.4 Proxy
- Runs inside `:core`; binds `127.0.0.1`, random port, bearer token, `SO_PEERCRED` peer-UID check.
- FGS type `specialUse`, persistent notification (status/port/provider/stop), quick-settings tile.
- Takeover rewrites runtime config to point at the local port; failover/model mapping/rectifier reused from upstream.
- Soak target: 8 h screen-off, 100 concurrent streams, no leaks.

### 3.5 Native terminal (first-class workstream)
**Why:** login flows (`claude login`, `codex login`), TUI tools, diagnostics. WebView/xterm.js rejected by decision; Termux terminal lib rejected (GPLv3).

- `:terminal` module (Kotlin + Compose), no GPL dependencies.
- **PTY** from the Shizuku service via `forkpty`; fd passed over Binder.
- **Parser:** ECMA-48/xterm subset state machine — CSI/OSC/DCS, SGR (16/256/truecolor), cursor save/restore, scroll regions, alternate screen, DEC private modes, bracketed paste, mouse reporting (SGR 1006), OSC 8 hyperlinks, OSC 52 clipboard, wide chars/emoji, combining marks.
- **Screen model:** primary + alt buffers, ring-buffer scrollback (configurable), optional reflow on resize, dirty-region tracking.
- **Renderer:** Compose `Canvas` + `TextMeasurer` with a glyph cache and dirty regions; bundled monospace font; target 60 fps at 80×24.
- **Input:** IME with composing text, hardware keyboard, modifier toolbar (Ctrl/Alt/Esc/Tab/arrows), paste confirmation, resize handling.
- **Conformance:** vttest subset + recorded ANSI streams captured from each CLI (Claude Code, Codex, Gemini, OpenCode) replayed as golden screen snapshots in CI.
- **Effort:** 6–9 agent-weeks + 2–3 human-weeks of keyboard/device testing. Top-2 risk.

### 3.6 Sync: what upstream does and why it must change
Upstream transport (`services/sync_protocol.rs`) is a **whole-snapshot sync**: `db.sql` + `skills.zip` + `manifest.json`, with a mutex and auto-sync on table changes. No `updated_at`, no tombstones, no merge. Desktop↔phone with that model = last writer wins and silent data loss.

**Design for aiaio (must land in Phase 1 schema work):**
- Per-entity sync columns: `updated_at`, `origin_device`, `deleted_at` (tombstone), `rev`.
- `devices` table: `device_id` (UUID in Keystore-backed prefs), name, platform, last_seen, last_pull/push heads.
- Append-only `oplog` (per device JSONL) uploaded alongside `db.sql`; snapshot kept for backward compatibility with desktop v4.
- Merge policy:
  - rows: LWW by `(updated_at, origin_device)`; tombstones win over older updates; deterministic tie-break.
  - settings: LWW per key.
  - skills: content-addressed blobs (SHA-256), so two devices never overwrite each other's assets.
  - provider `id`s are stable; imports dedupe by `(app, name, endpoint)`.
- Transports (all use the same protocol):
  - **WebDAV** and **S3** (reuse upstream, add `If-Match`/ETag to prevent lost updates).
  - **LAN direct**: QR pairing + TLS with pinned self-signed cert + device key exchange; mDNS optional. Local-first, works without cloud.
- Background: WorkManager, network constraints, resumable transfers, exponential backoff, foreground for large payloads.
- UX: Sync Doctor (device list, last sync, conflicts, "keep desktop / keep phone / merge"), dry-run preview, encrypted export.
- Encryption: optional sync passphrase; payloads sealed (age/libsodium); credentials in Keystore.
- Tests (CI, from week 1): 3-device simulation, concurrent edits, offline-then-reconnect, clock skew, tombstone GC, interrupted upload, corrupt remote, 100 MB DB.

### 3.7 Security baseline
- Keystore-wrapped DEK + SQLCipher (or field-level AES-GCM); no plaintext keys in logs/backups/deep links.
- Binder signature permissions; loopback token + UID check; no shell interpolation (argv arrays, allow-lists).
- `allowBackup=false`; opt-in crash reporting; redact secrets.
- Bottle integrity: signed manifest, per-file SHA-256, pinned versions.
- Threat model note: Shizuku grants shell-level power — the app must never expose it to third parties, and the runtime service must enforce path and command allow-lists.

---

## 4. Compose UI

### 4.1 Navigation
One activity, Navigation Compose, bottom bar: **Providers · MCP · Skills · Sessions · More**. Usage/quotas/profiles/sync/settings live under More and deep-link into detail screens. Edge-to-edge, predictive back, 48 dp targets, dynamic color, 4 locales (en/zh/ja/de).

### 4.2 Schema-driven forms (critical for 15 adapters)
- Field types: `text`, `secret`, `url`, `select`, `multiselect`, `modelPicker`, `json`, `toml`, `yaml`, `code`, `switch`, `list`.
- Schema JSON emitted by the Rust core (single source of truth); Compose renderer + validation.
- Custom screens only for OAuth, aggregation builder, provider comparison, and MCP connectivity.
- Code editor: Sora Editor (Apache-2.0) or CodeView; JSON/TOML/YAML/Markdown + lint.

### 4.3 Screen map
```
Home / Providers      cards, tabs (Direct/Routing/Aggregation), switch, usage badge
Provider editor       schema form + raw editor + model fetch + endpoint test
MCP                   unified list, per-app toggles, connectivity, import/resync
Skills                installed/discover/repos/updates/per-app toggles
Prompts, Profiles     file/template management, apply/clear
Sessions              per-tool list, full-text search, step/tool-call detail
Usage                 heatmap, request logs, cost, pricing, FX
Quotas                subscription/coding-plan/balance, OAuth flows
Sync                  devices, transports, Sync Doctor, conflicts, encryption
Runtime               status, install/update tools, terminal, logs
Settings              proxy, failover, rectifier/optimizer, logging, theme, about
Terminal              full-screen VT with modifier toolbar and session tabs
```

### 4.4 Lists and performance
Paging 3 for sessions/logs; indexes and FTS in the core; precomputed aggregates; screenshot tests (Roborazzi) at 360 dp, 600 dp, tablet.

---

## 5. Parity matrix

| Area | Kind | Notes |
|---|---|---|
| Providers + switch (15 tools) | Schema + custom | Core value |
| Presets (90+), custom endpoints | Schema | Imported from core |
| Direct / Routing / Aggregation | Schema + custom | Aggregation builder custom |
| Failover, model mapping, rectifier, optimizer | Schema | Reuse core |
| MCP (unified + per-app) | Custom + schema | Connectivity via runtime |
| Skills | Custom | Git/zip via runtime |
| Prompts | Custom | Per-app files |
| Profiles | Custom | Apply/clear via core |
| Sessions reader | Custom | Reuse parsers; Paging 3 |
| Usage / heatmap / logs | Custom | Reuse parsers + pricing |
| Quotas / balance / plans | Custom | OAuth + browser + Keystore |
| Usage scripts (rquickjs) | Schema | Gate if NDK build fails |
| WebDAV / S3 / LAN sync | Custom | New merge engine (non-negotiable) |
| Native terminal | Custom | New module |
| Deep links (`ccswitch://` → `aiaio://`) | Intent filter | Reuse parser |
| Import / export, backups | SAF | Reuse |
| Runtime manager | Custom | New |
| Claude Desktop, WSL, Windows env, tray, auto-update | N/A | Replaced or not applicable |

---

## 6. Repo layout

```
aiaio/                              # fork of cc-switch
├── src/  src-tauri/                # upstream desktop (kept, core-extracted)
├── core/
│   ├── crates/core/                # services, db, proxy, sync merge, parsers
│   ├── crates/ffi/                 # JNI + dispatch + event sink
│   └── crates/runtime-proto/       # shared schemas, sync types
├── android/
│   ├── app/                        # Compose UI, navigation, design system
│   ├── core-service/               # :core process, JNI, AIDL, FGS
│   ├── terminal/                   # VT parser, screen model, Compose renderer, PTY client
│   ├── runtime/                    # Shizuku client, user service, bottles, recipes
│   ├── sync/                       # WorkManager, transports, Sync Doctor
│   ├── schema/                     # generated form schemas, i18n resources
│   └── build-logic/
├── tools/bottles/                  # recipes + manifest schema (bottles hosted in aiaio-runtime)
├── docs/agents/                    # task specs, ADRs, playbooks
└── .github/workflows/{android,parity,release}.yml
```
Separate repo: `aiaio-runtime` (release assets + signed manifest + mirrors).

---

## 7. Roadmap (agent-assisted)

Parallel pods: **Core**, **Android UI**, **Terminal**, **Runtime**, **Sync**, **Feature waves**, with a human gate on device-verified acceptance criteria.

| Phase | Elapsed | Deliverable | Gate |
|---|---|---|---|
| **P0 Proofs** | W0–2 | Shizuku exec, bottle, config round-trip, PTY, proxy SSE, 16 KB build | Device proofs pass or pivot to Companion |
| **P1 Core + schema** | W1–6 | Tauri-free core, `dispatch`, AIDL, DB v18 with sync columns | Desktop still builds; golden tests pass |
| **P2 Compose foundation** | W2–7 | Nav, theme, schema renderer, editors, i18n, a11y | Provider CRUD end-to-end on device |
| **P3 Terminal** | W2–10 | VT parser+renderer, PTY, keyboard/IME, conformance suite | Runs Claude Code/Codex TUIs at 60 fps |
| **P4 Runtime** | W4–10 | Shizuku service, bottles, registry, install/run/login | Install + run + switch without Termux |
| **P5 Sync engine** | W3–10 | Merge model, oplog, WebDAV/S3, LAN pairing, Sync Doctor | 3-device simulation passes in CI |
| **P6 Feature waves** | W8–24 | A providers · B MCP/skills/prompts · C proxy modes · D sessions/usage · E quotas · F import/export/deeplinks · G profiles/settings | Parity fixtures green per wave |
| **P7 Hardening** | W18–28 | Device matrix, soak, perf, security, fuzz | No P0/P1 bugs; memory/cold-start budgets |
| **P8 Release** | W24–30 | Signed GitHub releases + updater, `libre` F-Droid flavor, onboarding, docs | Store/repo review + upgrade path |

**Milestones**
- **M1 (W6):** real provider list from real DB in Compose.
- **M2 (W10):** install Claude Code, launch it in the native terminal, switch provider, proxy routes — no Termux.
- **M3 (W16):** MCP/skills/prompts + sync between desktop and phone.
- **M4 (W24):** full feature parity.
- **M5 (W28–30):** public release; F-Droid flavor following.

**Calendar:** MVP (M2) ≈ 3 months; parity ≈ 6–7 months; polished release ≈ 8 months. Add 30–50% if device access is intermittent or upstream introduces breaking changes.

---

## 8. Agent delivery model (team = me + agents)

**Operating model**
- I act as tech lead: decomposition, ADRs, interface contracts, reviews, integration, rebases.
- Subagents run per pod in isolated worktrees; contract-first (proto/schema/IDL) so pods never block on each other.
- Human gates: device proofs, Shizuku behavior, keyboard/terminal feel, UX judgment, legal/licensing sign-off, release signing, final QA.

**Required repo scaffolding (build in Phase 0)**
- `AGENTS.md`: architecture, commands, module boundaries, definition of done.
- `docs/agents/tasks/*.md`: one spec per task with acceptance criteria and test commands.
- ADRs for every irreversible decision (package id, sync semantics, bottle format, DB migration).
- CI gates: `cargo test`, `cargo clippy -D warnings`, `./gradlew test lint detekt`, golden/parity tests, screenshot tests, 16 KB alignment check.
- No merge without: tests, lint, device note (or explicit "unverified" label), and updated docs.
- Nightly: parity run against upstream `main`; weekly: upstream rebase + conflict triage.

**Known agent failure modes and mitigations**
| Failure | Mitigation |
|---|---|
| Hallucinated Android/Shizuku APIs | Device-verified acceptance per task; compile + instrumented tests mandatory |
| Inconsistent UI | Shared design-system package + screenshot tests |
| Merge conflicts across pods | File ownership per pod; contract-first interfaces |
| Context loss between sessions | ADRs + task specs + a `STATE.md` updated on every merge |
| Overconfident "done" | Definition of done includes runtime evidence (logs, APK, screen recording) |

**Throughput assumption:** agents compress typing-heavy phases (core extraction, forms, tests, resources); they do not compress device testing, legal review, or OEM/battery debugging. Plan the critical path around those.

---

## 9. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Shizuku/exec proofs fail | Fatal to self-contained | P0 gate; Companion fallback |
| Native terminal too incomplete for TUIs | Broken login/UX | Early conformance suite; recorded streams; fallback to non-interactive auth |
| Sync merge bugs lose providers/keys | Trust-destroying | Oplog + tombstones + 3-device CI from week 1; dry-run + conflict UI |
| Bottle redistribution illegal | Takedown | Manifest-level source URLs; redistribute only with rights; per-tool license disclosure |
| F-Droid rejects runtime downloads | No F-Droid | `libre` flavor with FOSS-only tools + declared anti-features |
| Upstream churn (3.5k commits) | Rebase pain | Core-only fork, monthly rebase, parity suite |
| OEM battery killers stop proxy/sync | Broken background behavior | FGS + WorkManager + OEM guidance + Doze tests |
| Agent-generated regressions | Quality drift | CI gates, golden tests, small PRs, human review on core/sync/terminal |
| APK/runtime size | Install friction | Per-tool downloads, arm64 first, splits, mirrors |

**Kill criteria**
- P0 exec/Shizuku proofs fail → Companion mode.
- Terminal can't reach acceptable TUI fidelity by W12 → ship non-interactive logins first, terminal as beta.
- Sync merge can't pass 3-device CI by W14 → ship snapshot sync with explicit "one writer" mode, defer merge.

---

## 10. Resolved decisions

All product decisions are closed. Only administrative follow-ups remain:

1. **Name clearance** for "aiaio" (trademark/domain sanity check) before first public release. Package id is final: `io.github.kingkaonix.aiaio`.
2. **Bottle licenses** per tool must be recorded in the runtime manifest before publishing the `full` flavor (who may redistribute what, and from where).
3. **Desktop release cadence**: the desktop `aiaio` build ships alongside Android once the merge engine lands (P5); before that, desktop stays on upstream v4 with import-only compatibility.
