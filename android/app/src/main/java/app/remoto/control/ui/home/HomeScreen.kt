package app.remoto.control.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.remoto.control.R
import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.ui.ComingSoonFeature
import app.remoto.control.ui.components.RemotoTopBar
import app.remoto.control.ui.components.deviceDisplayName
import app.remoto.control.ui.theme.RemotoTheme

/** Inicio. Aquí irá un banner en el hito 6 (permitido en esta pantalla, sección 7.5). */
@Composable
fun HomeScreen(
    onOpenRemote: (String) -> Unit,
    onDiscover: () -> Unit,
    onComingSoon: (ComingSoonFeature) -> Unit,
    onDevices: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.demoReady.collect { onOpenRemote(it) } }
    HomeContent(
        state = state,
        onOpenRemote = onOpenRemote,
        onDiscover = onDiscover,
        onComingSoon = onComingSoon,
        onTryDemo = viewModel::startDemo,
        onDevices = onDevices,
        onSettings = onSettings,
        onHelp = onHelp,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onOpenRemote: (String) -> Unit,
    onDiscover: () -> Unit,
    onComingSoon: (ComingSoonFeature) -> Unit,
    onTryDemo: () -> Unit,
    onDevices: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
) {
    Scaffold(
        topBar = {
            RemotoTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onDevices) {
                        Icon(Icons.Filled.Tv, contentDescription = stringResource(R.string.devices_title))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                    IconButton(onClick = onHelp) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.help_title),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.continueWith?.let { device ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.home_last_device), style = MaterialTheme.typography.labelLarge)
                        Text(deviceDisplayName(device), style = MaterialTheme.typography.titleLarge)
                        Button(onClick = { onOpenRemote(device.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.home_open_remote))
                        }
                    }
                }
            }
            Text(stringResource(R.string.home_add_title), style = MaterialTheme.typography.titleMedium)
            StartOptions(onDiscover = onDiscover, onComingSoon = onComingSoon, onTryDemo = onTryDemo)
        }
    }
}

@Preview(name = "Inicio — claro", locale = "es")
@Preview(name = "Home — dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomePreview() {
    RemotoTheme(dynamicColor = false) {
        HomeContent(
            state = HomeUiState(devices = listOf(TvDevice("demo", "", Platform.DEMO)), lastDeviceId = "demo"),
            onOpenRemote = {},
            onDiscover = {},
            onComingSoon = {},
            onTryDemo = {},
            onDevices = {},
            onSettings = {},
            onHelp = {},
        )
    }
}
