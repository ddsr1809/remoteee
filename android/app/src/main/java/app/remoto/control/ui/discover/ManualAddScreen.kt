package app.remoto.control.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.remoto.control.R
import app.remoto.control.core.discovery.DeviceAdder
import app.remoto.control.core.driver.Platform
import app.remoto.control.core.net.HostValidator
import app.remoto.control.ui.components.RemotoTopBar
import app.remoto.control.ui.components.platformNameRes
import app.remoto.control.ui.theme.RemotoTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Plataformas que se pueden agregar por IP (las que tendrán driver de red). */
private val MANUAL_PLATFORMS = listOf(Platform.ANDROID_TV, Platform.SAMSUNG, Platform.LG, Platform.ROKU)

@HiltViewModel
class ManualAddViewModel @Inject constructor(
    private val adder: DeviceAdder,
) : ViewModel() {
    private val _added = Channel<String>(Channel.BUFFERED)
    val added: Flow<String> = _added.receiveAsFlow()

    fun save(ip: String, platform: Platform) {
        val host = HostValidator.normalizeIpv4(ip) ?: return
        if (HostValidator.validateIpv4(host) != HostValidator.Result.VALID) return
        viewModelScope.launch { _added.send(adder.add(platform, host).id) }
    }
}

@Composable
fun ManualAddScreen(
    onBack: () -> Unit,
    onAdded: (String) -> Unit,
    viewModel: ManualAddViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) { viewModel.added.collect { onAdded(it) } }
    ManualAddContent(onBack = onBack, onSave = viewModel::save)
}

@Composable
private fun ManualAddContent(onBack: () -> Unit, onSave: (String, Platform) -> Unit) {
    var ip by rememberSaveable { mutableStateOf("") }
    var platformName by rememberSaveable { mutableStateOf<String?>(null) }
    val platform = MANUAL_PLATFORMS.firstOrNull { it.name == platformName }
    val result = HostValidator.validateIpv4(ip)
    // El error se muestra cuando ya hay algo escrito; un campo vacío no es un error todavía.
    val error = when (result) {
        HostValidator.Result.INVALID_FORMAT -> R.string.manual_ip_error_format
        HostValidator.Result.NOT_A_DEVICE_ADDRESS -> R.string.manual_ip_error_reserved
        HostValidator.Result.VALID, HostValidator.Result.EMPTY -> null
    }
    val warning = result == HostValidator.Result.VALID && !HostValidator.isPrivateIpv4(ip.trim())
    val supporting = error ?: R.string.manual_ip_warning_public.takeIf { warning }
    val canSave = result == HostValidator.Result.VALID && platform != null

    Scaffold(topBar = { RemotoTopBar(title = stringResource(R.string.manual_title), onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.manual_instructions), style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = ip,
                onValueChange = { new -> ip = new.filter { it.isDigit() || it == '.' }.take(MAX_IP_LENGTH) },
                label = { Text(stringResource(R.string.manual_ip_label)) },
                placeholder = { Text(stringResource(R.string.manual_ip_placeholder)) },
                singleLine = true,
                isError = error != null,
                supportingText = if (supporting != null) {
                    { Text(stringResource(supporting)) }
                } else {
                    null
                },
                // Uri muestra el punto en todos los teclados; algunos teclados numéricos no lo tienen.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.manual_platform_label), style = MaterialTheme.typography.titleMedium)
            Column(Modifier.selectableGroup()) {
                MANUAL_PLATFORMS.forEach { p ->
                    ListItem(
                        headlineContent = { Text(stringResource(platformNameRes(p))) },
                        leadingContent = { RadioButton(selected = platform == p, onClick = null) },
                        modifier = Modifier.selectable(
                            selected = platform == p,
                            role = Role.RadioButton,
                            onClick = { platformName = p.name },
                        ),
                    )
                }
            }
            Button(
                onClick = { platform?.let { onSave(ip, it) } },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.manual_save))
            }
        }
    }
}

/** "255.255.255.255" tiene 15 caracteres. */
private const val MAX_IP_LENGTH = 15

@Preview(name = "IP manual — claro", locale = "es", heightDp = 800)
@Preview(name = "Manual IP — dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, heightDp = 800)
@Composable
private fun ManualAddPreview() {
    RemotoTheme(dynamicColor = false) {
        ManualAddContent(onBack = {}, onSave = { _, _ -> })
    }
}
