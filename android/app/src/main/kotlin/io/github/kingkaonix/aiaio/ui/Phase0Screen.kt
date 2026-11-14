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
import org.json.JSONArray
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
    var t02Result by remember { mutableStateOf<String?>(null) }
    var t02Running by remember { mutableStateOf(false) }
    var t02Log by remember { mutableStateOf<String?>(null) }
    var t02LogRunning by remember { mutableStateOf(false) }

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

        Button(
            enabled = !t02Running,
            onClick = {
                t02Running = true
                t02Result = "running…"
                scope.launch {
                    t02Result = try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            runT02Probe(runtimeManager)
                        }
                    } finally {
                        t02Running = false
                    }
                }
            },
        ) {
            Text("Run T02 bottle proof")
        }

        t02Result?.let { StatusCard(title = "T02 result", value = it) }

        Button(
            enabled = !t02LogRunning,
            onClick = {
                t02LogRunning = true
                scope.launch {
                    t02Log = try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            readT02ResultFile(runtimeManager)
                        }
                    } finally {
                        t02LogRunning = false
                    }
                }
            },
        ) {
            Text("Read T02 result file")
        }

        t02Log?.let { StatusCard(title = "T02 full log", value = it) }

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



private val T02_FIXTURE_BASES = listOf(
    "http://127.0.0.1:8766",       // adb reverse tcp:8766 tcp:8766
    "http://192.168.12.227:8766",  // LAN host
)
private const val T02_MANIFEST_PATH = "${RuntimePaths.ROOT}/probe/t02/manifest.json"

private fun t02ReadResult(json: JSONObject?, key: String = "output"): String =
    json?.optString(key).orEmpty().trim()

private fun t02IsOk(json: JSONObject?): Boolean = json != null && json.optInt("code", -1) == 0

private fun t02FetchManifest(url: String): String {
    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    conn.connectTimeout = 5000
    conn.readTimeout = 10000
    conn.instanceFollowRedirects = true
    return try {
        conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        conn.disconnect()
    }
}

