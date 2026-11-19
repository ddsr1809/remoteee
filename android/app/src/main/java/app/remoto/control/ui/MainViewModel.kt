package app.remoto.control.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.remoto.control.core.storage.DeviceRepository
import app.remoto.control.core.storage.SettingsRepository
import app.remoto.control.core.storage.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Cómo arranca la app: bienvenida la primera vez; si no, inicio y, si lo hay, el último control. */
sealed interface StartDestination {
    data object Loading : StartDestination

    data object Welcome : StartDestination

    data class Home(val openDeviceId: String?) : StartDestination
}

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
    devices: DeviceRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settings.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val start: StateFlow<StartDestination> = flow {
        val current = settings.settings.first()
        val lastDevice = current.lastDeviceId?.let { devices.get(it) }
        emit(
            if (!current.onboardingDone) {
                StartDestination.Welcome
            } else {
                StartDestination.Home(openDeviceId = lastDevice?.id)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StartDestination.Loading)
}
