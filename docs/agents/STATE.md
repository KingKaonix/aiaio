# Agent state

Updated: 2026-11-10
Phase: **0 — proofs / go-no-go**
Plan: `docs/PLAN.md`

## Board

| ID | Task | Owner | Status |
|---|---|---|---|
| T01 | Shizuku exec proof | Runtime | Not started |
| T02 | Bottle install proof | Runtime | Not started |
| T03 | Config round-trip proof | Core | Not started |
| T04 | PTY proof | Terminal | Not started |
| T05 | Proxy SSE proof | Core | Not started |
| T06 | Build + 16 KB proof | Android | Scaffolded (CI written, not yet run) |
| T07 | Device baseline bench | Human | Not started |

Status values: `Not started`, `In progress`, `Blocked`, `Unverified`, `Done`.

## Already done

- Forked upstream at `5e9812e` (v4.0.28), remote `upstream` configured.
- Repo scaffold: `core/` Rust workspace, `android/` Gradle project (5 modules), agent docs,
  ADRs, bottle manifest schema, CI workflows.
- Decisions locked: package id `io.github.kingkaonix.aiaio`, GitHub-first distribution,
  native Compose terminal, merge-based sync, matching desktop build.
- Rust workspace verified locally with Rust 1.99.1: `cargo test`, `cargo clippy -D warnings`
  and `cargo fmt --check` all pass. Resolved dependency MSRVs are ≤ 1.85, so the CI pin
  (Rust 1.95) is safe.
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

## Next 5 actions

1. Authenticate `gh`, create `kingkaonix/aiaio`, push `main`, run the `android` workflow.
2. Fix CI build errors until T06 artifacts exist (APK + 16 KB check green).
3. Get a device (Android 14+) with Shizuku and run T01.
4. If T01 passes, run T02 + T03 (runtime + core bridge).
5. Start `core/` extraction (Phase 1) only after T01 passes; T05 can run in parallel on the
   desktop build.

## Blockers

- **No GitHub auth / repo yet**: `gh` 2.103.3 is installed on the Termux box but not logged in,
  and the repository has no `origin` remote.
- **No git identity** configured (`user.name` / `user.email`), so the baseline commit cannot be
  created yet.
- **No local JDK/Android SDK**: Rust runs locally now, but APK builds still happen only in
  GitHub Actions (Android build-tools/aapt2 for Linux aarch64 are not distributed).
- **Real device required** for T01/T02/T04/T07; emulators cannot validate Shizuku.
- GitHub repo `aiaio` + `aiaio-runtime` must exist before release work; CI assumes the repo owner
  is `kingkaonix`.

## Invariants to re-check every week

- `targetSdk = 36`, `minSdk = 26`, `arm64-v8a` + `x86_64`, 16 KB page alignment.
- AndroidX versions stay within `minCompileSdk <= 36` and `minAgp <= 8.13.2` until the
  compileSdk/AGP upgrade lands.
- No GPLv3 linkage; no Play-only SDK.
- `AGENTS.md` definition of done honored.
- Upstream rebase status recorded here (last rebase, conflicts, parity run).
