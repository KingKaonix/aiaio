//! Wire types shared by the Rust core, the JNI façade and the Android `:core` service.
//!
//! Everything crossing a process boundary is JSON with these envelopes. Keeping the types in
//! one crate means the desktop Tauri shell, the Android service and the tests all agree on the
//! same protocol language.

use serde::{Deserialize, Serialize};
use serde_json::{json, Value};

/// Bumped whenever an existing command changes shape. Additive commands do not bump it.
pub const PROTOCOL_VERSION: u32 = 1;

/// Identifies the request format in logs and diagnostics.
pub const DISPATCH_FORMAT: &str = "aiaio-dispatch";

/// One command sent to the core.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Request {
    /// Opaque caller-supplied id, echoed in the response. Optional so the CLI can omit it.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub id: Option<String>,
    pub command: String,
    #[serde(default)]
    pub args: Value,
}

impl Request {
    pub fn new(command: impl Into<String>) -> Self {
        Self {
            id: None,
            command: command.into(),
            args: Value::Null,
        }
    }

    pub fn with_id(mut self, id: impl Into<String>) -> Self {
        self.id = Some(id.into());
        self
    }

    pub fn with_args(mut self, args: Value) -> Self {
        self.args = args;
        self
    }
}

/// Structured error returned to the caller. `code` is stable API; `message` is for humans.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct ErrorBody {
    pub code: String,
    pub message: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub details: Option<Value>,
}

/// One response from the core. Exactly one of `data` / `error` is present.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Response {
    pub ok: bool,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub id: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub data: Option<Value>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub error: Option<ErrorBody>,
}

impl Response {
    pub fn ok(id: Option<String>, data: Value) -> Self {
        Self {
            ok: true,
            id,
            data: Some(data),
            error: None,
        }
    }

    pub fn err(id: Option<String>, code: impl Into<String>, message: impl Into<String>) -> Self {
        Self {
            ok: false,
            id,
            data: None,
            error: Some(ErrorBody {
                code: code.into(),
                message: message.into(),
                details: None,
            }),
        }
    }
}

/// Sync metadata carried by every synced entity (ADR-0003).
///
/// `deleted_at` is the tombstone timestamp. A row is resurrected when an update is newer than
/// the tombstone, which keeps "delete then edit" intuitive across devices.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct SyncMeta {
    /// Unix milliseconds of the last write.
    pub updated_at: i64,
    /// Device id that produced the write (stable UUID stored in the Keystore-backed prefs).
    pub origin_device: String,
    /// Non-null when the row was tombstoned.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub deleted_at: Option<i64>,
    /// Per-row revision, monotonically increasing per origin device.
    #[serde(default)]
    pub rev: u64,
}

impl SyncMeta {
    pub fn new(updated_at: i64, origin_device: impl Into<String>) -> Self {
        Self {
            updated_at,
            origin_device: origin_device.into(),
            deleted_at: None,
            rev: 1,
        }
    }

    pub fn tombstone(updated_at: i64, origin_device: impl Into<String>) -> Self {
        Self {
            origin_device: origin_device.into(),
            deleted_at: Some(updated_at),
            updated_at,
            rev: 1,
        }
    }

    /// Timestamp used for last-writer-wins comparisons.
    pub fn effective_at(&self) -> i64 {
        self.deleted_at.unwrap_or(self.updated_at)
    }
}

/// One device participating in a sync group.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct DeviceRecord {
    pub device_id: String,
    pub name: String,
    pub platform: String,
    pub last_seen_at: i64,
}

/// Append-only sync operation. Oplogs are uploaded per device as JSONL next to the snapshot.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct OpLogEntry {
    pub device_id: String,
    /// Monotonic per-device sequence number.
    pub seq: u64,
    /// Entity kind, e.g. `provider`, `mcp_server`, `setting`, `skill`.
    pub entity: String,
    /// Stable entity key within `entity`.
    pub key: String,
    pub meta: SyncMeta,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub value: Option<Value>,
}

impl OpLogEntry {
    pub fn ping(device_id: impl Into<String>, seq: u64) -> Self {
        Self {
            device_id: device_id.into(),
            seq,
            entity: "device".to_string(),
            key: "ping".to_string(),
            meta: SyncMeta::new(0, "system"),
            value: Some(json!({"ok": true})),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn request_parses_without_optional_fields() {
        let request: Request = serde_json::from_str(r#"{"command":"ping"}"#).unwrap();
        assert_eq!(request.command, "ping");
        assert!(request.id.is_none());
        assert_eq!(request.args, Value::Null);
    }

    #[test]
    fn request_round_trips_with_id_and_args() {
        let request = Request::new("provider.list")
            .with_id("req-1")
            .with_args(json!({"app": "claude"}));
        let encoded = serde_json::to_string(&request).unwrap();
        let decoded: Request = serde_json::from_str(&encoded).unwrap();
        assert_eq!(request, decoded);
    }

    #[test]
    fn success_response_omits_error_field() {
        let response = Response::ok(Some("1".into()), json!({"pong": true}));
        let encoded = serde_json::to_value(&response).unwrap();
        assert_eq!(encoded["ok"], json!(true));
        assert!(encoded.get("error").is_none());
        assert_eq!(encoded["id"], json!("1"));
    }

    #[test]
    fn error_response_omits_data_field() {
        let response = Response::err(None, "unknown_command", "nope");
        let encoded = serde_json::to_value(&response).unwrap();
        assert_eq!(encoded["ok"], json!(false));
        assert_eq!(encoded["error"]["code"], json!("unknown_command"));
        assert!(encoded.get("data").is_none());
    }

    #[test]
    fn sync_meta_tombstone_uses_deleted_at_for_ordering() {
        let meta = SyncMeta::tombstone(500, "phone");
        assert_eq!(meta.effective_at(), 500);
        assert!(meta.deleted_at.is_some());
    }

    #[test]
    fn oplog_entry_round_trips() {
        let entry = OpLogEntry {
            device_id: "device-a".into(),
            seq: 7,
            entity: "provider".into(),
            key: "claude:default".into(),
            meta: SyncMeta::new(123, "device-a"),
            value: Some(json!({"name": "default"})),
        };
        let encoded = serde_json::to_string(&entry).unwrap();
        let decoded: OpLogEntry = serde_json::from_str(&encoded).unwrap();
        assert_eq!(entry, decoded);
    }
}
