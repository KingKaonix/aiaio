package io.github.kingkaonix.aiaio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.kingkaonix.aiaio.runtime.RuntimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * T01 soak: keep this process (and therefore the bound Shizuku user service) alive while the
 * screen is off, append one heartbeat per minute, then execute the probe again and stop.
 *
 * Each heartbeat is a fresh `IRuntimeService.exec` call, so a successful `after` line proves
 * the service was still usable after 30 minutes — not that some detached script survived.
 */
class SoakService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var manager: RuntimeManager
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        manager = RuntimeManager.shared(applicationContext)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat(Soak.NOTIFICATION_ID, notification("starting…"))
        if (job == null) {
            job = scope.launch { runSoak() }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runSoak() {
        try {
            if (!awaitService()) {
                finish("runtime service did not bind")
                return
            }

            write(Soak.PROBE, Soak.PROBE_BODY)
            write(Soak.SCRIPT, Soak.SCRIPT_BODY)
            exec("start")
            update("tick 0 / ${Soak.MINUTES}")

            for (tick in 1..Soak.MINUTES) {
                delay(60_000)
                if (!execWithRetry("tick", tick.toString())) {
                    finish("lost the runtime service at tick $tick")
                    return
                }
                if (tick % 5 == 0) {
                    update("tick $tick / ${Soak.MINUTES}")
                }
            }

            if (!execWithRetry("final")) {
                finish("probe after the soak failed")
                return
            }
            finish("soak complete")
        } catch (t: Throwable) {
            Log.e(TAG, "soak failed", t)
            finish("soak failed: ${t.message ?: t.javaClass.name}")
        }
    }

    private suspend fun awaitService(): Boolean = withTimeoutOrNull(10_000) {
        while (manager.service == null) {
            manager.bind()
            delay(200)
        }
        true
    } ?: false

    /** Binds again once if the first attempt fails, e.g. Shizuku restarted during the soak. */
    private suspend fun execWithRetry(vararg args: String): Boolean {
        if (exec(*args)) return true
        awaitService()
        return exec(*args)
    }

    private fun write(path: String, body: String): Boolean {
        val result = runCatching {
            manager.writeFile(path, body.toByteArray(Charsets.UTF_8), 0x1C0)
        }.getOrElse { error ->
            JSONObject().put("code", -1).put("error", error.message ?: error.javaClass.name).toString()
        }
        val code = runCatching { JSONObject(result).optInt("code", -1) }.getOrDefault(-1)
        if (code != 0) {
            Log.e(TAG, "write $path failed: $result")
        }
        return code == 0
    }

    /** Runs `soak.sh <args>` through the user service and checks the exit code. */
    private fun exec(vararg args: String): Boolean {
        val argv = listOf(Soak.SCRIPT) + args
        val result = manager.exec(argv, Soak.DIR)
        val json = runCatching { JSONObject(result) }.getOrNull()
        val code = json?.optInt("code", -1) ?: -1
        if (code != 0) {
            Log.w(TAG, "exec ${argv.joinToString(" ")} failed: $result")
        }
        return code == 0
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            Soak.CHANNEL_ID,
            "Runtime soak",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps the 30-minute screen-off runtime check alive"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(text: String): Notification =
        NotificationCompat.Builder(this, Soak.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("aiaio soak")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun update(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(Soak.NOTIFICATION_ID, notification(text))
    }

    private fun finish(text: String) {
        update(text)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundCompat(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Read the declared foreground service type from the manifest so the call always
            // matches what <service foregroundServiceType="..."> says (no hardcoded constant).
            val type = try {
                packageManager.getServiceInfo(
                    ComponentName(this, SoakService::class.java),
                    0,
                ).foregroundServiceType
            } catch (_: Exception) {
                0
            }
            startForeground(id, notification, type)
        } else {
            startForeground(id, notification)
        }
    }

    companion object {
        private const val TAG = "aiaio-soak"

        fun start(context: Context) {
            val intent = Intent(context, SoakService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }
}
