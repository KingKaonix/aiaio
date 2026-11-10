//! Tauri-free core of aiaio.
//!
//! Phase 0 ships the façade and the port traits only. Phase 1 moves `services/`, `database/`,
//! `proxy/`, `deeplink/`, `session_manager/`, `live/` and `mode/` from `src-tauri` into this
//! crate and replaces Tauri coupling with the ports in [`ports`].
//!
//! The desktop Tauri shell and the Android `:core` process both call [`AppCore::dispatch_json`],
//! so behaviour cannot drift between platforms.

pub mod dispatch;
pub mod ports;

pub use dispatch::AppCore;
pub use ports::{
    EventSink, NoopEventSink, PathProvider, PortError, SecretStore, UnsupportedPathProvider,
};

/// Core crate version, reported by `core.info` and useful in bug reports.
pub const CORE_VERSION: &str = env!("CARGO_PKG_VERSION");
