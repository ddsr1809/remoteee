package app.remoto.control.ui.remote

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.remoto.control.core.driver.Capability
import app.remoto.control.core.driver.ConnectionState
import app.remoto.control.core.driver.RemoteKey
import app.remoto.control.core.driver.RemoteSession
import app.remoto.control.core.driver.SimulatedFailure
import app.remoto.control.core.driver.TvApp
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.core.driver.TvPreviewState
import app.remoto.control.core.storage.DeviceRepository
import app.remoto.control.core.storage.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RemoteUiState(
    /** Todavía buscando el dispositivo guardado. */
    val loading: Boolean = true,
    /** El dispositivo ya no existe o no hay driver para su plataforma. */
    val deviceMissing: Boolean = false,
    val device: TvDevice? = null,
    val connection: ConnectionState = ConnectionState.Disconnected,
    val capabilities: Set<Capability> = emptySet(),
    /** Pantalla de la tele simulada; `null` con teles reales. */
    val preview: TvPreviewState? = null,
    val hapticsEnabled: Boolean = true,
) {
    val isSimulated: Boolean get() = preview != null

    fun supports(capability: Capability): Boolean = capability in capabilities
}

/**
 * ViewModel del control. Solo conoce el núcleo ([RemoteSession]); nunca un driver concreto.
 */
@HiltViewModel
class RemoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val session: RemoteSession,
    private val devices: DeviceRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val deviceId: String = checkNotNull(savedStateHandle.get<String>(ARG_DEVICE_ID)) { "Falta $ARG_DEVICE_ID" }

    /** null = buscando; true = encontrado; false = no existe o no tiene driver. */
    private val found = MutableStateFlow<Boolean?>(null)

    private val _apps = MutableStateFlow<List<TvApp>>(emptyList())
    val apps: StateFlow<List<TvApp>> = _apps.asStateFlow()

    private val sessionState = combine(
        session.device,
        session.state,
        session.capabilities,
        session.preview,
    ) { device, connection, capabilities, preview ->
        // Mientras la sesión todavía apunta a otra tele, no se muestra nada de ella.
        if (device?.id == deviceId) {
            SessionSnapshot(device, connection, capabilities, preview)
        } else {
            SessionSnapshot(null, ConnectionState.Disconnected, emptySet(), null)
        }
    }

    val state: StateFlow<RemoteUiState> = combine(
        sessionState,
        found,
        settings.settings.map { it.hapticsEnabled },
    ) { snapshot, found, haptics ->
        RemoteUiState(
            loading = found == null,
            deviceMissing = found == false,
            device = snapshot.device,
            connection = snapshot.connection,
            capabilities = snapshot.capabilities,
            preview = snapshot.preview,
            hapticsEnabled = haptics,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemoteUiState())

    init {
        viewModelScope.launch {
            val device = devices.get(deviceId)
            if (device == null || !session.supports(device.platform)) {
                found.value = false
                return@launch
            }
            found.value = true
            session.open(device)
        }
    }

    fun onKey(key: RemoteKey, longPress: Boolean = false) {
        viewModelScope.launch { session.sendKey(key, longPress) }
    }

    /** Botón de encendido: si la tele simulada está apagada la enciende; si no, envía la tecla. */
    fun onPower() {
        viewModelScope.launch {
            val current = state.value
            if (current.preview?.poweredOn == false && current.supports(Capability.POWER_ON)) {
                session.powerOn()
            } else {
                session.sendKey(RemoteKey.POWER)
            }
        }
    }

    fun onText(text: String) {
        viewModelScope.launch { session.sendText(text) }
    }

    fun onPointerMove(dx: Float, dy: Float) {
        viewModelScope.launch { session.movePointer(dx, dy) }
    }

    fun onPointerTap() {
        viewModelScope.launch { session.tap() }
    }

    fun loadApps() {
        viewModelScope.launch { _apps.value = session.listApps() }
    }

    fun onLaunchApp(appId: String) {
        viewModelScope.launch { session.launchApp(appId) }
    }

    fun onPair(code: String?) {
        viewModelScope.launch { session.pair(code) }
    }

    fun onRetry() {
        viewModelScope.launch { session.retry() }
    }

    fun onSimulateFailure(failure: SimulatedFailure) {
        viewModelScope.launch { session.simulateFailure(failure) }
    }

    private data class SessionSnapshot(
        val device: TvDevice?,
        val connection: ConnectionState,
        val capabilities: Set<Capability>,
        val preview: TvPreviewState?,
    )

    companion object {
        /** Nombre de la propiedad en `RemoteRoute`; Navigation la guarda con ese nombre. */
        const val ARG_DEVICE_ID = "deviceId"
    }
}
