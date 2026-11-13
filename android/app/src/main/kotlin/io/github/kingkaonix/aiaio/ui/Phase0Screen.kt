package io.github.kingkaonix.aiaio.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.kingkaonix.aiaio.BuildConfig
import io.github.kingkaonix.aiaio.R
import io.github.kingkaonix.aiaio.SoakService
import io.github.kingkaonix.aiaio.core.CoreClient
import io.github.kingkaonix.aiaio.runtime.RuntimeFileBridge
import io.github.kingkaonix.aiaio.runtime.RuntimeManager
import io.github.kingkaonix.aiaio.runtime.RuntimePaths
import io.github.kingkaonix.aiaio.Soak
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import rikka.shizuku.Shizuku

/**
 * Phase 0 status panel: proves the two-process wiring (Compose -> :core over AIDL), shows
 * whether Shizuku is available, and runs the T01 exec proof on demand.
 *
 * Real feature screens replace this in later phases.
 */
@Composable
fun Phase0Screen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()

    val coreClient = remember { CoreClient(appContext) }
    val runtimeManager = RuntimeManager.shared(appContext)
    var coreState by remember { mutableStateOf("checking…") }
    var shizukuState by remember { mutableStateOf("checking…") }
    var permissionResult by remember { mutableStateOf<String?>(null) }
    var probeResult by remember { mutableStateOf<String?>(null) }
    var probeRunning by remember { mutableStateOf(false) }
    var soakResult by remember { mutableStateOf<String?>(null) }
    var soakRunning by remember { mutableStateOf(false) }
    var t03Result by remember { mutableStateOf<String?>(null) }
    var t03Running by remember { mutableStateOf(false) }
    var t03Log by remember { mutableStateOf<String?>(null) }
    var t03LogRunning by remember { mutableStateOf(false) }

    DisposableEffect(coreClient) {
        coreClient.bind()
        onDispose { coreClient.unbind() }
    }

    DisposableEffect(runtimeManager) {
        onDispose { runtimeManager.unbind() }
    }

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            permissionResult = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                "granted"
            } else {
                "denied"
            }
            shizukuState = RuntimeManager.probe()
        }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    LaunchedEffect(coreClient) {
        val connected = withTimeoutOrNull(3_000) {
            while (!coreClient.isConnected()) {
                delay(50)
            }
            true
        } ?: false

        coreState = if (connected) {
            coreClient.dispatch("""{"id":"phase0","command":"ping"}""")
        } else {
            "core service not bound"
        }
        shizukuState = RuntimeManager.probe()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.phase0_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.phase0_body),
            style = MaterialTheme.typography.bodyMedium,
        )

        StatusCard(
            title = "Build",
            value = "flavor=${BuildConfig.TOOL_CATALOG}, version=${BuildConfig.VERSION_NAME}",
        )
        StatusCard(
            title = stringResource(R.string.core_state),
            value = coreState,
        )
        StatusCard(
            title = stringResource(R.string.shizuku_state),
            value = shizukuState,
        )

        permissionResult?.let { StatusCard(title = "Permission result", value = it) }

        Button(
            onClick = {
                shizukuState = RuntimeManager.probe()
            },
        ) {
            Text(stringResource(R.string.refresh_status))
        }

        Button(
            onClick = {
                permissionResult = RuntimeManager.requestPermission()
            },
        ) {
            Text(stringResource(R.string.request_shizuku_permission))
        }

        Button(
            enabled = !probeRunning,
            onClick = {
                probeRunning = true
                probeResult = "running…"
                scope.launch {
                    probeResult = try {
                        runT01Probe(runtimeManager)
                    } finally {
                        probeRunning = false
                        // The grant may have happened in Shizuku's own dialog, which does not
                        // always reach the request-result listener; the probe is the truth.
                        shizukuState = RuntimeManager.probe()
                    }
                }
            },
        ) {
            Text(stringResource(R.string.t01_run))
        }

        probeResult?.let { StatusCard(title = stringResource(R.string.t01_result), value = it) }

        Button(
            enabled = !soakRunning,
            onClick = {
                soakRunning = true
                scope.launch {
                    soakResult = try {
                        SoakService.start(appContext)
                        "soak running — 30 minutes, screen off"
                    } finally {
                        soakRunning = false
                    }
                }
            },
        ) {
            Text(stringResource(R.string.soak_start))
        }

        Button(
            enabled = !soakRunning,
            onClick = {
                soakRunning = true
                scope.launch {
                    soakResult = try {
                        readSoakLog(runtimeManager)
                    } finally {
                        soakRunning = false
                    }
                }
            },
        ) {
            Text(stringResource(R.string.soak_read))
        }

        soakResult?.let { StatusCard(title = stringResource(R.string.soak_result), value = it) }

        Button(
            enabled = !t03Running,
            onClick = {
                t03Running = true
                t03Result = "running…"
                scope.launch {
                    t03Result = try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            runT03Probe(runtimeManager)
                        }
                    } finally {
                        t03Running = false
                    }
                }
            },
        ) {
            Text("Run T03 file bridge probe")
        }

        t03Result?.let { StatusCard(title = "T03 result", value = it) }

        Button(
            enabled = !t03LogRunning,
            onClick = {
                t03LogRunning = true
                scope.launch {
                    t03Log = try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            readT03ResultFile(runtimeManager)
                        }
                    } finally {
                        t03LogRunning = false
                    }
                }
            },
        ) {
            Text("Read T03 result file")
        }

        t03Log?.let { StatusCard(title = "T03 full log", value = it) }

        Text(
            text = "Next gate: T01/T03 evidence on device (CI-built APK).",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "See docs/agents/tasks/T01-shizuku-exec.md",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private val PROBE_DIR = "${RuntimePaths.ROOT}/probe"
private val SOAK_LOG = Soak.LOG

/** Binds the user service and waits for it; returns a message when it is not usable. */
private suspend fun awaitService(manager: RuntimeManager): String? {
    val bind = manager.bind()
    if (bind.isFailure) {
        return "bind failed: ${bind.exceptionOrNull()?.message ?: "unknown"}"
    }

    val connected = withTimeoutOrNull(5_000) {
        while (manager.service == null) {
            delay(100)
        }
        true
    } ?: false
    return if (connected) null else "user service did not connect (Shizuku running and permission granted?)"
}

/**
 * T01: write a program into /data/local/tmp/aiaio as shell, chmod 0700, execute it, and
 * report each step. Runs entirely over the IRuntimeService AIDL boundary.
 */
private suspend fun runT01Probe(manager: RuntimeManager): String = buildString {
    awaitService(manager)?.let {
        appendLine(it)
        return@buildString
    }

    appendLine("uid: " + describe(manager.exec(listOf("id", "-u"))))
    appendLine("mkdirs: " + describe(manager.mkdirs(Soak.DIR)))
    appendLine(
        "write: " + describe(
            manager.writeFile(Soak.PROBE, Soak.PROBE_BODY.toByteArray(Charsets.UTF_8), 0x1C0),
        ),
    )
    appendLine("exec: " + describe(manager.exec(listOf(Soak.PROBE), Soak.DIR)))
}

/**
 * T01 soak is driven by [SoakService] (a foreground service) so the heartbeat keeps running
 * with the screen off; the UI only starts the service and reads [Soak.LOG] on demand.
 */
private suspend fun readSoakLog(manager: RuntimeManager): String = buildString {
    awaitService(manager)?.let {
        appendLine(it)
        return@buildString
    }

    val result = manager.exec(listOf("/system/bin/cat", SOAK_LOG), PROBE_DIR)
    val output = runCatching { JSONObject(result).optString("output") }.getOrNull()
    if (output.isNullOrBlank()) {
        appendLine("no log yet: " + describe(result))
        return@buildString
    }

    val lines = output.lines().filter { it.isNotBlank() }
    appendLine("ticks=${lines.count { it.startsWith("tick=") }} / ${Soak.MINUTES}")
    appendLine("first: ${lines.first()}")
    appendLine("last: ${lines.last()}")
    if (lines.any { it.startsWith("done ") }) {
        appendLine("soak complete")
    }
}

/** Turns the AIDL JSON envelope into one readable line. */
private fun describe(result: String): String = runCatching {
    val json = JSONObject(result)
    val code = json.optInt("code", Int.MIN_VALUE)
    val output = json.optString("output").trim()
    val error = json.optString("error")
    when {
        error.isNotEmpty() -> "code=$code error=$error"
        output.isNotEmpty() -> "code=$code output=$output"
        else -> "code=$code"
    }
}.getOrElse { result }

@Composable
private fun StatusCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}


