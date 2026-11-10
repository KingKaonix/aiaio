package io.github.kingkaonix.aiaio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.kingkaonix.aiaio.R

private enum class AppTab(val labelRes: Int, val glyph: String) {
    PROVIDERS(R.string.tab_providers, "P"),
    MCP(R.string.tab_mcp, "M"),
    SKILLS(R.string.tab_skills, "S"),
    SESSIONS(R.string.tab_sessions, "T"),
    MORE(R.string.tab_more, "…"),
}

@Composable
fun AiaioApp() {
    val tabs = AppTab.entries
    // Land on More so the Phase 0 status panel is visible by default.
    var selected by rememberSaveable { mutableStateOf(tabs.lastIndex) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Text(tab.glyph) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (tabs[selected]) {
                AppTab.PROVIDERS -> PlaceholderScreen(
                    title = "Providers",
                    body = "The provider list from the real database lands in Phase 2 (M1).",
                )

                AppTab.MCP -> PlaceholderScreen(
                    title = "MCP servers",
                    body = "Unified MCP list, per-app toggles and connectivity checks land in Phase 6.",
                )

                AppTab.SKILLS -> PlaceholderScreen(
                    title = "Skills",
                    body = "Installed, discover, repos, updates and per-app toggles land in Phase 6.",
                )

                AppTab.SESSIONS -> PlaceholderScreen(
                    title = "Sessions",
                    body = "Per-tool session search and detail land in Phase 6.",
                )

                AppTab.MORE -> Phase0Screen()
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(text = body, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "TODO(phase-2+)",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}
