package app.remoto.control.ui.discover

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.remoto.control.R
import app.remoto.control.core.discovery.DiscoveredDevice
import app.remoto.control.core.discovery.ScanState
import app.remoto.control.core.driver.FailureReason
import app.remoto.control.core.driver.HelpTopic
import app.remoto.control.core.driver.Platform
import app.remoto.control.ui.components.RemotoTopBar
import app.remoto.control.ui.components.platformNameRes
import app.remoto.control.ui.help.ConnectionErrorContent
import app.remoto.control.ui.help.text
import app.remoto.control.ui.theme.RemotoTheme

/** Buscar en mi Wi-Fi. Aquí podrá ir un banner en el hito 6 (permitido en esta pantalla). */
@Composable
fun DiscoverScreen(
    onBack: () -> Unit,
    onAdded: (String) -> Unit,
    onNotFound: () -> Unit,
    onHelp: (HelpTopic) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.added.collect { onAdded(it) } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    DiscoverContent(
        state = state,
        onBack = onBack,
        onSearch = viewModel::search,
        onAdd = viewModel::add,
        onNotFound = onNotFound,
        onHelp = onHelp,
    )
}

@Composable
private fun DiscoverContent(
    state: DiscoverUiState,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onAdd: (DiscoveredDevice) -> Unit,
    onNotFound: () -> Unit,
    onHelp: (HelpTopic) -> Unit,
) {
    Scaffold(
        topBar = {
            RemotoTopBar(
                title = stringResource(R.string.start_option_wifi),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onSearch, enabled = !state.isSearching) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.discover_search_again))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isSearching && state.found.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (val scan = state.scan) {
                    ScanState.NoWifi -> NoWifi(onSearch = onSearch, onHelp = { onHelp(HelpTopic.NO_WIFI) })
                    ScanState.PermissionRequired ->
                        LocalNetworkPermissionContent(onTryAgain = onSearch, modifier = Modifier.padding(16.dp))
                    is ScanState.Failed ->
                        if (scan.reason == FailureReason.LOCAL_NETWORK_PERMISSION_DENIED) {
                            LocalNetworkPermissionContent(onTryAgain = onSearch, modifier = Modifier.padding(16.dp))
                        } else {
                            ConnectionErrorContent(
                                topic = HelpTopic.forFailure(scan.reason),
                                onRetry = onSearch,
                                onMoreHelp = onNotFound,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    ScanState.Idle, is ScanState.Scanning, is ScanState.Finished ->
                        Results(state = state, onAdd = onAdd, onSearch = onSearch)
                }
            }
            HorizontalDivider()
            TextButton(
                onClick = onNotFound,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp),
            ) {
                Text(stringResource(R.string.discover_not_found))
            }
        }
    }
}

@Composable
private fun Results(state: DiscoverUiState, onAdd: (DiscoveredDevice) -> Unit, onSearch: () -> Unit) {
    val found = state.found
    when {
        found.isEmpty() && state.isSearching -> Box(
            Modifier.fillMaxWidth().heightIn(min = 240.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.discover_searching), style = MaterialTheme.typography.bodyLarge)
            }
        }
        found.isEmpty() -> Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.discover_none_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.discover_none_body), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.discover_search_again))
            }
        }
        else -> {
            Text(
                stringResource(R.string.discover_pick),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
            found.forEach { device -> FoundDevice(device, saved = state.isSaved(device), onAdd = { onAdd(device) }) }
        }
    }
}

@Composable
private fun FoundDevice(device: DiscoveredDevice, saved: Boolean, onAdd: () -> Unit) {
    val platform = device.platform ?: return
    val platformName = stringResource(platformNameRes(platform))
    ListItem(
        headlineContent = { Text(device.name ?: platformName) },
        supportingContent = { Text(stringResource(R.string.discover_device_detail, platformName, device.host)) },
        leadingContent = { Icon(Icons.Filled.Tv, contentDescription = null) },
        trailingContent = {
            if (saved) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.discover_saved),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        modifier = Modifier
            .heightIn(min = 56.dp)
            .clickable(
                onClickLabel = stringResource(if (saved) R.string.home_open_remote else R.string.discover_add),
                role = Role.Button,
                onClick = onAdd,
            ),
    )
}

@Composable
private fun NoWifi(onSearch: () -> Unit, onHelp: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            Icons.Filled.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
        )
        Text(stringResource(HelpTopic.NO_WIFI.text().title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.discover_no_wifi_body), style = MaterialTheme.typography.bodyLarge)
        Button(
            onClick = {
                // Android 10+: panel rápido de Wi-Fi sin salir de la app.
                val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Settings.Panel.ACTION_WIFI
                } else {
                    Settings.ACTION_WIFI_SETTINGS
                }
                runCatching { context.startActivity(Intent(action)) }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.discover_open_wifi_settings))
        }
        OutlinedButton(onClick = onSearch, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.discover_search_again))
        }
        TextButton(onClick = onHelp) { Text(stringResource(R.string.action_more_help)) }
    }
}

@Preview(name = "Buscar — resultados", locale = "es")
@Preview(name = "Search — dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DiscoverResultsPreview() {
    RemotoTheme(dynamicColor = false) {
        DiscoverContent(
            state = DiscoverUiState(
                scan = ScanState.Scanning(
                    listOf(
                        DiscoveredDevice("192.168.1.20", Platform.ANDROID_TV, name = "Sala"),
                        DiscoveredDevice("192.168.1.31", Platform.SAMSUNG),
                        DiscoveredDevice("192.168.1.40", Platform.ROKU),
                    ),
                ),
            ),
            onBack = {},
            onSearch = {},
            onAdd = {},
            onNotFound = {},
            onHelp = {},
        )
    }
}

@Preview(name = "Buscar — sin Wi-Fi", locale = "es")
@Composable
private fun DiscoverNoWifiPreview() {
    RemotoTheme(dynamicColor = false) {
        DiscoverContent(DiscoverUiState(scan = ScanState.NoWifi), {}, {}, {}, {}, {})
    }
}