private suspend fun runT03Probe(manager: RuntimeManager): String = buildString {
    var step = "init"
    try {
        step = "awaitService"
        appendLine(awaitService(manager) ?: "service: ok")
        val bridge = RuntimeFileBridge(manager)
        val base = "${RuntimePaths.ROOT}/probe/t03"
        val dir = "$base/.claude"
        step = "mkdirs"
        appendLine("mkdirs: ${bridge.mkdirs(dir).getOrThrow().optInt("code")}")

        val fixtures = linkedMapOf(
            "settings.json" to "{\n  \"schema\": 1,\n  \"name\": \"aiaio\",\n  \"nested\": {\"ok\": true}\n}\n",
            "settings.jsonc" to "{\n  // comment\n  \"enabled\": true,\n}\n",
            "config.toml" to "[core]\nname = \"aiaio\"\nenabled = true\n",
            "config.yaml" to "core:\n  name: aiaio\n  enabled: true\n",
        )
        fixtures.forEach { (name, text) ->
            step = "fixture:$name"
            val path = "$dir/$name"
            val bytes = text.toByteArray(Charsets.UTF_8)
            val w = bridge.writeAtomic(path, bytes, 0x180).getOrThrow()
            val read = bridge.read(path).getOrThrow().toString(Charsets.UTF_8)
            val st = bridge.stat(path).getOrThrow()
            appendLine("fixture[$name]: rt=${if (read == text) "ok" else "MISMATCH"} len=${st.optLong("len")} mode=${st.optInt("mode")}")
        }

        step = "listDir"
        val listed = bridge.listDir(dir).getOrThrow()
        appendLine("listDir: entries=${listed.optJSONArray("entries")?.length() ?: -1}")

        step = "rename"
        val settings = "$dir/settings.json"
        val renamed = "$dir/settings.renamed.json"
        bridge.rename(settings, renamed).getOrThrow()
        val stRenamed = bridge.stat(renamed).getOrThrow()
        bridge.rename(renamed, settings).getOrThrow()
        val stBack = bridge.stat(settings).getOrThrow()
        appendLine("rename: ok moved=${stRenamed.optBoolean("exists")} back=${stBack.optBoolean("exists")}")

        step = "restoreBackup"
        val original = fixtures["settings.json"] ?: error("missing fixture")
        val updated = original.replace("\"schema\": 1", "\"schema\": 2")
        bridge.writeAtomic(settings, updated.toByteArray(Charsets.UTF_8), 0x180).getOrThrow()
        bridge.remove(settings).getOrThrow()
        val restored = bridge.restoreBackup(settings).getOrThrow()
        val restoredText = bridge.read(settings).getOrThrow().toString(Charsets.UTF_8)
        appendLine("restore: ${restored.optBoolean("restored")} rt=${if (restoredText == original) "ok" else "MISMATCH"}")

        step = "escape"
        val escaped = manager.readFile("${RuntimePaths.ROOT}/../etc/hosts")
        val escJson = runCatching { JSONObject(escaped) }.getOrNull()
        val escCode = escJson?.optInt("code", -999)
        val escErr = escJson?.optString("error")?.take(60)
        appendLine("escape: code=$escCode ok=${escCode != 0} err=$escErr")

        step = "concurrent"
        val concurrentPath = "$dir/concurrent.json"
        val workers = 100
        val pool = Executors.newFixedThreadPool(12)
        val start = CountDownLatch(1)
        val done = CountDownLatch(workers)
        val payloads = (0 until workers).map { i -> "{\"i\":$i,\"pad\":\"${"x".repeat(48)}\"}\n".toByteArray(Charsets.UTF_8) }
        val failures = java.util.concurrent.ConcurrentLinkedQueue<String>()
        for (i in 0 until workers) {
            val p = payloads[i]
            pool.execute {
                try {
                    start.await(5, TimeUnit.SECONDS)
                    bridge.writeAtomic(concurrentPath, p, 0x180).getOrThrow()
                } catch (t: Throwable) {
                    failures.add("$i:${t.javaClass.simpleName}")
                } finally {
                    done.countDown()
                }
            }
        }
        start.countDown()
        done.await(45, TimeUnit.SECONDS)
        pool.shutdown()
        val finalBytes = bridge.read(concurrentPath).getOrNull()
        val matched = if (finalBytes == null) -1 else payloads.indexOfFirst { it.contentEquals(finalBytes) }
        appendLine("concurrent: w=$workers fail=${failures.size} match=$matched complete=${matched >= 0} len=${finalBytes?.size ?: -1}")
        if (failures.isNotEmpty()) appendLine("cFail: ${failures.take(2).joinToString("|")}")

        appendLine("T03 ok (atomic rename+fsync+verify; kill-test manual)")
    } catch (t: Throwable) {
        appendLine("T03 fail[$step]: ${t.javaClass.simpleName}:${t.message?.take(120)}")
    }
}.also { result ->
    runCatching {
        val bridge = RuntimeFileBridge(manager)
        val outPath = "${RuntimePaths.ROOT}/probe/t03/t03-result.txt"
        bridge.mkdirs("${RuntimePaths.ROOT}/probe/t03")
        bridge.writeAtomic(outPath, result.toByteArray(Charsets.UTF_8), 0x180)
    }
}

private suspend fun readT03ResultFile(manager: RuntimeManager): String {
    awaitService(manager)?.let { return it }
    val path = "${RuntimePaths.ROOT}/probe/t03/t03-result.txt"
    val result = manager.readFile(path)
    return runCatching {
        val json = JSONObject(result)
        val code = json.optInt("code", Int.MIN_VALUE)
        if (code != 0) {
            "read failed: $result"
        } else {
            val b64 = json.optString("data")
            String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT), Charsets.UTF_8)
        }
    }.getOrElse { "read failed: $result" }
}
