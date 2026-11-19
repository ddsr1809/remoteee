package app.remoto.control.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.core.storage.DeviceRepository
import app.remoto.control.core.storage.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val devices: List<TvDevice> = emptyList(),
    val lastDeviceId: String? = null,
) {
    /** Dispositivo para "Continuar con…": el último usado o, si no, el primero guardado. */
    val continueWith: TvDevice?
        get() = devices.firstOrNull { it.id == lastDeviceId } ?: devices.firstOrNull()
}

/** Lo comparten la bienvenida, el inicio y "próximamente". */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val devices: DeviceRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(devices.devices, settings.settings) { list, s ->
        HomeUiState(devices = list, lastDeviceId = s.lastDeviceId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _demoReady = Channel<String>(Channel.BUFFERED)

    /** Emite el id de la tele simulada cuando está lista para abrir su control. */
    val demoReady: Flow<String> = _demoReady.receiveAsFlow()

    /** "Probar sin tele": crea la tele simulada si hace falta y da por terminada la bienvenida. */
    fun startDemo() {
        viewModelScope.launch {
            val demo = devices.ensureDemoDevice()
            settings.setOnboardingDone()
            _demoReady.send(demo.id)
        }
    }
}
