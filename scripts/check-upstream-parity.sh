#!/usr/bin/env bash
# Guard against silent upstream drift. Run weekly and on PRs that touch core/ or sync.
#
# If a check here fails, read the upstream diff before adapting our fork: protocol constants
# and the sync snapshot format are compatibility boundaries (ADR-0003).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

status=0

require_file() {
  if [[ ! -f "$1" ]]; then
    echo "MISSING file: $1" >&2
    status=1
  fi
}

require_grep() {
  local description="$1" pattern="$2" file="$3"
  if ! grep -Eq "$pattern" "$file"; then
    echo "MISSING $description: /$pattern/ in $file" >&2
    status=1
  fi
}

echo "==> Upstream sync compatibility surface"
require_file "src-tauri/src/services/sync_protocol.rs"
if [[ -f src-tauri/src/services/sync_protocol.rs ]]; then
  require_grep "webdav sync protocol format" 'PROTOCOL_FORMAT.*cc-switch-webdav-sync' \
    src-tauri/src/services/sync_protocol.rs
  require_grep "sync protocol version" 'PROTOCOL_VERSION: u32 = 2' \
    src-tauri/src/services/sync_protocol.rs
  require_grep "db compat version" 'DB_COMPAT_VERSION: u32 = 6' \
    src-tauri/src/services/sync_protocol.rs
  require_grep "remote snapshot db" 'REMOTE_DB_SQL' src-tauri/src/services/sync_protocol.rs
  require_grep "remote skills zip" 'REMOTE_SKILLS_ZIP' src-tauri/src/services/sync_protocol.rs
fi

echo "==> Core dispatch/protocol types"
require_file "core/crates/runtime-proto/src/lib.rs"
if [[ -f core/crates/runtime-proto/src/lib.rs ]]; then
  require_grep "protocol version constant" 'PROTOCOL_VERSION: u32' \
    core/crates/runtime-proto/src/lib.rs
  require_grep "sync metadata" 'struct SyncMeta' core/crates/runtime-proto/src/lib.rs
fi

echo "==> GitHub Actions workflows"
require_file ".github/workflows/android.yml"

echo "==> Commit"
git rev-parse --short HEAD 2>/dev/null || echo "(not a git repository)"

echo "==> Rust tests"
cargo test --manifest-path core/Cargo.toml --workspace

if [[ $status -ne 0 ]]; then
  echo "upstream parity check FAILED" >&2
  exit 1
fi

echo "upstream parity check passed"
