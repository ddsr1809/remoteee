package app.remoto.control.core.driver

import app.remoto.control.core.ApplicationScope
import app.remoto.control.core.diagnostics.DiagnosticsLog
import app.remoto.control.core.storage.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * La tele que se está controlando. Guarda el driver activo, expone su estado y filtra
 * las órdenes según las capacidades del driver.
 *
 * Es lo único del núcleo que la interfaz usa para controlar: no sabe qué marca hay detrás.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RemoteSession @Inject constructor(
    factories: Set<@JvmSuppressWildcards TvDriverFactory>,
    private val settings: SettingsRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
    private val log: DiagnosticsLog,
) {
    private class Active(val device: TvDevice, val driver: TvDriver)

    private val factoriesByPlatform = factories.associateBy { it.platform }
    private val active = MutableStateFlow<Active?>(null)
    private val mutex = Mutex()

    val device: StateFlow<TvDevice?> =
        active.map { it?.device }.stateIn(scope, SharingStarted.Eagerly, null)

    val state: StateFlow<ConnectionState> =
        active
            .flatMapLatest { it?.driver?.state ?: flowOf(ConnectionState.Disconnected) }
            .stateIn(scope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    val capabilities: StateFlow<Set<Capability>> =
        active.map { it?.driver?.capabilities.orEmpty() }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    /** Pantalla de la tele simulada; `null` si el driver activo es real. */
    val preview: StateFlow<TvPreviewState?> =
        active
            .flatMapLatest { (it?.driver as? SimulatedTv)?.preview ?: flowOf(null) }
            .stateIn(scope, SharingStarted.Eagerly, null)

    fun supports(platform: Platform): Boolean = platform in factoriesByPlatform

    /**
     * Activa [device] y se conecta. Si ya era el activo, solo reconecta si hace falta.
     * @return `false` si no hay driver para su plataforma.
     */
    suspend fun open(device: TvDevice): Boolean {
        val driver = mutex.withLock {
            val current = active.value
            if (current != null && current.device.id == device.id) {
                if (current.device != device) active.value = Active(device, current.driver)
                current.driver
            } else {
                val factory = factoriesByPlatform[device.platform] ?: return false
                current?.driver?.disconnect()
                factory.create(device).also { active.value = Active(device, it) }
            }
        }
        settings.setLastDeviceId(device.id)
        if (driver.state.value.canConnect) safely { driver.connect() }
        return true
    }

    /** Vuelve a intentar la conexión tras un fallo o una desconexión. */
    suspend fun retry() {
        val driver = active.value?.driver ?: return
        if (driver.state.value.canConnect) safely { driver.connect() }
    }

    suspend fun pair(code: String?) {
        val driver = active.value?.driver ?: return
        if (driver.state.value is ConnectionState.PairingRequired) safely { driver.pair(code) }
    }

    suspend fun sendKey(key: RemoteKey, longPress: Boolean = false) {
        command(key.capability) { it.sendKey(key, longPress) }
    }

    suspend fun sendText(text: String) {
        if (text.isEmpty()) return
        command(Capability.KEYBOARD) { it.sendText(text) }
    }

    suspend fun movePointer(dx: Float, dy: Float) {
        command(Capability.TOUCHPAD) { it.movePointer(dx, dy) }
    }

    suspend fun tap() {
        command(Capability.TOUCHPAD) { it.tap() }
    }

    suspend fun listApps(): List<TvApp> {
        var apps = emptyList<TvApp>()
        command(Capability.APPS) { apps = it.listApps() }
        return apps
    }

    suspend fun launchApp(appId: String) {
        command(Capability.APPS) { it.launchApp(appId) }
    }

    suspend fun powerOn() {
        command(Capability.POWER_ON) { it.powerOn() }
    }

    suspend fun simulateFailure(failure: SimulatedFailure) {
        val simulated = active.value?.driver as? SimulatedTv ?: return
        safely { simulated.simulateFailure(failure) }
    }

    /** Si [deviceId] es el activo, lo desconecta y lo suelta (por ejemplo, al borrarlo). */
    suspend fun forget(deviceId: String) {
        mutex.withLock {
            val current = active.value ?: return
            if (current.device.id == deviceId) {
                current.driver.disconnect()
                active.value = null
            }
        }
    }

    /** La app pasó a segundo plano: se sueltan las conexiones. */
    fun onAppBackground() {
        active.value?.driver?.disconnect()
    }

    /** La app volvió a primer plano: reconecta sola con el último dispositivo. */
    fun onAppForeground() {
        val driver = active.value?.driver ?: return
        if (driver.state.value == ConnectionState.Disconnected) {
            scope.launch { safely { driver.connect() } }
        }
    }

    private suspend inline fun command(capability: Capability, block: (TvDriver) -> Unit) {
        val driver = active.value?.driver ?: return
        if (capability !in driver.capabilities) return
        if (!driver.state.value.canSendCommands) return
        safely { block(driver) }
    }

    /**
     * Los drivers publican sus errores como estado; una excepción aquí es un error de
     * programación y no debe tumbar la app. Queda en el registro de diagnóstico.
     */
    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val platform = active.value?.device?.platform
            log.log(TAG, "Excepción inesperada del driver ${platform ?: "-"}", e)
        }
    }

    private companion object {
        const val TAG = "session"
    }
}
