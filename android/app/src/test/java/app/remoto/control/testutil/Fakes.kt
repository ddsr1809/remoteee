package app.remoto.control.testutil

import app.remoto.control.core.driver.Capability
import app.remoto.control.core.driver.ConnectionState
import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.RemoteKey
import app.remoto.control.core.driver.SimulatedFailure
import app.remoto.control.core.driver.SimulatedTv
import app.remoto.control.core.driver.TvApp
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.core.driver.TvDriver
import app.remoto.control.core.driver.TvDriverFactory
import app.remoto.control.core.driver.TvPreviewState
import app.remoto.control.core.storage.AppSettings
import app.remoto.control.core.storage.DEMO_DEVICE_ID
import app.remoto.control.core.storage.DeviceRepository
import app.remoto.control.core.storage.SecretStore
import app.remoto.control.core.storage.SettingsRepository
import app.remoto.control.core.storage.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class InMemorySecretStore : SecretStore {
    val values = mutableMapOf<String, String>()

    override suspend fun get(deviceId: String, name: String): String? = values["$deviceId/$name"]

    override suspend fun put(deviceId: String, name: String, value: String) {
        values["$deviceId/$name"] = value
    }

    override suspend fun deleteAll(deviceId: String) {
        values.keys.removeAll { it.startsWith("$deviceId/") }
    }
}

class InMemorySettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    val current = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = current

    override suspend fun setOnboardingDone() = current.update { it.copy(onboardingDone = true) }

    override suspend fun setHapticsEnabled(enabled: Boolean) = current.update { it.copy(hapticsEnabled = enabled) }

    override suspend fun setThemeMode(mode: ThemeMode) = current.update { it.copy(themeMode = mode) }

    override suspend fun setLastDeviceId(id: String?) = current.update { it.copy(lastDeviceId = id) }
}

class InMemoryDeviceRepository(
    initial: List<TvDevice> = emptyList(),
    private val secrets: SecretStore = InMemorySecretStore(),
) : DeviceRepository {
    val current = MutableStateFlow(initial)
    override val devices: Flow<List<TvDevice>> = current

    override suspend fun get(id: String): TvDevice? = current.value.firstOrNull { it.id == id }

    override suspend fun upsert(device: TvDevice) =
        current.update { list -> list.filterNot { it.id == device.id } + device }

    override suspend fun rename(id: String, name: String) =
        current.update { list -> list.map { if (it.id == id) it.copy(name = name) else it } }

    override suspend fun delete(id: String) {
        current.update { list -> list.filterNot { it.id == id } }
        secrets.deleteAll(id)
    }

    override suspend fun ensureDemoDevice(): TvDevice =
        get(DEMO_DEVICE_ID) ?: TvDevice(DEMO_DEVICE_ID, "", Platform.DEMO).also { upsert(it) }
}

/** Driver controlable desde la prueba. Registra todo lo que recibe. */
open class FakeTvDriver(
    override val capabilities: Set<Capability> = Capability.entries.toSet(),
    /** Estado al que pasa `connect()`. */
    var connectResult: ConnectionState = ConnectionState.Connected,
) : TvDriver {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state

    var connectCalls = 0
    var disconnectCalls = 0
    var powerOnCalls = 0
    var pairCode: String? = null
    var throwOnKey = false
    val keys = mutableListOf<Pair<RemoteKey, Boolean>>()
    val texts = mutableListOf<String>()
    val launched = mutableListOf<String>()

    fun setState(state: ConnectionState) {
        _state.value = state
    }

    override suspend fun connect() {
        connectCalls++
        _state.value = connectResult
    }

    override suspend fun pair(code: String?) {
        pairCode = code
        _state.value = ConnectionState.Connected
    }

    override suspend fun sendKey(key: RemoteKey, longPress: Boolean) {
        if (throwOnKey) error("Fallo simulado del driver")
        keys += key to longPress
    }

    override suspend fun sendText(text: String) {
        texts += text
    }

    override suspend fun movePointer(dx: Float, dy: Float) = Unit

    override suspend fun tap() = Unit

    override suspend fun listApps(): List<TvApp> = listOf(TvApp("a", "A"))

    override suspend fun launchApp(appId: String) {
        launched += appId
    }

    override suspend fun powerOn() {
        powerOnCalls++
    }

    override fun disconnect() {
        disconnectCalls++
        _state.value = ConnectionState.Disconnected
    }
}

class FakeSimulatedTvDriver : FakeTvDriver(), SimulatedTv {
    val previewState = MutableStateFlow(TvPreviewState())
    override val preview: StateFlow<TvPreviewState> = previewState
    var simulated: SimulatedFailure? = null

    override suspend fun simulateFailure(failure: SimulatedFailure) {
        simulated = failure
    }
}

class FakeDriverFactory(
    override val platform: Platform,
    private val make: () -> FakeTvDriver = { FakeTvDriver() },
) : TvDriverFactory {
    val created = mutableListOf<FakeTvDriver>()

    override fun create(device: TvDevice): TvDriver = make().also { created += it }
}
