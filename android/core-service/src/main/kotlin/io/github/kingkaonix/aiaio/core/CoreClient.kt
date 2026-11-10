package io.github.kingkaonix.aiaio.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import java.util.concurrent.atomic.AtomicReference

/**
 * App-side client for [CoreService]. Bind once per process/activity scope and call [unbind]
 * when the scope ends. Commands are JSON strings; see `core/crates/runtime-proto`.
 */
class CoreClient(context: Context) {
    private val appContext = context.applicationContext
    private val remote = AtomicReference<ICoreService?>(null)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            remote.set(ICoreService.Stub.asInterface(service))
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remote.set(null)
        }
    }

    /** Returns false when the bind call itself failed (e.g. service not installed). */
    fun bind(): Boolean {
        if (remote.get() != null) return true
        return runCatching {
            appContext.bindService(
                Intent(appContext, CoreService::class.java),
                connection,
                Context.BIND_AUTO_CREATE,
            )
        }.getOrDefault(false)
    }

    fun unbind() {
        runCatching { appContext.unbindService(connection) }
        remote.set(null)
    }

    fun isConnected(): Boolean = remote.get() != null

    fun dispatch(request: String): String {
        val service = remote.get()
            ?: return errorResponse("core_unbound", "core service is not bound")
        return try {
            service.dispatch(request)
        } catch (t: Throwable) {
            errorResponse("core_error", t.message ?: t.javaClass.name)
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
