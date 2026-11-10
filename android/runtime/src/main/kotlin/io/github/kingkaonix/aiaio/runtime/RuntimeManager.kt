package io.github.kingkaonix.aiaio.runtime

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

/**
 * App-side handle for the Shizuku user service.
 *
 * Lifecycle: Shizuku owns the service process, so [service] can become null at any time when
 * Shizuku dies or the user revokes permission. Callers must handle a null service and retry
 * [bind] after the Shizuku binder is back.
 */
class RuntimeManager(private val context: Context) {

    @Volatile
    var service: IRuntimeService? = null
        private set

    private val userServiceArgs: Shizuku.UserServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, RuntimeUserService::class.java.name),
        )
            .daemon(false)
            .tag("aiaio-runtime")
            .processNameSuffix("runtime")
            .debuggable(BuildConfig.DEBUG)
            // Bump whenever IRuntimeService changes: Shizuku restarts a user service whose
            // version differs from the running one, which keeps binder transactions in sync.
            .version(2)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IRuntimeService.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    fun bind(): Result<Unit> = runCatching {
        check(Shizuku.pingBinder()) { "Shizuku is not running" }
        check(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            "Shizuku permission has not been granted"
        }
        Shizuku.bindUserService(userServiceArgs, connection)
    }

    fun unbind() {
        runCatching { Shizuku.unbindUserService(userServiceArgs, connection, true) }
        service = null
    }

    /** Runs [argv] as shell and returns the service's JSON result. */
    fun exec(argv: List<String>, cwd: String = ""): String =
        call("runtime service is not bound") { it.exec(argv.toTypedArray(), cwd) }

    /** Creates [path] (and parents) inside the runtime root. */
    fun mkdirs(path: String): String =
        call("runtime service is not bound") { it.mkdirs(path) }

    /** Writes [bytes] to [path] with the given Unix [mode] (e.g. 0x1C0 = 0700). */
    fun writeFile(path: String, bytes: ByteArray, mode: Int = 0x1C0): String =
        call("runtime service is not bound") { it.writeFile(path, bytes, mode) }

    private inline fun call(
        unboundMessage: String,
        block: (IRuntimeService) -> String,
    ): String {
        val remote = service ?: return errorJson(unboundMessage)
        return runCatching { block(remote) }
            .getOrElse { errorJson(it.message ?: it.javaClass.name) }
    }

    private fun errorJson(message: String): String =
        org.json.JSONObject()
            .put("code", -1)
            .put("error", message)
            .toString()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 4001

        /** Human-readable Shizuku state, safe to call from the UI thread. */
        fun probe(): String = try {
            when {
                !Shizuku.pingBinder() -> "not running"
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> "ready"
                Shizuku.shouldShowRequestPermissionRationale() -> "permission denied (can ask again)"
                else -> "permission not granted"
            }
        } catch (t: Throwable) {
            "error: ${t.message ?: t.javaClass.name}"
        }

        /**
         * Opens the Shizuku permission dialog. The result is delivered to listeners registered
         * with Shizuku.addRequestPermissionResultListener (see the onboarding screen in Phase 4).
         */
        fun requestPermission(): String = try {
            if (!Shizuku.pingBinder()) {
                "Shizuku is not running"
            } else {
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
                "permission request sent"
            }
        } catch (t: Throwable) {
            "error: ${t.message ?: t.javaClass.name}"
        }
    }
}
