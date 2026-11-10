# aiaio Android

Gradle project for the Android app. Five modules:

| Module | Contents |
|---|---|
| `app` | Compose UI, navigation, theme, Phase 0 status panel |
| `core-service` | `:core` process hosting `libaiaio_core.so`, AIDL `ICoreService`, `NativeCore` JNI bridge |
| `terminal` | VT parser, screen buffer, Compose renderer, session/transport |
| `runtime` | Shizuku client, shell-UID user service, exec plumbing |
| `sync` | merge primitives, transports, WorkManager sync worker |

## Toolchain

- JDK 17+ (CI uses 21), Gradle **8.14.3**, AGP 8.13.2, Kotlin 2.3.21
- compileSdk/targetSdk 36, minSdk 26, NDK 27.2.12479018
- ABIs: `arm64-v8a`, `x86_64`

There is intentionally no Gradle wrapper yet: the wrapper JAR cannot be generated on the
Termux dev box (no JDK), and CI installs Gradle 8.14.3 explicitly. Run tasks with
`gradle -p android ...`; `scripts/build-android.sh` does the full Rust + APK flow.

## Dependency ceiling

Every AndroidX artifact must declare `minCompileSdk <= 36` and
`minAndroidGradlePluginVersion <= 8.13.2` in `META-INF/com/android/build/gradle/aar-metadata.properties`.
Late-2026 releases require compileSdk 37 / AGP 9.1; `gradle/libs.versions.toml` is pinned to the
newest compatible versions and documents this. Bumping compileSdk/AGP is a separate task.

## Native core

`libaiaio_core.so` is built from `core/crates/ffi` and lands in
`app/src/main/jniLibs/<abi>/` (git-ignored). The CI `android` workflow builds it with
`cargo ndk` before assembling the APK. Without the `.so`, the app still runs: `CoreBridge`
reports `core_unavailable` and the Phase 0 panel says so.

## Product flavors

- `github` — full tool catalog; proprietary CLIs may be installed from their official source
  with explicit consent.
- `libre` — FOSS-only catalog; proprietary tools are hidden.

`BuildConfig.TOOL_CATALOG` is the only difference. Debug builds use the `.debug` application id
suffix so they can coexist with a release install.

## CI

The `android` workflow runs on pushes that touch `android/`, `core/`, `.cargo/` or `scripts/`,
and can be started manually with `gh workflow run android.yml -R KingKaonix/aiaio --ref main`.
