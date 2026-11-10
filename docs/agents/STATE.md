# Agent state

Updated: 2026-11-10
Phase: **0 — proofs / go-no-go**
Plan: `docs/PLAN.md`

## Board

| ID | Task | Owner | Status |
|---|---|---|---|
| T01 | Shizuku exec proof | Runtime | Blocked on T06 APK (probe methods not implemented yet) |
| T02 | Bottle install proof | Runtime | Not started |
| T03 | Config round-trip proof | Core | Not started |
| T04 | PTY proof | Terminal | Not started |
| T05 | Proxy SSE proof | Core | Not started |
| T06 | Build + 16 KB proof | Android | In progress (CI: Rust + parity green, APK job iterating) |
| T07 | Device baseline bench | Human | Not started |

Status values: `Not started`, `In progress`, `Blocked`, `Unverified`, `Done`.

## Already done

- Forked upstream at `5e9812e` (v4.0.28), remote `upstream` configured; full history fetched
  (`git fetch upstream --unshallow`, 3,549 commits) because GitHub rejects shallow pushes.
- Repo scaffold: `core/` Rust workspace, `android/` Gradle project (5 modules), agent docs,
  ADRs, bottle manifest schema, CI workflows.
- Decisions locked: package id `io.github.kingkaonix.aiaio`, GitHub-first distribution,
  native Compose terminal, merge-based sync, matching desktop build.
- Rust workspace verified locally with Rust 1.99.1: `cargo test`, `cargo clippy -D warnings`
  and `cargo fmt --check` all pass. CI runs the same on Rust 1.95 and is green.
- Shizuku 13.1.5 API verified against `RikkaApps/Shizuku-API` source:
  - user services extend the AIDL `Stub`, have a public no-arg constructor, are **not**
    declared in the manifest, and are instantiated by Shizuku by class name;
  - `UserServiceArgs(ComponentName)` → `daemon`, `tag`, `processNameSuffix`, `debuggable`,
    `version`;
  - `bindUserService` / `unbindUserService(args, conn, remove)` and
    `add/removeRequestPermissionResultListener`.
- Dependency audit against AAR `minCompileSdk` metadata: late-2026 AndroidX releases require
  compileSdk 37 + AGP 9.1. Catalog is pinned to the newest versions compatible with
  compileSdk 36 / AGP 8.13.2 (see comment in `android/gradle/libs.versions.toml`).
- GitHub: `gh` authenticated as `KingKaonix`, public repo
  <https://github.com/KingKaonix/aiaio> created, `main` pushed, tags pushed.
- CI plumbing fixed along the way:
  - `setup-android` must not install the removed legacy `tools` package → `packages:
    "platform-tools"`;
  - push triggers were initially dropped for the new repo (verified with a temporary probe
    workflow, then removed);
  - `cargo ndk` resolves the workspace from the working directory, so the Android lib build
    runs from `core/` (`-o ../android/app/src/main/jniLibs`); the repo-root
    `.cargo/config.toml` is still found by walking up.
- Dev phone used for device proofs: Android 14 / SDK 34, Motorola moto g play 2024, Shizuku
  already installed (`moe.shizuku.privileged.api`). Agent can install and drive the APK with
  `vscreen` on the invisible second screen.

## Next 5 actions

1. Get the `android` workflow green (`githubDebug` + `libreDebug` APKs, unit tests, lint,
   16 KB check) and record the run id here.
2. Download the debug APK from the CI run and install it on the dev phone; confirm the
   `:core` AIDL ping and Shizuku status panel render (screenshot as evidence).
3. Implement the T01 probe: add `mkdirs` / `writeFile(path, bytes, mode)` / `exec` to
   `IRuntimeService`, write a script under `/data/local/tmp/aiaio/probe`, run it as uid 2000,
   surface the output in the status screen.
4. Run T01 on the phone (30-minute screen-off repeat included) and attach evidence to
   `docs/agents/tasks/T01-shizuku-exec.md`.
5. If T01 passes, start `core/` extraction (Phase 1); T05 can run in parallel on desktop.

## Blockers

- **T06 not green yet**: the APK job is the only failing CI job; see latest
  `gh run list -R KingKaonix/aiaio`.
- **Real device required** for T01/T02/T04/T07; emulators cannot validate Shizuku. A device is
  available (see above) but the probe code does not exist yet.
- **No local JDK/Android SDK**: Rust runs locally now, but APK builds still happen only in
  GitHub Actions (Android build-tools/aapt2 for Linux aarch64 are not distributed).
- `aiaio-runtime` repo (bottle hosting) does not exist yet; needed before release work.

## Invariants to re-check every week

- `targetSdk = 36`, `minSdk = 26`, `arm64-v8a` + `x86_64`, 16 KB page alignment.
- AndroidX versions stay within `minCompileSdk <= 36` and `minAgp <= 8.13.2` until the
  compileSdk/AGP upgrade lands.
- No GPLv3 linkage; no Play-only SDK.
- `AGENTS.md` definition of done honored.
- Upstream rebase status recorded here (last rebase, conflicts, parity run).
