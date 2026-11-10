//! The single façade used by every shell (desktop Tauri, Android `:core`, tests).

use std::path::Path;
use std::sync::Arc;

use aiaio_runtime_proto::{Request, Response, PROTOCOL_VERSION};
use serde::Deserialize;
use serde_json::{json, Value};

use crate::files::FileBridge;
use crate::ports::{EventSink, NoopEventSink, PathProvider, PortError};

/// Serializable fallback for the (practically impossible) case where a `Response` cannot be
/// serialized. Kept as a constant so the error path cannot itself fail.
const SERIALIZE_ERROR: &str =
    r#"{"ok":false,"error":{"code":"serialize","message":"response serialization failed"}}"#;

pub struct AppCore {
    paths: Option<Arc<dyn PathProvider>>,
    files: Option<Arc<dyn FileBridge>>,
    events: Arc<dyn EventSink>,
}

impl Default for AppCore {
    fn default() -> Self {
        Self::new()
    }
}

impl AppCore {
    /// Core with no platform ports. Useful for `ping`, protocol probes and tests.
    pub fn new() -> Self {
        Self::with_ports(None, Arc::new(NoopEventSink))
    }

    pub fn with_ports(paths: Option<Arc<dyn PathProvider>>, events: Arc<dyn EventSink>) -> Self {
        Self {
            paths,
            files: None,
            events,
        }
    }

    /// Adds the file bridge used by the `fs.*` commands. Without one they return
    /// `unsupported` instead of pretending a write succeeded.
    pub fn with_file_bridge(mut self, files: Arc<dyn FileBridge>) -> Self {
        self.files = Some(files);
        self
    }

    /// Handle one JSON request and return one JSON response. Never panics on malformed input.
    pub fn dispatch_json(&self, request_json: &str) -> String {
        let response = match serde_json::from_str::<Request>(request_json) {
            Ok(request) => self.handle(request),
            Err(error) => Response::err(None, "invalid_request", error.to_string()),
        };

        serde_json::to_string(&response).unwrap_or_else(|_| SERIALIZE_ERROR.to_string())
    }

    fn handle(&self, request: Request) -> Response {
        tracing::debug!(command = %request.command, "dispatch");

        match request.command.as_str() {
            "ping" => Response::ok(
                request.id,
                json!({
                    "pong": true,
                    "protocol": PROTOCOL_VERSION,
                    "coreVersion": crate::CORE_VERSION,
                }),
            ),
            "core.info" => {
                let runtime_root = self
                    .paths
                    .as_ref()
                    .and_then(|paths| paths.runtime_root().ok())
                    .map(|path| path.display().to_string());

                self.events
                    .emit("core.info", &json!({"protocol": PROTOCOL_VERSION}));

                Response::ok(
                    request.id,
                    json!({
                        "protocol": PROTOCOL_VERSION,
                        "coreVersion": crate::CORE_VERSION,
                        "runtimeRoot": runtime_root,
                    }),
                )
            }
            command if command.starts_with("fs.") => match self.dispatch_fs(command, &request.args)
            {
                Ok(data) => Response::ok(request.id, data),
                Err(error) => Response::err(request.id, error.code(), error.to_string()),
            },
            other => Response::err(
                request.id,
                "unknown_command",
                format!("unknown command: {other}"),
            ),
        }
    }

