package app.remoto.control.ui.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.remoto.control.R
import app.remoto.control.core.driver.Capability
import app.remoto.control.core.driver.ConnectionState
import app.remoto.control.core.driver.HelpTopic
import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.SimulatedFailure
import app.remoto.control.core.driver.TvApp
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.core.driver.TvPreviewState
import app.remoto.control.ui.components.RemotoTopBar
import app.remoto.control.ui.components.deviceDisplayName
import app.remoto.control.ui.components.platformNameRes
import app.remoto.control.ui.components.rememberHaptics
import app.remoto.control.ui.help.ConnectionErrorContent
import app.remoto.control.ui.pairing.PairingContent
import app.remoto.control.ui.theme.RemotoTheme

/**
 * Pantalla del control. Solo usa tipos de `core/driver` y su ViewModel: no sabe qué
 * marca hay detrás (criterio de aceptación del hito 1). Aquí nunca habrá anuncios.
 */
@Composable
fun RemoteScreen(
    onBack: () -> Unit,
    onHelp: (HelpTopic?) -> Unit,
    viewModel: RemoteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    RemoteContent(
        state = state,
        apps = apps,
        onBack = onBack,
        onHelp = onHelp,
        actions = RemoteActions(
            onKey = viewModel::onKey,
            onPower = viewModel::onPower,
            onKeyboard = {},
            onToggleTouchpad = {},
            onApps = viewModel::loadApps,
            onPointerMove = viewModel::onPointerMove,
            onPointerTap = viewModel::onPointerTap,
        ),
        onText = viewModel::onText,
        onLaunchApp = viewModel::onLaunchApp,
        onPair = viewModel::onPair,
        onRetry = viewModel::onRetry,
        onSimulateFailure = viewModel::onSimulateFailure,
    )
}

@Composable
private fun RemoteContent(
    state: RemoteUiState,
    apps: List<TvApp>,
    onBack: () -> Unit,
    onHelp: (HelpTopic?) -> Unit,
    actions: RemoteActions,
    onText: (String) -> Unit,
    onLaunchApp: (String) -> Unit,
    onPair: (String?) -> Unit,
    onRetry: () -> Unit,
    onSimulateFailure: (SimulatedFailure) -> Unit,
) {
    var touchpadMode by rememberSaveable { mutableStateOf(false) }
    var showKeyboard by rememberSaveable { mutableStateOf(false) }
    var showApps by rememberSaveable { mutableStateOf(false) }
    val haptic = rememberHaptics(state.hapticsEnabled)

    val shownDevice = state.device ?: state.unsupportedDevice
    val title = shownDevice?.let { deviceDisplayName(it) } ?: stringResource(R.string.app_name)
    Scaffold(
        topBar = {
            RemotoTopBar(
                title = title,
                onBack = onBack,
                actions = { if (state.isSimulated) SimulateErrorMenu(onSimulateFailure) },
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                state.unsupportedDevice != null -> PlatformComingSoon(state.unsupportedDevice, title, onBack)
                state.deviceMissing -> DeviceMissing(onBack)
                state.loading -> Loading(null)
                else -> when (val connection = state.connection) {
                    ConnectionState.Disconnected, ConnectionState.Connecting -> Loading(title)
                    is ConnectionState.PairingRequired -> {
                        state.preview?.let { TvPreview(it) }
                        PairingContent(type = connection.type, onSubmit = onPair)
                    }
                    is ConnectionState.Failed -> ConnectionErrorContent(
                        topic = connection.help ?: HelpTopic.forFailure(connection.reason),
                        onRetry = onRetry,
                        onMoreHelp = { onHelp(null) },
                    )
                    ConnectionState.Connected -> {
                        state.preview?.let { TvPreview(it) }
                        RemotePad(
                            capabilities = state.capabilities,
                            touchpadMode = touchpadMode,
                            haptic = haptic,
                            actions = actions.copy(
                                onKeyboard = { showKeyboard = true },
                                onToggleTouchpad = { touchpadMode = !touchpadMode },
                                onApps = {
                                    actions.onApps()
                                    showApps = true
                                },
                            ),
                        )
                    }
                }
            }
        }
    }

    if (showKeyboard && state.supports(Capability.KEYBOARD)) {
        KeyboardDialog(
            onSend = {
                onText(it)
                showKeyboard = false
            },
            onDismiss = { showKeyboard = false },
        )
    }
    if (showApps && state.supports(Capability.APPS)) {
        AppsDialog(
            apps = apps,
            onLaunch = {
                onLaunchApp(it)
                showApps = false
            },
            onDismiss = { showApps = false },
        )
    }
}

@Composable
private fun Loading(deviceName: String?) {
    Box(Modifier.fillMaxWidth().heightIn(min = 240.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            if (deviceName != null) {
                Text(stringResource(R.string.remote_connecting, deviceName), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun DeviceMissing(onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.remote_device_missing), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_back)) }
    }
}

/** Tele guardada cuya plataforma todavía no tiene driver. */
@Composable
private fun PlatformComingSoon(device: TvDevice, name: String, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val platformName = stringResource(platformNameRes(device.platform))
        Text(
            stringResource(R.string.remote_platform_coming_soon, name, platformName),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_back)) }
    }
}

/** Solo en la tele simulada: provoca cada error para ver su pantalla de ayuda. */
@Composable
private fun SimulateErrorMenu(onSimulateFailure: (SimulatedFailure) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.BugReport, contentDescription = stringResource(R.string.remote_simulate_error))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                SimulatedFailure.TIMEOUT to R.string.simulate_timeout,
                SimulatedFailure.PAIRING_REJECTED to R.string.simulate_pairing_rejected,
                SimulatedFailure.PERMISSION_DENIED to R.string.simulate_permission_denied,
            ).forEach { (failure, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = {
                        expanded = false
                        onSimulateFailure(failure)
                    },
                )
            }
        }
    }
}

@Composable
private fun KeyboardDialog(onSend: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.keyboard_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.keyboard_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSend(text) }, enabled = text.isNotEmpty()) {
                Text(stringResource(R.string.keyboard_send))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun AppsDialog(apps: List<TvApp>, onLaunch: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.apps_title)) },
        text = {
            if (apps.isEmpty()) {
                Text(stringResource(R.string.apps_empty))
            } else {
                Column {
                    apps.forEach { app ->
                        TextButton(onClick = { onLaunch(app.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(demoAppName(app))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

@Preview(name = "Control — claro", locale = "es", heightDp = 900)
@Preview(name = "Remote — dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, heightDp = 900)
@Composable
private fun RemotePreview() {
    RemotoTheme(dynamicColor = false) {
        RemoteContent(
            state = RemoteUiState(
                loading = false,
                device = TvDevice("demo", "", Platform.DEMO),
                connection = ConnectionState.Connected,
                capabilities = Capability.entries.toSet(),
                preview = TvPreviewState(
                    tiles = (1..8).map { TvApp("app$it", "App $it") },
                    focusedTile = 1,
                    volume = 35,
                    channel = 7,
                ),
            ),
            apps = emptyList(),
            onBack = {},
            onHelp = {},
            actions = RemoteActions({ _, _ -> }, {}, {}, {}, {}, { _, _ -> }, {}),
            onText = {},
            onLaunchApp = {},
            onPair = {},
            onRetry = {},
            onSimulateFailure = {},
        )
    }
}
