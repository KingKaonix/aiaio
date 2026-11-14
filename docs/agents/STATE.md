# Agent state

Updated: 2026-11-13
Phase: **0 — proofs / go-no-go** (baseline provisional/unverified; Companion fallback in parallel)
Plan: `docs/PLAN.md`
Spec: `_bmad-output/specs/spec-aiaio-completion/` (SPEC, issues, completion-plan, companion-mode, stories 1-8)

## Board

| ID | Task | Owner | Status |
|---|---|---|---|
| T01 | Shizuku exec probe | Runtime | Done (device-evidenced 2026-11-13) — probe previously green; 30-min foreground-service soak verified via in-app log (`ticks=30/30`, `uid=2000`, 12:04:33–12:34:52, `soak complete`) on Motorola moto g play 2024 / Android 14 (SDK 34), `io.github.kingkaonix.aiaio.debug`; evidence `docs/agents/evidence/T01-soak-complete-2026-11-13.jpg` (+ prior). Failure-mode UI not exhaustively re-verified. |
| T02 | Bottle install proof | Runtime | Blocked / unverified — no `aiaio-runtime` assets, no signed manifest/test bottle, no install flow in current build; rights/signing unknown (cannot execute proof) |
| T03 | Config round-trip proof | Core | In progress (wired, CI green, device probed-pass for covered steps) — runtime bridge + core-service `fs.*` wiring/probe landed (`2461837d`,`9b294adf`,`8f7bfe34`,`91340c1e`,`cbba77d6`, CI 43503506432/43513148814/43514905341 green). On-device probes passed visible covered steps on `cbba77d6` and on current build `43529923244` (`065f7910…`, 2026-11-14): JSON/JSONC/TOML/YAML fixtures rt=ok with mode/len shown, `listDir`/rename/restore ok, traversal rejected, `concurrent: w=100 fail=0 match=99 complete=true` (`T03 ok ... kill-test manual`). Full persisted log readback and mid-write kill-test evidence still pending (vscreen/UI truncation, no adb). Evidence: `docs/agents/evidence/T03-probe-log-2026-11-14-current-build.jpg`. |
| T04 | PTY proof | Terminal | Not started / unverified — no `forkpty`/PTY fd AIDL or terminal UI in current build; implementation required before device proof |
| T05 | Proxy SSE proof | Core | Not started / unverified — no proxy runtime/controls observable in current build; core/desktop tests and implementation+device evidence required |
| T06 | Build + 16 KB proof | Android | Done (CI) + device launch observed 2026-11-13 on Motorola moto g play 2024 (Android 14/SDK 34), debug `0.1.0-debug` Phase 0; artifact hashes/alignment not re-attached this session |
| T07 | Device baseline bench | Human | In progress (partial) — device/app observed; most metrics unverified without adb/dumpsys/Macrobenchmark access (vscreen only) |
| T08 | Companion-mode fallback scope | Core/UI/Product | In progress — scope/feature matrix documented in `_bmad-output/specs/spec-aiaio-completion/companion-mode.md` + `docs/agents/tasks/T08-companion-fallback-scope.md`; formal review/approval and pivot-time TBD resolution pending (Story 2) |
| T09 | P0 evidence refresh + go/no-go | Human/Pods | In progress (partial, decision recorded 2026-11-13) — T01/T06 evidenced, T07 partial, T02 blocked, T03/T04/T05 unverified; **self-contained go/no-go: not closable (proceed with remediation), Companion fallback not triggered but kept ready** |

## Go/no-go (T09, recorded 2026-11-13)

- **Decision:** Self-contained path **NO-GO for closure / not closable at this time**. Proceed with remediation; do not claim self-contained viability complete.
- **Basis:** T01 Shizuku exec+30-min soak evidenced pass on device; T06 CI green + device launch observed; T07 partial baseline only. T03 device probe passes covered file-bridge steps on `cbba77d6` and again on current build `43529923244` (2026-11-14, fixtures/list/rename/restore/traversal, mode/len visible, `concurrent: w=100 fail=0 match=99 complete=true`) but full persisted log readback and mid-write kill-test evidence pending. T02 probe fixed in code locally (`ac92e691`, unpushed/no CI APK yet) but device proof on fixed build and production/runtime artifacts+rights/signing remain blocked/pending. T04/T05 require implementation and/or device evidence (PTY plumbing+UI, proxy slice/tests/runtime). Evidence/tooling gaps remain (artifact hashes/alignment for all installed builds, adb/dumpsys/bench metrics).
- **Kill criteria:** No demonstrated P0 exec/Shizuku proof failure (T01 passed). Incompleteness is not a proven non-viability trigger → **full Companion-mode activation NOT triggered**. T08 fallback scope remains review-ready for evidence-backed pivot if future proofs show non-viability.

Status values: `Not started`, `In progress`, `Blocked`, `Unverified`, `Done`.

## Already done

