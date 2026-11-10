package io.github.kingkaonix.aiaio.core

import android.util.Log

/**
 * Kotlin side of the JNI bridge. The symbol names are part of the ABI contract with
 * `core/crates/ffi`; renaming the class or moving the package breaks the app silently at
 * runtime, which is why proguard-rules.pro keeps this class intact.
 */
class NativeCore private constructor() {
    companion object {
        @JvmStatic
        external fun nativeDispatch(request: String): String

        @JvmStatic
        external fun nativeProtocolVersion(): Int
    }
}

/** Loads the core library once and degrades gracefully when it is not bundled. */
object CoreBridge {
    private const val TAG = "aiaio-core"

    private val loadError: Throwable? = try {
        System.loadLibrary("aiaio_core")
        null
    } catch (t: Throwable) {
        Log.e(TAG, "libaiaio_core.so could not be loaded", t)
        t
    }

    val isLoaded: Boolean
        get() = loadError == null

    fun protocolVersion(): Int = if (isLoaded) {
        runCatching { NativeCore.nativeProtocolVersion() }.getOrDefault(0)
    } else {
        0
    }

    fun dispatch(request: String): String = if (!isLoaded) {
        errorResponse(
            code = "core_unavailable",
            message = "libaiaio_core.so is not bundled in this build",
        )
    } else {
        try {
            NativeCore.nativeDispatch(request)
        } catch (t: Throwable) {
            Log.e(TAG, "dispatch failed", t)
            errorResponse(
                code = "core_panic",
                message = t.message ?: t.javaClass.name,
            )
        }
    }

    private fun errorResponse(code: String, message: String): String =
        org.json.JSONObject()
            .put("ok", false)
            .put(
                "error",
                org.json.JSONObject()
                    .put("code", code)
                    .put("message", message),
            )
            .toString()
}
