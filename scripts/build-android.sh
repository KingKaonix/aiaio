#!/usr/bin/env bash
# Build the Rust core for Android, assemble a debug APK, verify 16 KB alignment.
#
# CI is the source of truth; this is for a workstation with JDK 21, Android SDK 36,
# NDK 27.2, Rust (with Android targets) and cargo-ndk.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NDK_VERSION="${NDK_VERSION:-27.2.12479018}"
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-${SDK_ROOT}/ndk/${NDK_VERSION}}"

cd "$ROOT"

for tool in cargo cargo-ndk gradle; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "error: $tool not found on PATH" >&2
    exit 1
  fi
done

if [[ ! -d "$ANDROID_NDK_HOME" ]]; then
  echo "error: ANDROID_NDK_HOME does not point at an NDK: $ANDROID_NDK_HOME" >&2
  exit 1
fi

echo "==> Building libaiaio_core.so (arm64-v8a, x86_64)"
# cargo-ndk resolves the workspace from the working directory, so run inside core/.
# The 16 KB linker flags come from the repo-root .cargo/config.toml, which Cargo finds
# by walking up from here.
(
  cd core
  cargo ndk -t arm64-v8a -t x86_64 -o ../android/app/src/main/jniLibs \
    build --release -p aiaio-ffi
)

echo "==> Assembling githubDebug APK"
gradle -p android :app:assembleGithubDebug "$@"

APK="android/app/build/outputs/apk/github/debug/app-github-debug.apk"
echo "==> APK: $APK"
"$ROOT/scripts/check-16kb.sh" "$APK"
sha256sum "$APK"
