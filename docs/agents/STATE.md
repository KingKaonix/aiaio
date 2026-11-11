# Agent state

Updated: 2026-11-10
Phase: **0 — proofs / go-no-go**
Plan: `docs/PLAN.md`

## Board

| ID | Task | Owner | Status |
|---|---|---|---|
| T01 | Shizuku exec probe | Runtime | In progress — probe green on device; detached soak died at tick 6 (process updated mid-run), foreground-service replacement built, device run pending |
| T02 | Bottle install proof | Runtime | Not started |
| T03 | Config round-trip proof | Core | In progress — `FileBridge` + `fs.*` land with tests; device adapter pending |
| T04 | PTY proof | Terminal | Not started |
| T05 | Proxy SSE proof | Core | Not started |
| T06 | Build + 16 KB proof | Android | Done — run 42906877098 (both flavors, lint, 16 KB) |
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
  already installed (`moe.shizuku.privileged.api` UI, `af.shizuku.plus.api` server). Agent can
  install and drive the APK with `vscreen` on the invisible second screen.
- T01 probe **passes on the device** (`98fd830` build, `docs/agents/evidence/T01-probe-result.jpg`):
  `uid=2000`, `mkdirs` → `/data/local/tmp/aiaio/probe`, `write` mode 700, `exec` →
  `probe-uid=2000`, `probe-user=shell`, `probe-dir=/data/local/tmp/aiaio/probe`.
- Debug signing is now deterministic: CI creates the key at `$RUNNER_TEMP/aiaio-debug.keystore`,
  caches it (`android-debug-keystore-v2`), passes `AIAIO_DEBUG_KEYSTORE` to Gradle and prints
  the certificate fingerprint (SHA-256 `b6a7737b…3d76`, `CN=Android Debug`). The earlier
  `~/.android/debug.keystore` cache never saved, which is why every run produced a new key and
  in-place updates failed with "package conflicts with an existing package".
- Device facts worth remembering: taps only land while the phone screen is on (`key 224`
  wakes it, keys work with the screen off); a stale virtual display fails launches with
  `SafeActivityOptions.checkPermissions` and is fixed by `vscreen release` + relaunch; the
  Shizuku permission dialog is hosted by `af.shizuku.plus.api` on the main display and can be
  moved with `vscreen handoff in`.

## Next 5 actions

1. CI-build the foreground-service soak (`android: keep the Shizuku user service alive in a foreground service for the T01 screen-off soak`), install over `b36440d` (proves in-place update again with the cached key).
2. Start the 30-minute screen-off soak from the app; expect `ticks=30/30`, `wake=Asleep` ticks, an `after ...` probe line and a `done` line; read it via "Read soak log" and attach the card text + `vscreen status` as evidence.
3. Flip T01 to Done and start Phase 1: `core/` extraction (Tauri-free `dispatch(json)->json` already lands `fs.*`; next: DB v18 with sync columns, golden tests). T03/T05 can run in parallel without a device.
4. Keep T06 green: every CI build must update the installed APK in place (no uninstall).
5. Then T02 (bottle install proof) and T04 (PTY proof) on the same phone.

## Blockers

- **T01 soak not yet verified**: the old detached-script soak died at tick 6 when `b36440d` was installed over `98fd830`; the replacement (`SoakService`) needs a full 30-minute screen-off device run.
- **Real device required** for T01/T02/T04/T07; emulators cannot validate Shizuku.
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