- Forked upstream at `5e9812e` (v4.0.28), remote `upstream` configured; full history fetched.
- Repo scaffold: `core/` Rust workspace, `android/` Gradle project (5 modules), agent docs, ADRs, bottle manifest schema, CI workflows.
- Decisions locked: package id `io.github.kingkaonix.aiaio`, GitHub-first distribution, native Compose terminal, merge-based sync, matching desktop build.
- Rust workspace previously verified locally (Rust 1.99.1) and in CI (Rust 1.95): `cargo test`, `cargo clippy -D warnings`, `cargo fmt --check` (re-verify in current env/CI when running).
- Shizuku 13.1.5 API verified against `RikkaApps/Shizuku-API` source (AIDL stub, no manifest declaration, `UserServiceArgs`, bind/unbind, permission listener).
- Dependency audit: AndroidX catalog pinned to versions compatible with compileSdk 36 / AGP 8.13.2.
- GitHub: public repo <https://github.com/KingKaonix/aiaio> (main pushed).
- CI: android workflow green for scaffold/build proofs (including deterministic debug signing cache fix noted historically); parity workflow configured.
- Device baseline (historical, 2026-11-10): Moto g play 2024 (Android 14/SDK 34), Shizuku installed. T01 probe **passed** on device (evidence `docs/agents/evidence/T01-probe-result.jpg`, `docs/agents/evidence/T01-device-shizuku-next.jpg`) — `uid=2000`, runtime path ops/exec via Shizuku service OK.
- **2026-11-13 device evidence refresh (via Agent-VScreen, no adb/root):** on Motorola moto g play 2024 / Android 14 (SDK 34), app `io.github.kingkaonix.aiaio.debug` (Phase 0 screen) observed. T01 probe re-run showed `uid=2000`, mkdirs/write(mode=700)/exec (`probe-user=shell`, `probe-dir=/data/local/tmp/aiaio/probe`) — `docs/agents/evidence/T01-probe-result-2026-11-13.jpg`. 30-min foreground-service soak log read: `ticks=30/30`, `first: start uid=2000 at 12:04:33 wake=Awake`, `last: done uid=2000 at 12:34:52`, `soak complete` — `docs/agents/evidence/T01-soak-complete-2026-11-13.jpg`. T01 marked Done (failure-mode UI not exhaustively re-verified). Device access now available through vscreen for further proofs.
- **2026-11-13 planning/spec:** created formal spec workspace `_bmad-output/specs/spec-aiaio-completion/` with `SPEC.md`, `issues.md`, `completion-plan.md`, `companion-mode.md`, `stories.yaml` (1-8, all checkpoints true), `.memlog.md`. Decisions recorded: baseline provisional prior to this refresh, milestones directional/usable, UX reference confirmed (`_bmad-output/planning-artifacts/ux-designs/ux-aiaio-2026-11-13/`), pursue self-contained path gated by P0 while preparing Companion-mode fallback in parallel. Open: runtime repo/signing/per-tool redistribution rights unknown.

## Next 5 actions

1. **T03 closeout evidence:** capture full persisted probe log (`/data/local/tmp/aiaio/probe/t03/t03-result.txt`) and perform/document mid-write kill-test (original file intact) to satisfy acceptance criteria; concurrency/mode/len now observed on device but full log artifact + kill-test remain. Then mark T03 done.
2. **T04:** implement PTY path (native helper + `forkpty` in Shizuku service, master fd via `ParcelFileDescriptor` over Binder, app reader/writer/resize/signal wiring, terminal UI/session) and run interactive/resize/Unicode/signal/30-min stability + cleanup checks on device.
3. **T05:** extract minimal proxy slice into core, add loopback+token+peer-UID+SSE/mock-transform tests (CI/desktop), then prove on device build with streamed request evidence.
4. **T02/T07 + runtime readiness:** define/publish test bottle + signed manifest (resolve `aiaio-runtime` repo, signing, redistribution rights) for T02 install/update/rollback proof; collect T07 metrics with proper bench/shell tooling (adb/dumpsys/Macrobenchmark) when available.
5. **T09/T08:** update evidence/hashes and revisit go/no-go as T03–T05/T02/T07 land; keep T08 review-ready for evidence-backed pivot if self-contained proofs fail.

## Blockers

- **P0 go/no-go not closable:** T01/T06 evidenced (2026-11-13 refresh/launch), but T02 (artifacts/rights), T03 (device adapter+harness), T04 (PTY implementation), T05 (proxy implementation+tests/device evidence), T07 (bench tooling access) remain incomplete.
- **Implementation gaps on current build:** no bottle install flow, no PTY/terminal session, no exposed proxy runtime; runtime file-bridge AIDL/adapter/harness landed in repo but on-device T03 proof/evidence still pending on CI APK.
- **Environment/tooling limits:** no local JDK/Android SDK and no `cargo` detected here (CI needed for builds/tests); vscreen provides UI evidence but not adb/dumpsys/Macrobenchmark, limiting T07 and low-level PTY/proxy diagnostics.
- **`aiaio-runtime` repo/manifest/signing/rights unknown:** blocks publishable/test bottle pipeline and T02 completion as specified.

## Invariants to re-check every week

- `targetSdk = 36`, `minSdk = 26`, `arm64-v8a` + `x86_64`, 16 KB page alignment.
- AndroidX versions stay within `minCompileSdk <= 36` and `minAgp <= 8.13.2` until compileSdk/AGP upgrade lands.
- No GPLv3 linkage; no Play-only SDK; no secret leakage.
- `AGENTS.md` definition of done honored (device evidence for runtime claims).
- Upstream rebase status recorded here (last rebase, conflicts, parity run).