    /// `fs.*` commands share one error shape: a stable code plus a human message.
    fn dispatch_fs(&self, command: &str, args: &Value) -> Result<Value, FsError> {
        let files = self.files.as_ref().ok_or(FsError::NoBridge)?;

        match command {
            "fs.readFile" => {
                let args: PathArgs = parse_args(args)?;
                let bytes = files.read(Path::new(&args.path))?;
                let text = String::from_utf8(bytes)
                    .map_err(|error| FsError::NotUtf8(error.utf8_error().valid_up_to()))?;
                Ok(json!({"path": args.path, "text": text}))
            }
            "fs.writeFile" => {
                let args: WriteArgs = parse_args(args)?;
                let mode = args.mode.unwrap_or(0o600);
                files.write_atomic(Path::new(&args.path), args.text.as_bytes(), mode)?;
                Ok(json!({"path": args.path, "bytes": args.text.len(), "mode": mode}))
            }
            "fs.stat" => {
                let args: PathArgs = parse_args(args)?;
                let stat = files.stat(Path::new(&args.path))?;
                Ok(json!({
                    "path": args.path,
                    "len": stat.len,
                    "mode": stat.mode,
                    "isFile": stat.is_file,
                    "isDir": stat.is_dir,
                }))
            }
            "fs.listDir" => {
                let args: PathArgs = parse_args(args)?;
                let entries = files.list(Path::new(&args.path))?;
                Ok(json!({"path": args.path, "entries": entries}))
            }
            "fs.rename" => {
                let args: RenameArgs = parse_args(args)?;
                files.rename(Path::new(&args.from), Path::new(&args.to))?;
                Ok(json!({"from": args.from, "to": args.to}))
            }
            "fs.remove" => {
                let args: PathArgs = parse_args(args)?;
                files.remove(Path::new(&args.path))?;
                Ok(json!({"path": args.path}))
            }
            "fs.restoreBackup" => {
                let args: PathArgs = parse_args(args)?;
                files.restore_backup(Path::new(&args.path))?;
                Ok(json!({"path": args.path, "restored": true}))
            }
            other => Err(FsError::Unknown(other.to_string())),
        }
    }
}

#[derive(Debug, thiserror::Error)]
enum FsError {
    #[error("no file bridge configured for this shell")]
    NoBridge,
    #[error("invalid arguments: {0}")]
    Args(#[from] serde_json::Error),
    #[error(transparent)]
    Port(#[from] PortError),
    #[error(
        "file is not valid UTF-8 (valid prefix: {0} bytes); binary files need phase-2 encoding"
    )]
    NotUtf8(usize),
    #[error("unknown file command: {0}")]
    Unknown(String),
}

impl FsError {
    fn code(&self) -> &'static str {
        match self {
            FsError::NoBridge => "unsupported",
            FsError::Args(_) => "invalid_args",
            FsError::Port(PortError::PathEscape(_)) => "path_escape",
            FsError::Port(PortError::NotFound(_)) => "not_found",
            FsError::Port(PortError::Verification(_)) => "verification_failed",
            FsError::Port(PortError::Unsupported(_)) => "unsupported",
            FsError::Port(PortError::Secret(_)) => "secret_store",
            FsError::Port(PortError::Io(_)) => "io",
            FsError::NotUtf8(_) => "not_utf8",
            FsError::Unknown(_) => "unknown_command",
        }
    }
}

fn parse_args<T: for<'de> Deserialize<'de>>(args: &Value) -> Result<T, FsError> {
    serde_json::from_value(args.clone()).map_err(FsError::Args)
}

#[derive(Debug, Deserialize)]
struct PathArgs {
    path: String,
}

#[derive(Debug, Deserialize)]
struct WriteArgs {
    path: String,
    text: String,
    #[serde(default)]
    mode: Option<u32>,
}

#[derive(Debug, Deserialize)]
struct RenameArgs {
    from: String,
    to: String,
}

#[cfg(test)]
mod tests {
    use super::*;

    fn check(response_json: &str) -> Response {
        serde_json::from_str(response_json).expect("response must be valid JSON")
    }

