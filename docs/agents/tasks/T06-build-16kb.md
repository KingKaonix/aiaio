# T06 — Build + 16 KB proof

**Owner:** Android pod · **Depends on:** none · **Status:** Done (CI) + device launch observed

## Goal

A CI-produced debug APK that targets API 36, contains both arm64-v8a and x86_64 Rust core
libraries, and passes the 16 KB page-size alignment check.

## Steps

1. `.github/workflows/android.yml` builds `core` for `aarch64-linux-android` and
   `x86_64-linux-android` with `cargo-ndk`, outputs into `android/app/src/main/jniLibs`.
2. `gradle :app:assembleDebug` with JDK 21, SDK 36, NDK 27.2.
3. Run `zipalign -c -P 16 -v 4 app-debug.apk`.
4. Check each `.so` with `readelf -lW` — every `LOAD` segment aligned to `0x4000`.
5. Upload the APK as a workflow artifact with its SHA-256.

## Acceptance criteria

- [x] CI green on `main`; artifact downloadable. *(historical STATE: android workflow/run 42906877098; latest android run on main observed successful: 42938162281 / commit 6ab4a626, 2026-11-11 — artifacts/hashes not re-downloaded in this environment)*
- [ ] `targetSdkVersion` in the APK = 36, `minSdkVersion` = 26. *(not re-verified from artifact in this session; per prior CI/state)*
- [ ] `libaiaio_core.so` present for `arm64-v8a` and `x86_64`. *(not re-verified in this session; per prior CI/state)*
- [ ] `zipalign -c -P 16` passes. *(per prior CI/state)*
- [ ] `readelf` shows `0x4000` alignment on all LOAD segments. *(per prior CI/state)*
- [x] APK installs and launches on a real Android 14+ device. *(observed 2026-11-13 via Agent-VScreen on Motorola moto g play 2024, Android 14/SDK 34: `io.github.kingkaonix.aiaio.debug` launched to Phase 0 screen, shows `Build flavor=full, version=0.1.0-debug`, core ping ok, Shizuku ready; `docs/agents/evidence/P0-app-phase0-2026-11-13.jpg`)*

## Evidence

- CI (prior): workflow run 42906877098 recorded in STATE (green, both flavors, lint, 16 KB). Latest android workflow success observed via GitHub API: run 42938162281 (`android: declare FOREGROUND_SERVICE_DATA_SYNC permission`, commit 6ab4a6268e4fd462c9f8851d8c0c45b53763ff31, 2026-11-11T01:19:53Z). APK SHA-256 / zipalign / readelf outputs not attached in-repo this session.
- Device launch (2026-11-13): `docs/agents/evidence/P0-app-phase0-2026-11-13.jpg` (vscreen). Device: Motorola moto g play - 2024, Android 14 (SDK 34).
