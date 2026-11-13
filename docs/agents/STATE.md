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
| T03 | Config round-trip proof | Core | In progress (implementation added, device proof pending) — core `FileBridge`/`fs.*` + tests exist; Android runtime bridge extended (AIDL v3 read/stat/list/rename/remove/restore/atomic write, service+`RuntimeFileBridge`, Phase 0 T03 probe harness), but on-device round-trip/kill-test/concurrency evidence not yet collected |
| T04 | PTY proof | Terminal | Not started / unverified — no `forkpty`/PTY fd AIDL or terminal UI in current build; implementation required before device proof |
| T05 | Proxy SSE proof | Core | Not started / unverified — no proxy runtime/controls observable in current build; core/desktop tests and implementation+device evidence required |
| T06 | Build + 16 KB proof | Android | Done (CI) + device launch observed 2026-11-13 on Motorola moto g play 2024 (Android 14/SDK 34), debug `0.1.0-debug` Phase 0; artifact hashes/alignment not re-attached this session |
| T07 | Device baseline bench | Human | In progress (partial) — device/app observed; most metrics unverified without adb/dumpsys/Macrobenchmark access (vscreen only) |
| T08 | Companion-mode fallback scope | Core/UI/Product | In progress — scope/feature matrix documented in `_bmad-output/specs/spec-aiaio-completion/companion-mode.md` + `docs/agents/tasks/T08-companion-fallback-scope.md`; formal review/approval and pivot-time TBD resolution pending (Story 2) |
| T09 | P0 evidence refresh + go/no-go | Human/Pods | In progress (partial, decision recorded 2026-11-13) — T01/T06 evidenced, T07 partial, T02 blocked, T03/T04/T05 unverified; **self-contained go/no-go: not closable (proceed with remediation), Companion fallback not triggered but kept ready** |

## Go/no-go (T09, recorded 2026-11-13)

- **Decision:** Self-contained path **NO-GO for closure / not closable at this time**. Proceed with remediation; do not claim self-contained viability complete.
- **Basis:** T01 Shizuku exec+30-min soak evidenced pass on device; T06 CI green + device launch observed; T07 partial baseline only. T02 blocked (runtime repo/manifest/test bottle + rights/signing unknown). T03/T04/T05 require implementation and/or device evidence (file-bridge adapter/harness, PTY plumbing+UI, proxy slice/tests/runtime). Evidence/tooling gaps remain (artifact hashes/alignment, adb/dumpsys/bench metrics).
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

1. **CI/tooling:** run and keep green `ci-tooling` on GitHub Actions for pushed changes (APK/report artifacts, hashes, parity) to validate the built toolchain.
2. **T03:** collect device evidence on CI-built debug APK using Phase 0 T03 probe (round trip/stat/list/rename/restore, traversal rejection, concurrent atomic writes) and add explicit kill-test + full fixture/100-write logs; wire Rust core `fs.*` dispatch to Android runtime bridge if needed for end-to-end core path.
3. **T04:** implement PTY path (native helper + `forkpty` in Shizuku service, master fd via `ParcelFileDescriptor`, terminal UI wiring) and run interactive/resize/Unicode/signal/30-min stability checks on device.
4. **T05:** extract minimal proxy slice into core, add loopback+token+peer-UID+SSE/mock-transform tests (CI/desktop), then prove on device build with streamed request evidence.
5. **T02/T07 + runtime readiness:** define/publish test bottle + signed manifest (resolve `aiaio-runtime` repo, signing, redistribution rights) for T02 install/update/rollback proof; collect T07 metrics with proper bench/shell tooling (adb/dumpsys/Macrobenchmark) when available. Keep T08 review-ready for pivot; revisit T09 go/no-go as evidence lands.

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