    #[test]
    fn ping_returns_protocol_and_version() {
        let response = check(&AppCore::new().dispatch_json(r#"{"command":"ping"}"#));
        assert!(response.ok);
        let data = response.data.expect("data");
        assert_eq!(data["pong"], json!(true));
        assert_eq!(data["protocol"], json!(PROTOCOL_VERSION));
        assert_eq!(data["coreVersion"], json!(crate::CORE_VERSION));
    }

    #[test]
    fn request_id_is_echoed() {
        let response = check(&AppCore::new().dispatch_json(r#"{"id":"abc","command":"ping"}"#));
        assert_eq!(response.id.as_deref(), Some("abc"));
    }

    #[test]
    fn unknown_command_is_reported() {
        let response = check(&AppCore::new().dispatch_json(r#"{"command":"nope"}"#));
        assert!(!response.ok);
        assert_eq!(response.error.expect("error").code, "unknown_command");
    }

    #[test]
    fn malformed_json_does_not_panic() {
        let response = check(&AppCore::new().dispatch_json("{not json"));
        assert!(!response.ok);
        assert_eq!(response.error.expect("error").code, "invalid_request");
    }

    #[test]
    fn core_info_reports_null_runtime_root_without_ports() {
        let response = check(&AppCore::new().dispatch_json(r#"{"command":"core.info"}"#));
        assert!(response.ok);
        let data = response.data.expect("data");
        assert_eq!(data["protocol"], json!(PROTOCOL_VERSION));
        assert!(data["runtimeRoot"].is_null());
    }

    struct TestDir(std::path::PathBuf);

    impl TestDir {
        fn new() -> Self {
            let path = std::env::temp_dir().join(format!(
                "aiaio-dispatch-{}-{}",
                std::process::id(),
                std::time::SystemTime::now()
                    .duration_since(std::time::UNIX_EPOCH)
                    .unwrap()
                    .as_nanos()
            ));
            std::fs::create_dir_all(&path).expect("create root");
            Self(path)
        }
    }

    impl Drop for TestDir {
        fn drop(&mut self) {
            let _ = std::fs::remove_dir_all(&self.0);
        }
    }

    fn core_with_files(dir: &TestDir) -> AppCore {
        let bridge = crate::files::LocalFileBridge::new(&dir.0).expect("bridge");
        AppCore::new().with_file_bridge(Arc::new(bridge))
    }

    #[test]
    fn fs_write_then_read_round_trips() {
        let dir = TestDir::new();
        let core = core_with_files(&dir);

        let write = check(&core.dispatch_json(
            r#"{"id":"w","command":"fs.writeFile","args":{"path":".claude/settings.json","text":"{\"a\":1}"}}"#,
        ));
        assert!(write.ok, "{write:?}");
        assert_eq!(write.data.unwrap()["bytes"], json!(7));

        let read = check(&core.dispatch_json(
            r#"{"id":"r","command":"fs.readFile","args":{"path":".claude/settings.json"}}"#,
        ));
        assert!(read.ok, "{read:?}");
        assert_eq!(read.data.unwrap()["text"], json!(r#"{"a":1}"#));

        let stat = check(
            &core.dispatch_json(r#"{"command":"fs.stat","args":{"path":".claude/settings.json"}}"#),
        );
        assert!(stat.ok);
        assert_eq!(stat.data.unwrap()["mode"], json!(0o600));
    }

    #[test]
    fn fs_commands_fail_loudly_without_a_bridge() {
        let response = check(
            &AppCore::new().dispatch_json(r#"{"command":"fs.readFile","args":{"path":"x"}}"#),
        );
        assert!(!response.ok);
        assert_eq!(response.error.unwrap().code, "unsupported");
    }

    #[test]
    fn fs_rejects_traversal_with_a_stable_code() {
        let dir = TestDir::new();
        let core = core_with_files(&dir);
        let response = check(
            &core.dispatch_json(r#"{"command":"fs.readFile","args":{"path":"../etc/passwd"}}"#),
        );
        assert!(!response.ok);
        assert_eq!(response.error.unwrap().code, "path_escape");
    }

    #[test]
    fn fs_bad_args_are_reported_not_panicked() {
        let dir = TestDir::new();
        let core = core_with_files(&dir);
        let response = check(&core.dispatch_json(r#"{"command":"fs.readFile","args":{}}"#));
        assert!(!response.ok);
        assert_eq!(response.error.unwrap().code, "invalid_args");
    }
}