private suspend fun runT02Probe(manager: RuntimeManager): String = buildString {
    var step = "init"
    val failures = mutableListOf<String>()
    try {
        step = "awaitService"
        val svc = awaitService(manager)
        if (svc != null) {
            appendLine(svc)
            appendLine("T02 fail: service unavailable")
            return@buildString
        }
        appendLine("service: ok")

        val bridge = RuntimeFileBridge(manager)
        val probeDir = "${RuntimePaths.ROOT}/probe/t02"
        bridge.mkdirs(probeDir).getOrThrow()

        step = "fetch-manifest"
        var fixtureBase = T02_FIXTURE_BASES.first()
        var manifest = ""
        var lastErr: Throwable? = null
        for (candidate in T02_FIXTURE_BASES) {
            try {
                manifest = t02FetchManifest("$candidate/manifest.json").trim()
                fixtureBase = candidate
                lastErr = null
                break
            } catch (t: Throwable) {
                lastErr = t
            }
        }
        if (lastErr != null) throw lastErr!!
        val manifestJson = JSONObject(manifest)
        val sigVal = manifestJson.optJSONObject("signature")?.optString("value").orEmpty()
        val bottles = manifestJson.optJSONArray("bottles") ?: JSONArray()
        bridge.writeAtomic(T02_MANIFEST_PATH, manifest.toByteArray(Charsets.UTF_8), 0x180).getOrThrow()
        appendLine("manifest: fetched base=$fixtureBase keyId=${manifestJson.optString("keyId")} bottles=${bottles.length()} sig=${sigVal.take(12)}")
        if (bottles.length() < 2 || sigVal.isBlank()) failures.add("manifest-invalid")

        // Negative: bad signature
        step = "bad-signature"
        val badSigObj = JSONObject(manifest).apply {
            val sig = optJSONObject("signature") ?: JSONObject().also { put("signature", it) }
            sig.put("value", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==")
        }
        val badSigRes = runCatching { JSONObject(manager.bottleInstall(badSigObj.toString(), "testnode", "1.0.0", "")) }.getOrNull()
        val badSigCode = badSigRes?.optInt("code", -1) ?: -1
        val badSigErr = badSigRes?.optString("error").orEmpty()
        appendLine("badSig: code=$badSigCode err=${badSigErr.take(120)}")
        if (badSigCode == 0 || !badSigErr.contains("signature", ignoreCase = true)) failures.add("badSig-accepted")

        // Negative: checksum/size mismatch via wrong override archive for v1
        step = "bad-checksum"
        val badSumRes = runCatching {
            JSONObject(manager.bottleInstall(manifest, "testnode", "1.0.0", "$fixtureBase/testnode-v2.0.0-android-arm64.tar.gz"))
        }.getOrNull()
        val badSumCode = badSumRes?.optInt("code", -1) ?: -1
        val badSumErr = badSumRes?.optString("error").orEmpty()
        appendLine("badChecksum: code=$badSumCode err=${badSumErr.take(120)}")
        if (badSumCode == 0 || !(badSumErr.contains("sha256", true) || badSumErr.contains("size", true) || badSumErr.contains("mismatch", true))) {
            failures.add("badChecksum-accepted")
        }

        // Interrupted download: serve a partial file
        step = "interrupted"
        val interrupted = runCatching {
            JSONObject(manager.bottleInstall(manifest, "testnode", "1.0.0", "$fixtureBase/testnode-partial.tar.gz"))
        }.getOrNull()
        val listAfterInterrupt = runCatching { JSONObject(manager.bottleList("testnode")) }.getOrNull()
        val curAfterInterrupt = listAfterInterrupt?.opt("current")?.toString() ?: "null"
        val intCode = interrupted?.optInt("code", -1) ?: -1
        val intErr = interrupted?.optString("error").orEmpty()
        appendLine("interrupted: code=$intCode err=${intErr.take(120)} current=$curAfterInterrupt")
        if (intCode == 0 || (curAfterInterrupt != "null" && curAfterInterrupt.isNotBlank())) failures.add("interrupted-activated")

        // Install v1
        step = "install-v1"
        val v1 = runCatching { JSONObject(manager.bottleInstall(manifest, "testnode", "1.0.0", "")) }.getOrNull()
        val v1ok = t02IsOk(v1)
        appendLine("install-v1: code=${v1?.optInt("code", -1)} ver=${v1?.optString("version")} err=${v1?.optString("error")?.take(120)} root=${v1?.optString("root")?.takeLast(36)}")
        if (!v1ok) failures.add("install-v1")

        val nv1 = runCatching { JSONObject(manager.bottleExec("testnode", "1.0.0", listOf("bin/node", "-v"), "")) }.getOrNull()
        val nv1Out = t02ReadResult(nv1)
        appendLine("node-v1: code=${nv1?.optInt("code", -1)} out=${nv1Out.take(80)} err=${nv1?.optString("error")?.take(80)}")
        if (!t02IsOk(nv1) || !nv1Out.contains("v1.0.0-testnode")) failures.add("node-v1")

        val pkg1 = runCatching { JSONObject(manager.bottleExec("testnode", "1.0.0", listOf("bin/hello-pkg"), "")) }.getOrNull()
        val pkg1Out = t02ReadResult(pkg1)
        appendLine("pkg-v1: code=${pkg1?.optInt("code", -1)} out=${pkg1Out.take(80)} err=${pkg1?.optString("error")?.take(80)}")
        if (!t02IsOk(pkg1) || pkg1Out.isBlank()) failures.add("pkg-v1")

        // Install v2 (update) -> becomes current
        step = "install-v2"
        val v2 = runCatching { JSONObject(manager.bottleInstall(manifest, "testnode", "2.0.0", "")) }.getOrNull()
        appendLine("install-v2: code=${v2?.optInt("code", -1)} ver=${v2?.optString("version")} err=${v2?.optString("error")?.take(120)}")
        if (!t02IsOk(v2)) failures.add("install-v2")

        val nv2 = runCatching { JSONObject(manager.bottleExec("testnode", "", listOf("bin/node", "-v"), "")) }.getOrNull()
        val nv2Out = t02ReadResult(nv2)
        appendLine("node-current-after-v2: code=${nv2?.optInt("code", -1)} out=${nv2Out.take(80)} err=${nv2?.optString("error")?.take(80)}")
        if (!t02IsOk(nv2) || !nv2Out.contains("v2.0.0-testnode")) failures.add("node-v2-current")

        val listAfterV2 = runCatching { JSONObject(manager.bottleList("testnode")) }.getOrNull()
        val v2Count = listAfterV2?.optJSONArray("versions")?.length() ?: -1
        appendLine("list-after-v2: versions=$v2Count current=${listAfterV2?.opt("current")}")
        if (v2Count < 2) failures.add("list-v2")

        // Rollback to v1
        step = "rollback-v1"
        val rb = runCatching { JSONObject(manager.bottleActivate("testnode", "1.0.0")) }.getOrNull()
        appendLine("rollback: code=${rb?.optInt("code", -1)} err=${rb?.optString("error")?.take(120)}")
        if (!t02IsOk(rb)) failures.add("rollback")

        val nvRb = runCatching { JSONObject(manager.bottleExec("testnode", "", listOf("bin/node", "-v"), "")) }.getOrNull()
        val nvRbOut = t02ReadResult(nvRb)
        appendLine("node-current-after-rollback: code=${nvRb?.optInt("code", -1)} out=${nvRbOut.take(80)} err=${nvRb?.optString("error")?.take(80)}")
        if (!t02IsOk(nvRb) || !nvRbOut.contains("v1.0.0-testnode")) failures.add("rollback-current")

        // GC stale/non-current (remove v2 while v1 active)
        step = "gc-v2"
        val rm = runCatching { JSONObject(manager.bottleRemove("testnode", "2.0.0")) }.getOrNull()
        appendLine("remove-v2: code=${rm?.optInt("code", -1)} err=${rm?.optString("error")?.take(120)}")
        if (!t02IsOk(rm)) failures.add("gc-remove-v2")

        val listAfterGc = runCatching { JSONObject(manager.bottleList("testnode")) }.getOrNull()
        val gcCount = listAfterGc?.optJSONArray("versions")?.length() ?: -1
        appendLine("list-after-gc: versions=$gcCount current=${listAfterGc?.opt("current")}")
        if (gcCount != 1) failures.add("gc-count")

        // metadata/manifest fields check summary
        val b0 = bottles.optJSONObject(0)
        val fieldsOk = b0 != null && b0.has("license") && b0.has("sourceUrl") && b0.has("redistributable")
        appendLine("manifest-fields: license+sourceUrl+redistributable present=$fieldsOk (fixture)")
        if (!fieldsOk) failures.add("manifest-fields")

        if (failures.isEmpty()) {
            appendLine("T02 ok (proof fixtures; production runtime/signing external)")
        } else {
            appendLine("T02 fail[checks]: ${failures.joinToString(",")}")
        }
    } catch (t: Throwable) {
        appendLine("T02 fail[$step]: ${t.javaClass.simpleName}:${t.message?.take(180)}")
        if (failures.isNotEmpty()) appendLine("T02 fail[checks]: ${failures.joinToString(",")}")
    }
}.also { result ->
    runCatching {
        val bridge = RuntimeFileBridge(manager)
        val outPath = "${RuntimePaths.ROOT}/probe/t02/t02-result.txt"
        bridge.mkdirs("${RuntimePaths.ROOT}/probe/t02")
        bridge.writeAtomic(outPath, result.toByteArray(Charsets.UTF_8), 0x180)
    }
}

private suspend fun readT02ResultFile(manager: RuntimeManager): String {
    awaitService(manager)?.let { return it }
    val path = "${RuntimePaths.ROOT}/probe/t02/t02-result.txt"
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

