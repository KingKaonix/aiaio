#!/usr/bin/env bash
# Verify 16 KB page-size compliance:
#   1. APK is zipaligned with 16 KB pages (-P 16)
#   2. every .so packaged from jniLibs has LOAD segments aligned to >= 0x4000
#
# Usage: scripts/check-16kb.sh [path/to/app.apk]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="${1:-}"

if [[ -n "$APK" ]]; then
  if [[ ! -f "$APK" ]]; then
    echo "error: APK not found: $APK" >&2
    exit 1
  fi

  BUILD_TOOLS="${ANDROID_BUILD_TOOLS:-}"
  if [[ -z "$BUILD_TOOLS" ]]; then
    for base in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}"; do
      if [[ -n "$base" && -x "$base/build-tools/36.0.0/zipalign" ]]; then
        BUILD_TOOLS="$base/build-tools/36.0.0"
        break
      fi
    done
  fi
  if [[ -z "$BUILD_TOOLS" ]]; then
    BUILD_TOOLS="$(dirname "$(command -v zipalign || true)")"
  fi
  if [[ ! -x "$BUILD_TOOLS/zipalign" ]]; then
    echo "error: zipalign not found (set ANDROID_BUILD_TOOLS)" >&2
    exit 1
  fi

  echo "==> zipalign -c -P 16"
  "$BUILD_TOOLS/zipalign" -c -P 16 -v 4 "$APK"
fi

shopt -s nullglob
LIBS=("$ROOT"/android/app/src/main/jniLibs/*/libaiaio_core.so)
if [[ ${#LIBS[@]} -eq 0 ]]; then
  echo "error: no libaiaio_core.so under android/app/src/main/jniLibs" >&2
  exit 1
fi

READELF="${ANDROID_READELF:-}"
if [[ -z "$READELF" ]]; then
  NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
  if [[ -z "$NDK" ]]; then
    for base in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}"; do
      if [[ -n "$base" && -d "$base/ndk" ]]; then
        NDK="$(ls -d "$base"/ndk/* 2>/dev/null | tail -n 1)"
        break
      fi
    done
  fi
  if [[ -n "$NDK" ]]; then
    READELF="$(find "$NDK/toolchains/llvm/prebuilt" -name llvm-readelf -type f 2>/dev/null | head -n 1)"
  fi
fi
READELF="${READELF:-readelf}"

status=0
for lib in "${LIBS[@]}"; do
  echo "==> $lib"
  mapfile -t aligns < <("$READELF" -lW "$lib" | awk '$1 == "LOAD" { print $NF }')
  if ! python3 - "$lib" "${aligns[@]}" <<'PY'
import sys

lib = sys.argv[1]
aligns = sys.argv[2:]
if not aligns:
    sys.exit(f"{lib}: no LOAD segments found")
bad = []
for value in aligns:
    try:
        alignment = int(value, 16)
    except ValueError:
        sys.exit(f"{lib}: unparsable alignment {value!r}")
    if alignment < 16384:
        bad.append(value)
if bad:
    sys.exit(f"{lib}: LOAD alignment below 16 KB: {', '.join(bad)}")
print(f"  {len(aligns)} LOAD segment(s), all aligned to >= 16 KB")
PY
  then
    status=1
  fi
done

if [[ $status -ne 0 ]]; then
  echo "16 KB check FAILED" >&2
  exit 1
fi

echo "16 KB check passed"
