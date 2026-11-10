//! Ports that replace the Tauri coupling found in upstream `src-tauri`.
//!
//! Android cannot use `dirs::home_dir`, spawn processes from the app UID for runtime tools, or
//! touch the desktop keychain. Each of those becomes a trait implemented by the platform:
//! - desktop: real home dir, `std::process`, OS keychain
//! - Android: `/data/local/tmp/aiaio` via the Shizuku service, `ProcessBuilder` as shell,
//!   Android Keystore

use std::path::{Path, PathBuf};

use serde_json::Value;

#[derive(Debug, thiserror::Error)]
pub enum PortError {
    #[error("{0} is not supported by this implementation")]
    Unsupported(&'static str),
    #[error("io error: {0}")]
    Io(#[from] std::io::Error),
    #[error("path escapes the runtime root: {0}")]
    PathEscape(PathBuf),
    #[error("no such file: {0}")]
    NotFound(PathBuf),
    #[error("read-back did not match what was written: {0}")]
    Verification(PathBuf),
    #[error("secret store error: {0}")]
    Secret(String),
}

/// Resolves the runtime directories and enforces the path allow-list.
pub trait PathProvider: Send + Sync {
    /// Root of the self-contained runtime, e.g. `/data/local/tmp/aiaio`.
    fn runtime_root(&self) -> Result<PathBuf, PortError>;

    /// Directory holding per-tool config (`~/.claude`, `~/.codex`, …).
    fn home_dir(&self) -> Result<PathBuf, PortError>;

    /// Canonicalises `path` and rejects anything outside [`PathProvider::runtime_root`].
    fn ensure_within_root(&self, path: &Path) -> Result<PathBuf, PortError>;
}

/// Receives status/progress/log events. On desktop this forwards to the Tauri emitter; on
/// Android it writes to the AIDL callback; in tests it is a no-op.
pub trait EventSink: Send + Sync {
    fn emit(&self, event: &str, payload: &Value);
}

/// Keychain/Keystore abstraction. Values are opaque bytes; encryption policy lives above.
pub trait SecretStore: Send + Sync {
    fn get(&self, key: &str) -> Result<Option<Vec<u8>>, PortError>;
    fn put(&self, key: &str, value: &[u8]) -> Result<(), PortError>;
    fn delete(&self, key: &str) -> Result<(), PortError>;
}

#[derive(Debug, Default)]
pub struct NoopEventSink;

impl EventSink for NoopEventSink {
    fn emit(&self, _event: &str, _payload: &Value) {}
}

/// Used on Android until the Shizuku bridge is wired in (T03). Every path operation fails
/// loudly instead of silently writing to the wrong place.
#[derive(Debug, Default)]
pub struct UnsupportedPathProvider;

impl PathProvider for UnsupportedPathProvider {
    fn runtime_root(&self) -> Result<PathBuf, PortError> {
        Err(PortError::Unsupported("runtime root"))
    }

    fn home_dir(&self) -> Result<PathBuf, PortError> {
        Err(PortError::Unsupported("runtime home"))
    }

    fn ensure_within_root(&self, path: &Path) -> Result<PathBuf, PortError> {
        Err(PortError::PathEscape(path.to_path_buf()))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn unsupported_provider_fails_loudly() {
        let provider = UnsupportedPathProvider;
        assert!(provider.runtime_root().is_err());
        assert!(provider.home_dir().is_err());
        assert!(provider.ensure_within_root(Path::new("/tmp/x")).is_err());
    }
}
