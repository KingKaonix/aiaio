//! File access for the runtime workspace (T03).
//!
//! Every tool config aiaio edits (`~/.claude/settings.json`, `~/.codex/config.toml`, …) lives
//! under one root. Android reaches it through the Shizuku service, desktop through the normal
//! filesystem, so the rules are stated once here and enforced by every implementation:
//!
//! - paths are **relative** to the root; absolute paths and `..` are rejected;
//! - a symlink that leaves the root is rejected by canonicalising the parent directory;
//! - writes are atomic: temp file → `fsync` → `rename` → verify, with the previous content kept
//!   as a `<name>.aiaio-bak` sidecar for recovery;
//! - writes to the same path are serialised, so concurrent callers never interleave bytes.

use std::collections::HashMap;
use std::fs;
use std::io::Write;
use std::path::{Component, Path, PathBuf};
use std::sync::{Arc, Mutex};

use serde::{Deserialize, Serialize};

use crate::ports::PortError;

/// Suffix of the recovery copy kept next to a file when it is overwritten.
pub const BACKUP_SUFFIX: &str = ".aiaio-bak";

/// Prefix of in-flight temporary files. They are created in the target directory so the final
/// `rename` stays on one filesystem (and therefore atomic).
pub const TEMP_PREFIX: &str = ".aiaio-tmp-";

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct DirEntry {
    pub name: String,
    pub is_dir: bool,
    pub len: u64,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct FileStat {
    pub len: u64,
    pub mode: u32,
    pub is_file: bool,
    pub is_dir: bool,
}

/// Platform file access. Implementations must apply the same path rules.
pub trait FileBridge: Send + Sync {
    /// Reads a whole file.
    fn read(&self, path: &Path) -> Result<Vec<u8>, PortError>;

    /// Writes `bytes` atomically and sets `mode` (`0o600` for secret-bearing files).
    fn write_atomic(&self, path: &Path, bytes: &[u8], mode: u32) -> Result<(), PortError>;

    /// Lists a directory, sorted by name.
    fn list(&self, path: &Path) -> Result<Vec<DirEntry>, PortError>;

    fn stat(&self, path: &Path) -> Result<FileStat, PortError>;

    /// Renames within the root.
    fn rename(&self, from: &Path, to: &Path) -> Result<(), PortError>;

    /// Removes a file (directories only when empty).
    fn remove(&self, path: &Path) -> Result<(), PortError>;

    /// Restores the backup sidecar written by [`FileBridge::write_atomic`].
    fn restore_backup(&self, path: &Path) -> Result<(), PortError>;
}

/// Local filesystem implementation, used on desktop and in tests.
pub struct LocalFileBridge {
    root: PathBuf,
    locks: Mutex<HashMap<PathBuf, Arc<Mutex<()>>>>,
}

impl LocalFileBridge {
    /// `root` must exist; it is canonicalised once so later comparisons are stable.
    pub fn new(root: impl Into<PathBuf>) -> Result<Self, PortError> {
        let root = fs::canonicalize(root.into())?;
        Ok(Self {
            root,
            locks: Mutex::new(HashMap::new()),
        })
    }

    pub fn root(&self) -> &Path {
        &self.root
    }

    /// Maps a caller path onto the root, rejecting anything that could escape it.
    fn resolve(&self, path: &Path) -> Result<PathBuf, PortError> {
        if path.is_absolute() {
            return Err(PortError::PathEscape(path.to_path_buf()));
        }

        let mut relative = PathBuf::new();
        for component in path.components() {
            match component {
                Component::Normal(part) => relative.push(part),
                Component::CurDir => {}
                Component::ParentDir | Component::RootDir | Component::Prefix(_) => {
                    return Err(PortError::PathEscape(path.to_path_buf()));
                }
            }
        }

        if relative.as_os_str().is_empty() {
            // "." is the root itself, useful for listing. Mutating calls reject it below.
            return Ok(self.root.clone());
        }

        let joined = self.root.join(&relative);
        self.assert_inside(&joined, path)?;
        Ok(joined)
    }

    /// Like [`LocalFileBridge::resolve`], but refuses the root itself: creating, renaming or
    /// deleting the root is never a caller's business.
    fn resolve_entry(&self, path: &Path) -> Result<PathBuf, PortError> {
        let resolved = self.resolve(path)?;
        if resolved == self.root {
            return Err(PortError::PathEscape(path.to_path_buf()));
        }
        Ok(resolved)
    }

    /// Canonicalises the deepest existing ancestor and requires it to stay under the root. This
    /// catches symlinked directories even when the final file does not exist yet.
    fn assert_inside(&self, joined: &Path, requested: &Path) -> Result<(), PortError> {
        let mut existing = joined;
        while !existing.exists() {
            existing = existing
                .parent()
                .ok_or_else(|| PortError::PathEscape(requested.to_path_buf()))?;
        }

        let canonical = fs::canonicalize(existing)?;
        if canonical == self.root || canonical.starts_with(&self.root) {
            Ok(())
        } else {
            Err(PortError::PathEscape(requested.to_path_buf()))
        }
    }

    fn lock_for(&self, path: &Path) -> Arc<Mutex<()>> {
        let mut locks = self
            .locks
            .lock()
            .unwrap_or_else(|poison| poison.into_inner());
        locks
            .entry(path.to_path_buf())
            .or_insert_with(|| Arc::new(Mutex::new(())))
            .clone()
    }

    fn backup_path(path: &Path) -> PathBuf {
        let mut name = path
            .file_name()
            .map(|name| name.to_os_string())
            .unwrap_or_default();
        name.push(BACKUP_SUFFIX);
        path.with_file_name(name)
    }

    fn sync_dir(dir: &Path) {
        // Directory fsync makes the rename durable. It is unsupported on some filesystems;
        // there is nothing useful to do about that, so the error is ignored on purpose.
        if let Ok(handle) = fs::File::open(dir) {
            let _ = handle.sync_all();
        }
    }
}

impl FileBridge for LocalFileBridge {
    fn read(&self, path: &Path) -> Result<Vec<u8>, PortError> {
        let resolved = self.resolve(path)?;
        fs::read(&resolved).map_err(|error| match error.kind() {
            std::io::ErrorKind::NotFound => PortError::NotFound(path.to_path_buf()),
            _ => PortError::Io(error),
        })
    }

    fn write_atomic(&self, path: &Path, bytes: &[u8], mode: u32) -> Result<(), PortError> {
        let resolved = self.resolve_entry(path)?;
        let parent = resolved
            .parent()
            .ok_or_else(|| PortError::PathEscape(path.to_path_buf()))?;
        fs::create_dir_all(parent)?;
        // Re-check after creating parents: a concurrently created symlink must not win.
        self.assert_inside(&resolved, path)?;

        let lock = self.lock_for(&resolved);
        let _guard = lock.lock().unwrap_or_else(|poison| poison.into_inner());

        let temp = parent.join(format!("{TEMP_PREFIX}{}", uuid::Uuid::new_v4()));

        let result = (|| -> Result<(), PortError> {
            let mut file = fs::File::create(&temp)?;
            file.write_all(bytes)?;
            file.sync_all()?;
            set_mode(&temp, mode)?;

            if resolved.exists() {
                let backup = Self::backup_path(&resolved);
                let backup_temp = parent.join(format!("{TEMP_PREFIX}{}", uuid::Uuid::new_v4()));
                fs::copy(&resolved, &backup_temp)?;
                fs::rename(&backup_temp, &backup)?;
            }

            fs::rename(&temp, &resolved)?;
            Self::sync_dir(parent);

            let written = fs::read(&resolved)?;
            if written != bytes {
                return Err(PortError::Verification(path.to_path_buf()));
            }
            Ok(())
        })();

        if result.is_err() {
            let _ = fs::remove_file(&temp);
        }
        result
    }

    fn list(&self, path: &Path) -> Result<Vec<DirEntry>, PortError> {
        let resolved = self.resolve(path)?;
        let mut entries = Vec::new();
        for entry in fs::read_dir(&resolved)? {
            let entry = entry?;
            let metadata = entry.metadata()?;
            entries.push(DirEntry {
                name: entry.file_name().to_string_lossy().into_owned(),
                is_dir: metadata.is_dir(),
                len: metadata.len(),
            });
        }
        entries.sort_by(|left, right| left.name.cmp(&right.name));
        Ok(entries)
    }

    fn stat(&self, path: &Path) -> Result<FileStat, PortError> {
        let resolved = self.resolve(path)?;
        let metadata = fs::metadata(&resolved).map_err(|error| match error.kind() {
            std::io::ErrorKind::NotFound => PortError::NotFound(path.to_path_buf()),
            _ => PortError::Io(error),
        })?;
        Ok(FileStat {
            len: metadata.len(),
            mode: mode_of(&metadata),
            is_file: metadata.is_file(),
            is_dir: metadata.is_dir(),
        })
    }

    fn rename(&self, from: &Path, to: &Path) -> Result<(), PortError> {
        let from = self.resolve_entry(from)?;
        let to = self.resolve_entry(to)?;
        if let Some(parent) = to.parent() {
            fs::create_dir_all(parent)?;
        }
        fs::rename(&from, &to)?;
        if let Some(parent) = to.parent() {
            Self::sync_dir(parent);
        }
        Ok(())
    }

    fn remove(&self, path: &Path) -> Result<(), PortError> {
        let resolved = self.resolve_entry(path)?;
        let metadata = fs::metadata(&resolved)?;
        if metadata.is_dir() {
            fs::remove_dir(&resolved)?;
        } else {
            fs::remove_file(&resolved)?;
        }
        Ok(())
    }

    fn restore_backup(&self, path: &Path) -> Result<(), PortError> {
        let resolved = self.resolve_entry(path)?;
        let backup = Self::backup_path(&resolved);
        let bytes = fs::read(&backup).map_err(|error| match error.kind() {
            std::io::ErrorKind::NotFound => PortError::NotFound(path.to_path_buf()),
            _ => PortError::Io(error),
        })?;
        let mode = mode_of(&fs::metadata(&backup)?);
        self.write_atomic(path, &bytes, mode)
    }
}

#[cfg(unix)]
fn set_mode(path: &Path, mode: u32) -> Result<(), PortError> {
    use std::os::unix::fs::PermissionsExt;
    fs::set_permissions(path, fs::Permissions::from_mode(mode))?;
    Ok(())
}

#[cfg(not(unix))]
fn set_mode(_path: &Path, _mode: u32) -> Result<(), PortError> {
    Ok(())
}

#[cfg(unix)]
fn mode_of(metadata: &fs::Metadata) -> u32 {
    use std::os::unix::fs::PermissionsExt;
    metadata.permissions().mode() & 0o777
}

#[cfg(not(unix))]
fn mode_of(_metadata: &fs::Metadata) -> u32 {
    0
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::atomic::{AtomicU64, Ordering};

    static COUNTER: AtomicU64 = AtomicU64::new(0);

    struct TestDir(PathBuf);

    impl TestDir {
        fn new(name: &str) -> Self {
            let unique = COUNTER.fetch_add(1, Ordering::SeqCst);
            let path = std::env::temp_dir().join(format!(
                "aiaio-files-{name}-{}-{unique}",
                std::process::id()
            ));
            let _ = fs::remove_dir_all(&path);
            fs::create_dir_all(&path).expect("create test root");
            Self(path)
        }

        fn join(&self, path: &str) -> PathBuf {
            self.0.join(path)
        }
    }

    impl Drop for TestDir {
        fn drop(&mut self) {
            let _ = fs::remove_dir_all(&self.0);
        }
    }

    fn bridge(dir: &TestDir) -> LocalFileBridge {
        LocalFileBridge::new(&dir.0).expect("bridge")
    }

    #[test]
    fn round_trip_is_byte_identical() {
        let dir = TestDir::new("roundtrip");
        let bridge = bridge(&dir);
        let payload = br#"{"theme":"dark","env":{"A":"1"}}"#;

        bridge
            .write_atomic(Path::new(".claude/settings.json"), payload, 0o600)
            .unwrap();

        assert_eq!(
            bridge.read(Path::new(".claude/settings.json")).unwrap(),
            payload
        );
        assert_eq!(
            bridge
                .stat(Path::new(".claude/settings.json"))
                .unwrap()
                .mode,
            0o600
        );
    }

    #[test]
    fn absolute_and_parent_paths_are_rejected() {
        let dir = TestDir::new("escape");
        let bridge = bridge(&dir);

        for path in [
            Path::new("/etc/passwd"),
            Path::new("../outside"),
            Path::new("a/../../outside"),
        ] {
            assert!(
                matches!(
                    bridge.read(path),
                    Err(PortError::PathEscape(_)) | Err(PortError::Io(_))
                ),
                "{} must be rejected",
                path.display()
            );
        }

        assert!(matches!(
            bridge.write_atomic(Path::new("../x"), b"x", 0o600),
            Err(PortError::PathEscape(_))
        ));
    }

    #[cfg(unix)]
    #[test]
    fn symlink_out_of_root_is_rejected() {
        let dir = TestDir::new("symlink");
        let bridge = bridge(&dir);
        let outside = std::env::temp_dir();
        std::os::unix::fs::symlink(&outside, dir.join("escape")).unwrap();

        assert!(matches!(
            bridge.write_atomic(Path::new("escape/pwned"), b"x", 0o600),
            Err(PortError::PathEscape(_))
        ));
    }

    #[test]
    fn missing_file_reports_not_found() {
        let dir = TestDir::new("missing");
        let bridge = bridge(&dir);
        assert!(matches!(
            bridge.read(Path::new("nope.json")),
            Err(PortError::NotFound(_))
        ));
        assert!(matches!(
            bridge.restore_backup(Path::new("nope.json")),
            Err(PortError::NotFound(_))
        ));
    }

    #[test]
    fn overwrite_keeps_the_previous_content_as_backup() {
        let dir = TestDir::new("backup");
        let bridge = bridge(&dir);
        let path = Path::new(".codex/config.toml");

        bridge.write_atomic(path, b"first", 0o600).unwrap();
        bridge.write_atomic(path, b"second", 0o600).unwrap();

        assert_eq!(bridge.read(path).unwrap(), b"second");
        assert_eq!(
            bridge
                .read(Path::new(".codex/config.toml.aiaio-bak"))
                .unwrap(),
            b"first"
        );

        // A corrupt target is recoverable from the sidecar.
        bridge.write_atomic(path, b"{broken", 0o600).unwrap();
        bridge.restore_backup(path).unwrap();
        assert_eq!(bridge.read(path).unwrap(), b"second");
    }

    #[test]
    fn concurrent_writers_never_interleave() {
        let dir = TestDir::new("concurrent");
        let bridge = Arc::new(bridge(&dir));
        let path = Path::new(".claude/settings.json");
        let payloads: Vec<Vec<u8>> = (0..100u8)
            .map(|index| format!("payload-{index}").repeat(64).into_bytes())
            .collect();

        let mut handles = Vec::new();
        for payload in payloads.clone() {
            let bridge = Arc::clone(&bridge);
            handles.push(std::thread::spawn(move || {
                bridge.write_atomic(path, &payload, 0o600).unwrap();
            }));
        }
        for handle in handles {
            handle.join().unwrap();
        }

        let written = bridge.read(path).unwrap();
        assert!(
            payloads.iter().any(|payload| payload == &written),
            "final content must be exactly one of the payloads"
        );
    }

    #[test]
    fn list_is_sorted_and_reports_directories() {
        let dir = TestDir::new("list");
        let bridge = bridge(&dir);
        bridge
            .write_atomic(Path::new("b.txt"), b"b", 0o600)
            .unwrap();
        bridge
            .write_atomic(Path::new("a.txt"), b"a", 0o600)
            .unwrap();
        bridge
            .write_atomic(Path::new("sub/c.txt"), b"c", 0o600)
            .unwrap();

        let entries = bridge.list(Path::new(".")).unwrap();
        let names: Vec<_> = entries.iter().map(|entry| entry.name.as_str()).collect();
        assert_eq!(names, vec!["a.txt", "b.txt", "sub"]);
        assert!(entries[2].is_dir);
    }

    #[test]
    fn rename_and_remove_stay_inside_the_root() {
        let dir = TestDir::new("rename");
        let bridge = bridge(&dir);
        bridge
            .write_atomic(Path::new("one.json"), b"x", 0o600)
            .unwrap();
        bridge
            .rename(Path::new("one.json"), Path::new("two.json"))
            .unwrap();

        assert!(bridge.read(Path::new("one.json")).is_err());
        assert_eq!(bridge.read(Path::new("two.json")).unwrap(), b"x");

        bridge.remove(Path::new("two.json")).unwrap();
        assert!(bridge.read(Path::new("two.json")).is_err());
        assert!(matches!(
            bridge.rename(Path::new("two.json"), Path::new("../escape")),
            Err(PortError::PathEscape(_))
        ));
    }
}
