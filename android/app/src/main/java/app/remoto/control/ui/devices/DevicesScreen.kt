package app.remoto.control.ui.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.ui.components.RemotoTopBar
import app.remoto.control.ui.components.deviceDisplayName
import app.remoto.control.ui.components.platformNameRes
import app.remoto.control.ui.theme.RemotoTheme

@Composable
fun DevicesScreen(
    onBack: () -> Unit,
    onOpenRemote: (String) -> Unit,
    viewModel: DevicesViewModel = hiltViewModel(),
) {
    val devices by viewModel.list.collectAsStateWithLifecycle()
    DevicesContent(
        devices = devices,
        onBack = onBack,
        onOpenRemote = onOpenRemote,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
    )
}

@Composable
private fun DevicesContent(
    devices: List<TvDevice>,
    onBack: () -> Unit,
    onOpenRemote: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(topBar = { RemotoTopBar(title = stringResource(R.string.devices_title), onBack = onBack) }) { padding ->
        if (devices.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.devices_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(devices, key = { it.id }) { device ->
                    val name = deviceDisplayName(device)
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { Text(stringResource(platformNameRes(device.platform))) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { renaming = device.id }) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.devices_rename_named, name),
                                    )
                                }
                                IconButton(onClick = { deleting = device.id }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.devices_delete_named, name),
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { onOpenRemote(device.id) },
                    )
                }
            }
        }
    }

    devices.firstOrNull { it.id == renaming }?.let { device ->
        RenameDialog(
            current = deviceDisplayName(device),
            onConfirm = {
                onRename(device.id, it)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    devices.firstOrNull { it.id == deleting }?.let { device ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.devices_delete_title, deviceDisplayName(device))) },
            text = { Text(stringResource(R.string.devices_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(device.id)
                    deleting = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.devices_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_NAME) },
                label = { Text(stringResource(R.string.devices_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private const val MAX_NAME = 40

@Preview(name = "Dispositivos — claro", locale = "es")
@Preview(name = "Devices — dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DevicesPreview() {
    RemotoTheme(dynamicColor = false) {
        DevicesContent(
            devices = listOf(
                TvDevice("demo", "", Platform.DEMO),
                TvDevice("a", "Sala", Platform.ANDROID_TV, "192.0.2.10"),
            ),
            onBack = {},
            onOpenRemote = {},
            onRename = { _, _ -> },
            onDelete = {},
        )
    }
}
