# T06 — Build + 16 KB proof

**Owner:** Android pod · **Depends on:** none · **Status:** Scaffolded

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

- [ ] CI green on `main`; artifact downloadable.
- [ ] `targetSdkVersion` in the APK = 36, `minSdkVersion` = 26.
- [ ] `libaiaio_core.so` present for `arm64-v8a` and `x86_64`.
- [ ] `zipalign -c -P 16` passes.
- [ ] `readelf` shows `0x4000` alignment on all LOAD segments.
- [ ] APK installs and launches on a real Android 14+ device.

## Evidence

Workflow run URL, APK SHA-256, `zipalign`/`readelf` output, and a launch screenshot.
