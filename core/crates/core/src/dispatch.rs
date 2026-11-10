//! The single façade used by every shell (desktop Tauri, Android `:core`, tests).

use std::sync::Arc;

use aiaio_runtime_proto::{Request, Response, PROTOCOL_VERSION};
use serde_json::json;

use crate::ports::{EventSink, NoopEventSink, PathProvider};

/// Serializable fallback for the (practically impossible) case where a `Response` cannot be
/// serialized. Kept as a constant so the error path cannot itself fail.
const SERIALIZE_ERROR: &str =
    r#"{"ok":false,"error":{"code":"serialize","message":"response serialization failed"}}"#;

pub struct AppCore {
    paths: Option<Arc<dyn PathProvider>>,
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
        Self { paths, events }
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
            other => Response::err(
                request.id,
                "unknown_command",
                format!("unknown command: {other}"),
            ),
        }
    }
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
}
