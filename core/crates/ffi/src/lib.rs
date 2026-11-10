//! JNI boundary for the Android `:core` process.
//!
//! Class: `io.github.kingkaonix.aiaio.core.NativeCore`
//! Library: `libaiaio_core.so` (packaged in `jniLibs`, loaded with
//! `System.loadLibrary("aiaio_core")`).
//!
//! The symbol names below are ABI: renaming the Kotlin class/package requires renaming these
//! functions. `proguard-rules.pro` keeps the class intact in release builds.

use std::sync::OnceLock;

use aiaio_core_engine::AppCore;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jstring};
use jni::JNIEnv;

static CORE: OnceLock<AppCore> = OnceLock::new();

fn core() -> &'static AppCore {
    CORE.get_or_init(AppCore::new)
}

fn to_java_string(env: &mut JNIEnv<'_>, value: &str) -> jstring {
    match env.new_string(value) {
        Ok(output) => output.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// `nativeDispatch(requestJson: String): String`
#[no_mangle]
pub extern "system" fn Java_io_github_kingkaonix_aiaio_core_NativeCore_nativeDispatch(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    request: JString<'_>,
) -> jstring {
    let request = match env.get_string(&request) {
        Ok(value) => value.to_string_lossy().into_owned(),
        Err(error) => {
            let payload = serde_json::json!({
                "ok": false,
                "error": {
                    "code": "jni",
                    "message": error.to_string(),
                },
            })
            .to_string();
            return to_java_string(&mut env, &payload);
        }
    };

    to_java_string(&mut env, &core().dispatch_json(&request))
}

/// `nativeProtocolVersion(): Int`
#[no_mangle]
pub extern "system" fn Java_io_github_kingkaonix_aiaio_core_NativeCore_nativeProtocolVersion(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jint {
    aiaio_runtime_proto::PROTOCOL_VERSION as jint
}
