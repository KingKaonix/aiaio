# T09 — P0 evidence refresh and go/no-go (Story 1)

**Owner:** Human + Runtime/Core/Terminal pods · **Depends on:** device access, installable CI APK · **Status:** In progress (partial, decision recorded) — T01/T06 evidenced 2026-11-13; T02/T03/T04/T05/T07 blocked or unverified; self-contained go/no-go not closable
**Spec:** `_bmad-output/specs/spec-aiaio-completion/` (CAP-1) · **Story:** 1

## Goal

Refresh and record device/runtime evidence for T01–T07 to establish a verified self-contained viability baseline and explicit go/no-go/pivot inputs.

## Context

- `docs/agents/STATE.md` baseline (2026-11-10) was provisional/unverified; device access is now available via Agent-VScreen.
- 2026-11-13 refresh (Motorola moto g play - 2024, Android 14 / SDK 34, `io.github.kingkaonix.aiaio.debug`, via vscreen/no adb/root): **T01 Done** (probe + 30-min foreground-service soak evidenced), **T06 Done (CI) + device launch observed**, **T07 partial** (device/app observed, most metrics unverified without adb/dumpsys/bench tooling).
- **T02 Blocked/unverified:** no `aiaio-runtime` repo/assets, no signed test manifest/bottle, no install flow in current build; rights/signing unknown.
- **T03 In progress (device-blocked):** core `FileBridge`/`fs.*` + tests exist but runtime AIDL/device adapter/harness missing for on-device proof.
- **T04 Not started/unverified:** no `forkpty`/PTY fd AIDL or terminal UI in current build.
- **T05 Not started/unverified:** no proxy runtime/controls observable in current build; core tests + implementation/device evidence required.
- Blockers: implementation gaps, runtime-repo/signing/rights unknown, no local JDK/SDK/cargo in this env (CI required), vscreen cannot provide adb/dumpsys/Macrobenchmark-level metrics.

## Steps

1. Install latest CI-built debug APK (deterministic signing, in-place update) on test device; record build/commit hash. *(build/install context partially observed 2026-11-13; exact CI artifact hash not re-attached)*
2. **T01:** run 30-min screen-off foreground-service soak; capture ticks/wake/after/done, probe output, `vscreen status`/logcat/screenshot evidence. **Done (evidenced 2026-11-13).**
3. **T04:** prove PTY (`forkpty` via Shizuku, fd over Binder, interactive shell, echo/signals/resize/Unicode, 30-min stability, session cleanup) with transcript/fd/RSS evidence. **Pending implementation + device proof.**
4. **T05:** prove proxy SSE (loopback, token+peer-UID, transform stream, backpressure, concurrency/leak checks) — desktop/core tests + Android/device path as applicable. **Pending implementation + evidence.**
5. **T02:** bottle install/verify/unpack/health-check/update/rollback (or rights-gated official-source path) once runtime manifest/test bottle available; document if blocked by `aiaio-runtime`. **Blocked on artifacts/rights.**
6. **T03:** device file-bridge adapter + byte-identical round-trip/atomicity/kill-test/rejection evidence. **Pending adapter/harness + evidence.**
7. **T07:** baseline metrics (cold start, core ready, RSS, proxy/battery/terminal perf, runtime disk) on available device(s). **Partial.**
8. Update `docs/agents/STATE.md`, task files (T01–T07), and spec workspace with pass/fail, evidence links, and kill-criterion decision (proceed / mitigate / pivot to Companion). **Recorded 2026-11-13 (partial).**

## Acceptance criteria

- [x] Each of T01–T07 is `Done`, `Blocked` (with cause), or `Unverified` with explicit evidence/missing-evidence noted — no implied success. *(T01 Done, T06 Done+launch, T07 partial/in-progress, T02 Blocked/unverified, T03 In progress device-blocked, T04/T05 Not started/unverified)*
- [x] Kill criteria evaluated and decision recorded; if triggered, Companion-mode activation documented (see T08). *(see Decision)*
- [ ] Evidence attached per task format (device/OS/ABI, Shizuku version, logs/screenshots/recordings/hashes). *(partial: device/OS/screenshots/log reads attached for T01/T06/T07 context; hashes/ABI/thermal/logcat/bench artifacts incomplete; T02–T05 evidence pending)*

## Decision (2026-11-13, P0 go/no-go)

- **Self-contained path:** **NO-GO for closure / not closable at this time.** Shizuku exec viability is positively evidenced (T01) and CI/device launch evidenced (T06), but required P0 proofs are incomplete: T02 blocked on missing runtime artifacts/rights, T03/T04/T05 lack required implementation/device evidence, T07 lacks full metrics/tooling.
- **Kill-criterion evaluation:** No recorded proof demonstrates P0 exec/Shizuku viability failure (T01 passed). Incompleteness is due to blocked artifacts and unimplemented/unverified surfaces, not a demonstrated self-contained impossibility. **Full Companion-mode activation is NOT triggered at this time.**
- **Action:** **Proceed with remediation on the self-contained path** (implement/unblock T02–T05/T07 proofs and collect evidence). Keep Companion-mode fallback scope **ready for evidence-backed activation** if future proofs/constraints show non-viability (per T08 and PLAN/SPEC triggers). Record updated evidence and revisit this decision as proofs complete.

## Evidence

- 2026-11-13 (vscreen/device, no adb/root): Motorola moto g play - 2024, Android 14 / SDK 34, 720x1600@212dpi, Agent-VScreen 1.1.7; app `io.github.kingkaonix.aiaio.debug` (`flavor=full, version=0.1.0-debug`) Phase 0 screen, core ping ok, Shizuku ready.
  - T01: `docs/agents/evidence/T01-probe-result-2026-11-13.jpg` (probe `uid=2000`, mkdirs/write mode 700/exec `probe-user=shell`, `probe-dir=/data/local/tmp/aiaio/probe`), `docs/agents/evidence/T01-soak-complete-2026-11-13.jpg` (soak `ticks=30/30`, start 12:04:33, end 12:34:52, `soak complete`).
  - T06/T07 context: `docs/agents/evidence/P0-app-phase0-2026-11-13.jpg`.
- Prior: `docs/agents/evidence/T01-device-shizuku-next.jpg`, `docs/agents/evidence/T01-probe-result.jpg`.
- T02–T05, T07 full bench evidence: pending (see task files).
