package app.remoto.control.drivers.demo

import app.remoto.control.core.driver.Capability
import app.remoto.control.core.driver.ConnectionState
import app.remoto.control.core.driver.FailureReason
import app.remoto.control.core.driver.PairingType
import app.remoto.control.core.driver.RemoteKey
import app.remoto.control.core.driver.SimulatedFailure
import app.remoto.control.core.driver.SimulatedTv
import app.remoto.control.core.driver.TvApp
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.core.driver.TvDriver
import app.remoto.control.core.driver.TvPreviewState
import app.remoto.control.core.storage.SecretStore
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Tele simulada. Acepta todas las capacidades, reacciona a cada tecla y puede simular
 * errores. La usan quienes no tienen tele a mano y los revisores de Google Play.
 *
 * La primera vez pide emparejar con un código que "muestra" la tele simulada, igual que
 * Android TV. Guarda un token en el [SecretStore] para no volver a pedirlo.
 */
class DemoTvDriver(
    private val device: TvDevice,
    private val secrets: SecretStore,
    private val random: Random = Random.Default,
) : TvDriver,
    SimulatedTv {
    override val capabilities: Set<Capability> = Capability.entries.toSet()

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _preview = MutableStateFlow(TvPreviewState(tiles = DEMO_APPS))
    override val preview: StateFlow<TvPreviewState> = _preview.asStateFlow()

    private var pendingFailure: SimulatedFailure? = null
    private var expectedCode: String? = null

    /** Cambia con cada `connect`/`disconnect` para descartar conexiones que ya no valen. */
    private var attempt = 0

    override suspend fun connect() {
        val current = _state.value
        if (current == ConnectionState.Connecting || current == ConnectionState.Connected) return
        val myAttempt = ++attempt
        _state.value = ConnectionState.Connecting

        when (pendingFailure) {
            SimulatedFailure.TIMEOUT -> {
                delay(TIMEOUT_DELAY_MS)
                if (myAttempt == attempt) fail(FailureReason.TIMEOUT)
                return
            }
            SimulatedFailure.PERMISSION_DENIED -> {
                delay(CONNECT_DELAY_MS)
                if (myAttempt == attempt) fail(FailureReason.LOCAL_NETWORK_PERMISSION_DENIED)
                return
            }
            SimulatedFailure.PAIRING_REJECTED, null -> delay(CONNECT_DELAY_MS)
        }
        if (myAttempt != attempt) return

        val alreadyPaired = pendingFailure != SimulatedFailure.PAIRING_REJECTED &&
            secrets.get(device.id, TOKEN_KEY) != null
        if (myAttempt != attempt) return

        if (alreadyPaired) {
            _state.value = ConnectionState.Connected
        } else {
            val code = random.nextInt(0, 10_000).toString().padStart(4, '0')
            expectedCode = code
            _preview.update { it.copy(pairingCode = code) }
            _state.value = ConnectionState.PairingRequired(PairingType.CODE_ON_TV)
        }
    }

    override suspend fun pair(code: String?) {
        if (_state.value !is ConnectionState.PairingRequired) return
        val myAttempt = attempt
        delay(PAIR_DELAY_MS)
        if (myAttempt != attempt) return

        val accepted = pendingFailure != SimulatedFailure.PAIRING_REJECTED &&
            code?.trim() == expectedCode
        expectedCode = null
        _preview.update { it.copy(pairingCode = null) }
        if (accepted) {
            secrets.put(device.id, TOKEN_KEY, DEMO_TOKEN)
            _state.value = ConnectionState.Connected
        } else {
            fail(FailureReason.PAIRING_REJECTED)
        }
    }

    override suspend fun sendKey(key: RemoteKey, longPress: Boolean) {
        if (_state.value != ConnectionState.Connected) return
        _preview.update { applyKey(it, key).copy(lastKey = key, keyCount = it.keyCount + 1) }
    }

    override suspend fun sendText(text: String) {
        if (_state.value != ConnectionState.Connected) return
        _preview.update { it.copy(typedText = (it.typedText + text).takeLast(MAX_TEXT)) }
    }

    override suspend fun movePointer(dx: Float, dy: Float) {
        if (_state.value != ConnectionState.Connected) return
        _preview.update {
            it.copy(
                pointerX = (it.pointerX + dx * POINTER_SPEED).coerceIn(0f, 1f),
                pointerY = (it.pointerY + dy * POINTER_SPEED).coerceIn(0f, 1f),
                pointerVisible = true,
            )
        }
    }

    override suspend fun tap() {
        if (_state.value != ConnectionState.Connected) return
        _preview.update { tile(it).copy(lastKey = RemoteKey.OK, keyCount = it.keyCount + 1) }
    }

    override suspend fun listApps(): List<TvApp> = DEMO_APPS

    override suspend fun launchApp(appId: String) {
        if (_state.value != ConnectionState.Connected) return
        val app = DEMO_APPS.firstOrNull { it.id == appId } ?: return
        _preview.update {
            it.copy(
                poweredOn = true,
                openApp = app,
                playing = true,
                focusedTile = DEMO_APPS.indexOf(app),
            )
        }
    }

    override suspend fun powerOn() {
        _preview.update { it.copy(poweredOn = true) }
    }

    override fun disconnect() {
        attempt++
        expectedCode = null
        _preview.update { it.copy(pairingCode = null, pointerVisible = false) }
        _state.value = ConnectionState.Disconnected
    }

    override suspend fun simulateFailure(failure: SimulatedFailure) {
        pendingFailure = failure
        disconnect()
        connect()
    }

    private fun fail(reason: FailureReason) {
        pendingFailure = null
        expectedCode = null
        _preview.update { it.copy(pairingCode = null) }
        _state.value = ConnectionState.Failed.of(reason)
    }

    private fun tile(s: TvPreviewState): TvPreviewState {
        if (!s.poweredOn) return s
        // Con el touchpad, el clic abre el mosaico bajo el puntero (rejilla de COLUMNS x ROWS).
        val col = (s.pointerX * COLUMNS).toInt().coerceIn(0, COLUMNS - 1)
        val row = (s.pointerY * ROWS).toInt().coerceIn(0, ROWS - 1)
        val index = (row * COLUMNS + col).coerceAtMost(s.tiles.lastIndex)
        return if (s.openApp == null && index >= 0) {
            s.copy(focusedTile = index, openApp = s.tiles[index], playing = true)
        } else {
            s.copy(playing = !s.playing)
        }
    }

    companion object {
        const val CONNECT_DELAY_MS = 600L
        const val TIMEOUT_DELAY_MS = 2_500L
        const val PAIR_DELAY_MS = 400L
        const val TOKEN_KEY = "pairing_token"
        private const val DEMO_TOKEN = "demo-paired"
        private const val MAX_TEXT = 40
        private const val POINTER_SPEED = 0.002f
        const val COLUMNS = 4
        const val ROWS = 2
        const val VOLUME_STEP = 2
        const val MAX_CHANNEL = 999

        /** Apps inventadas: ningún nombre ni logotipo de marcas reales. */
        val DEMO_APPS = listOf(
            TvApp("movies", "Movies"),
            TvApp("series", "Series"),
            TvApp("music", "Music"),
            TvApp("news", "News"),
            TvApp("sports", "Sports"),
            TvApp("kids", "Kids"),
            TvApp("photos", "Photos"),
            TvApp("games", "Games"),
        )

        /** Efecto de cada tecla en la pantalla simulada. Pública para las pruebas. */
        fun applyKey(s: TvPreviewState, key: RemoteKey): TvPreviewState {
            if (key == RemoteKey.POWER) {
                return if (s.poweredOn) {
                    s.copy(poweredOn = false, openApp = null, playing = false, pointerVisible = false)
                } else {
                    s.copy(poweredOn = true)
                }
            }
            if (!s.poweredOn) return s
            val lastTile = (s.tiles.size - 1).coerceAtLeast(0)
            return when (key) {
                RemoteKey.UP -> s.moveFocusBy(-COLUMNS, lastTile)
                RemoteKey.DOWN -> s.moveFocusBy(COLUMNS, lastTile)
                RemoteKey.LEFT -> s.moveFocusBy(-1, lastTile)
                RemoteKey.RIGHT -> s.moveFocusBy(1, lastTile)
                RemoteKey.OK ->
                    if (s.openApp == null && s.tiles.isNotEmpty()) {
                        s.copy(openApp = s.tiles[s.focusedTile.coerceIn(0, lastTile)], playing = true)
                    } else {
                        s.copy(playing = !s.playing)
                    }
                RemoteKey.BACK -> s.copy(openApp = null, playing = false)
                RemoteKey.HOME -> s.copy(openApp = null, playing = false, focusedTile = 0)
                RemoteKey.VOLUME_UP -> s.copy(volume = (s.volume + VOLUME_STEP).coerceAtMost(100), muted = false)
                RemoteKey.VOLUME_DOWN -> s.copy(volume = (s.volume - VOLUME_STEP).coerceAtLeast(0), muted = false)
                RemoteKey.MUTE -> s.copy(muted = !s.muted)
                RemoteKey.CHANNEL_UP -> s.copy(channel = if (s.channel >= MAX_CHANNEL) 1 else s.channel + 1)
                RemoteKey.CHANNEL_DOWN -> s.copy(channel = if (s.channel <= 1) MAX_CHANNEL else s.channel - 1)
                RemoteKey.PLAY_PAUSE -> if (s.openApp != null) s.copy(playing = !s.playing) else s
                RemoteKey.POWER -> s // ya tratado arriba
            }
        }

        /** Mueve el foco en la rejilla de inicio; con una app abierta las flechas no hacen nada. */
        private fun TvPreviewState.moveFocusBy(delta: Int, last: Int): TvPreviewState {
            if (openApp != null) return this
            val next = focusedTile + delta
            return if (next in 0..last) copy(focusedTile = next) else this
        }
    }
}
