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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

/**
 * Phase 0 status panel: proves the two-process wiring (Compose -> :core over AIDL) and shows
 * whether Shizuku is available. Real feature screens replace this in later phases.
 */
@Composable
fun Phase0Screen() {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val coreClient = remember { CoreClient(appContext) }
    var coreState by remember { mutableStateOf("checking…") }
    var shizukuState by remember { mutableStateOf("checking…") }
    var permissionResult by remember { mutableStateOf<String?>(null) }

    DisposableEffect(coreClient) {
        coreClient.bind()
        onDispose { coreClient.unbind() }
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
                permissionResult = RuntimeManager.requestPermission()
            },
        ) {
            Text(stringResource(R.string.request_shizuku_permission))
        }

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
