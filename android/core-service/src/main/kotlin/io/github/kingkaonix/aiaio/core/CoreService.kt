package io.github.kingkaonix.aiaio.core

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Hosts the Rust core in the `:core` process. All work is dispatched through JSON so the
 * AIDL surface stays tiny and the desktop/Tauri shell can reuse the same façade.
 */
class CoreService : Service() {
    private val binder = object : ICoreService.Stub() {
        override fun ping(): Boolean = true

        override fun protocolVersion(): Int = CoreBridge.protocolVersion()

        override fun dispatch(request: String): String = CoreBridge.dispatch(request)
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
