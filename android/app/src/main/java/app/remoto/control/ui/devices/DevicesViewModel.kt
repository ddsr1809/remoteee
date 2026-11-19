package app.remoto.control.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.remoto.control.core.driver.RemoteSession
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.core.storage.DeviceRepository
import app.remoto.control.core.storage.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Teles guardadas. El límite de una tele en la versión gratis llega con "Pro" en el hito 6. */
@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val devices: DeviceRepository,
    private val settings: SettingsRepository,
    private val session: RemoteSession,
) : ViewModel() {
    val list: StateFlow<List<TvDevice>> =
        devices.devices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun rename(id: String, name: String) {
        viewModelScope.launch { devices.rename(id, name) }
    }

    /** Borra la tele y sus secretos; si era la activa, se desconecta. */
    fun delete(id: String) {
        viewModelScope.launch {
            session.forget(id)
            devices.delete(id)
            if (settings.settings.first().lastDeviceId == id) settings.setLastDeviceId(null)
        }
    }
}
