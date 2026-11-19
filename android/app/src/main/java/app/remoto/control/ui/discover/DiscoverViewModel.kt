package app.remoto.control.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.remoto.control.core.discovery.DeviceAdder
import app.remoto.control.core.discovery.DeviceScanner
import app.remoto.control.core.discovery.DiscoveredDevice
import app.remoto.control.core.discovery.SavedDeviceMatcher
import app.remoto.control.core.discovery.ScanState
import app.remoto.control.core.discovery.devices
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.core.storage.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val scan: ScanState = ScanState.Idle,
    val saved: List<TvDevice> = emptyList(),
) {
    val found: List<DiscoveredDevice> get() = scan.devices
    val isSearching: Boolean get() = scan is ScanState.Idle || scan is ScanState.Scanning

    fun isSaved(device: DiscoveredDevice): Boolean = SavedDeviceMatcher.isSaved(saved, device)
}

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val scanner: DeviceScanner,
    private val adder: DeviceAdder,
    devices: DeviceRepository,
) : ViewModel() {
    private val scan = MutableStateFlow<ScanState>(ScanState.Idle)
    private var scanJob: Job? = null

    val state: StateFlow<DiscoverUiState> = combine(scan, devices.devices) { s, saved -> DiscoverUiState(s, saved) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    private val _added = Channel<String>(Channel.BUFFERED)

    /** Emite el id de la tele recién guardada para abrir su control. */
    val added: Flow<String> = _added.receiveAsFlow()

    init {
        search()
    }

    fun search() {
        scanJob?.cancel()
        scan.value = ScanState.Idle
        scanJob = viewModelScope.launch { scanner.scan().collect { scan.value = it } }
    }

    /** Al volver de los ajustes del sistema (Wi-Fi o permisos), se busca otra vez sola. */
    fun onResume() {
        val current = scan.value
        if (current is ScanState.NoWifi || current is ScanState.PermissionRequired || current is ScanState.Failed) {
            search()
        }
    }

    fun add(device: DiscoveredDevice) {
        viewModelScope.launch {
            val saved = adder.add(device) ?: return@launch
            _added.send(saved.id)
        }
    }
}
