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

    @Volatile
    private var runtimeBridge: io.github.kingkaonix.aiaio.runtime.RuntimeFileBridge? = null

    fun attachRuntime(context: android.content.Context) {
        runtimeBridge = io.github.kingkaonix.aiaio.runtime.RuntimeFileBridge(
            io.github.kingkaonix.aiaio.runtime.RuntimeManager.shared(context)
        )
    }

    fun dispatch(request: String): String {
        if (isFsCommand(request)) {
            runtimeBridge?.let { return dispatchFs(request, it) }
        }
        if (!isLoaded) {
            return errorResponse(
                code = "core_unavailable",
                message = "libaiaio_core.so is not bundled in this build",
            )
        }
        return try {
            NativeCore.nativeDispatch(request)
        } catch (t: Throwable) {
            Log.e(TAG, "dispatch failed", t)
            errorResponse(
                code = "core_panic",
                message = t.message ?: t.javaClass.name,
            )
        }
    }

    private fun isFsCommand(request: String): Boolean = try {
        val cmd = org.json.JSONObject(request).optString("command")
        cmd.startsWith("fs.")
    } catch (_: Throwable) {
        false
    }

    private fun dispatchFs(request: String, bridge: io.github.kingkaonix.aiaio.runtime.RuntimeFileBridge): String {
        val req = try { org.json.JSONObject(request) } catch (t: Throwable) {
            return errorResponse("invalid_request", t.message ?: "invalid json")
        }
        val id = req.opt("id")
        val cmd = req.optString("command")
        val args = req.optJSONObject("args") ?: org.json.JSONObject()
        return try {
            val data = when (cmd) {
                "fs.readFile" -> {
                    val path = args.getString("path")
                    val bytes = bridge.read(path).getOrThrow()
                    val text = bytes.toString(Charsets.UTF_8)
                    org.json.JSONObject().put("path", path).put("text", text)
                }
                "fs.writeFile" -> {
                    val path = args.getString("path")
                    val text = args.getString("text")
                    val mode = if (args.has("mode")) args.getInt("mode") else 0x180
                    val out = bridge.writeAtomic(path, text.toByteArray(Charsets.UTF_8), mode).getOrThrow()
                    org.json.JSONObject().put("path", path).put("bytes", text.toByteArray(Charsets.UTF_8).size).put("mode", mode).put("result", out)
                }
                "fs.stat" -> bridge.stat(args.getString("path")).getOrThrow()
                "fs.listDir" -> bridge.listDir(args.getString("path")).getOrThrow()
                "fs.rename" -> bridge.rename(args.getString("from"), args.getString("to")).getOrThrow()
                "fs.remove" -> bridge.remove(args.getString("path")).getOrThrow()
                "fs.restoreBackup" -> bridge.restoreBackup(args.getString("path")).getOrThrow()
                else -> return envelope(id, false, null, org.json.JSONObject().put("code", "unknown_command").put("message", "unknown command: $cmd"))
            }
            envelope(id, true, data, null)
        } catch (t: Throwable) {
            val msg = t.message ?: t.javaClass.name
            val code = when {
                "escapes" in msg || "path" in msg.lowercase() && "escape" in msg.lowercase() -> "path_escape"
                "not found" in msg.lowercase() || "NoSuchElement" in t.javaClass.name -> "not_found"
                else -> "io"
            }
            envelope(id, false, null, org.json.JSONObject().put("code", code).put("message", msg))
        }
    }

    private fun envelope(id: Any?, ok: Boolean, data: org.json.JSONObject?, error: org.json.JSONObject?): String {
        val o = org.json.JSONObject().put("ok", ok)
        if (id != null && id != org.json.JSONObject.NULL) o.put("id", id)
        if (data != null) o.put("data", data)
        if (error != null) o.put("error", error)
        return o.toString()
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
