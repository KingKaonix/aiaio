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
import io.github.kingkaonix.aiaio.core.CoreClient
import io.github.kingkaonix.aiaio.runtime.RuntimeManager
import io.github.kingkaonix.aiaio.runtime.RuntimePaths
import io.github.kingkaonix.aiaio.Soak
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

        Text(
            text = "Next gate: T01 (execute a program in /data/local/tmp/aiaio as shell UID).",
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
    appendLine("ticks=${lines.count { it.startsWith("tick=") }} / $SOAK_MINUTES")
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
