package app.remoto.control.drivers.demo

import app.remoto.control.core.driver.Capability
import app.remoto.control.core.driver.ConnectionState
import app.remoto.control.core.driver.FailureReason
import app.remoto.control.core.driver.HelpTopic
import app.remoto.control.core.driver.PairingType
import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.RemoteKey
import app.remoto.control.core.driver.SimulatedFailure
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.core.driver.TvPreviewState
import app.remoto.control.testutil.InMemorySecretStore
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoTvDriverTest {
    private val device = TvDevice("demo", "", Platform.DEMO)
    private val secrets = InMemorySecretStore()

    private fun driver() = DemoTvDriver(device, secrets, Random(7))

    /** Conecta y empareja con el código que "muestra" la tele. */
    private suspend fun DemoTvDriver.connectAndPair() {
        connect()
        pair(preview.value.pairingCode)
    }

    @Test
    fun `declara todas las capacidades`() {
        assertEquals(Capability.entries.toSet(), driver().capabilities)
    }

    @Test
    fun `la primera vez pide el codigo que muestra la tele y luego conecta`() = runTest {
        val driver = driver()

        driver.connect()

        assertEquals(ConnectionState.PairingRequired(PairingType.CODE_ON_TV), driver.state.value)
        val code = driver.preview.value.pairingCode
        assertNotNull(code)
        assertEquals(4, code!!.length)

        driver.pair(code)

        assertEquals(ConnectionState.Connected, driver.state.value)
        assertNull(driver.preview.value.pairingCode)
        assertNotNull(secrets.get("demo", DemoTvDriver.TOKEN_KEY))
    }

    @Test
    fun `una vez emparejada reconecta sin pedir codigo`() = runTest {
        driver().connectAndPair()

        val again = driver() // p. ej. tras cerrar y reabrir la app
        again.connect()

        assertEquals(ConnectionState.Connected, again.state.value)
    }

    @Test
    fun `codigo incorrecto falla con su tema de ayuda`() = runTest {
        val driver = driver()
        driver.connect()

        driver.pair("0000x")

        assertEquals(
            ConnectionState.Failed(FailureReason.PAIRING_REJECTED, HelpTopic.PAIRING_REJECTED),
            driver.state.value,
        )
        assertNull(driver.preview.value.pairingCode)
    }

    @Test
    fun `simular tiempo agotado muestra su ayuda y reintentar funciona`() = runTest {
        val driver = driver()
        driver.connectAndPair()

        driver.simulateFailure(SimulatedFailure.TIMEOUT)

        assertEquals(
            ConnectionState.Failed(FailureReason.TIMEOUT, HelpTopic.CONNECTION_TIMEOUT),
            driver.state.value,
        )
        driver.connect() // "Reintentar": el error simulado solo ocurre una vez
        assertEquals(ConnectionState.Connected, driver.state.value)
    }

    @Test
    fun `simular permiso negado muestra la ayuda del permiso de red local`() = runTest {
        val driver = driver()
        driver.connectAndPair()

        driver.simulateFailure(SimulatedFailure.PERMISSION_DENIED)

        assertEquals(
            ConnectionState.Failed(FailureReason.LOCAL_NETWORK_PERMISSION_DENIED, HelpTopic.LOCAL_NETWORK_PERMISSION),
            driver.state.value,
        )
    }

    @Test
    fun `simular emparejamiento rechazado pide codigo y lo rechaza aunque sea correcto`() = runTest {
        val driver = driver()
        driver.connectAndPair()

        driver.simulateFailure(SimulatedFailure.PAIRING_REJECTED)
        assertTrue(driver.state.value is ConnectionState.PairingRequired)

        driver.pair(driver.preview.value.pairingCode)

        assertEquals(
            ConnectionState.Failed(FailureReason.PAIRING_REJECTED, HelpTopic.PAIRING_REJECTED),
            driver.state.value,
        )
        driver.connect()
        assertEquals(ConnectionState.Connected, driver.state.value) // el token sigue guardado
    }

    @Test
    fun `desconectar a mitad de conexion la descarta`() = runTest {
        val driver = driver()
        launch { driver.connect() }
        advanceTimeBy(DemoTvDriver.CONNECT_DELAY_MS / 2)
        assertEquals(ConnectionState.Connecting, driver.state.value)

        driver.disconnect()
        advanceUntilIdle()

        assertEquals(ConnectionState.Disconnected, driver.state.value)
    }

    @Test
    fun `cada tecla produce una reaccion visible`() = runTest {
        val driver = driver()
        driver.connectAndPair()

        RemoteKey.entries.forEachIndexed { index, key ->
            driver.sendKey(key)
            assertEquals(key, driver.preview.value.lastKey)
            assertEquals(index + 1, driver.preview.value.keyCount)
        }
    }

    @Test
    fun `sin conexion las teclas se ignoran`() = runTest {
        val driver = driver()

        driver.sendKey(RemoteKey.VOLUME_UP)
        driver.sendText("hola")

        assertEquals(0, driver.preview.value.keyCount)
        assertEquals("", driver.preview.value.typedText)
    }

    @Test
    fun `volumen, silencio y canales`() {
        var s = TvPreviewState(volume = 99, channel = 999)
        s = DemoTvDriver.applyKey(s, RemoteKey.VOLUME_UP)
        assertEquals(100, s.volume)
        s = DemoTvDriver.applyKey(s, RemoteKey.VOLUME_UP)
        assertEquals(100, s.volume)
        s = DemoTvDriver.applyKey(s, RemoteKey.MUTE)
        assertTrue(s.muted)
        s = DemoTvDriver.applyKey(s, RemoteKey.VOLUME_DOWN)
        assertFalse(s.muted)
        assertEquals(98, s.volume)
        s = DemoTvDriver.applyKey(s, RemoteKey.CHANNEL_UP)
        assertEquals(1, s.channel)
        s = DemoTvDriver.applyKey(s, RemoteKey.CHANNEL_DOWN)
        assertEquals(DemoTvDriver.MAX_CHANNEL, s.channel)
    }

    @Test
    fun `navegacion por los mosaicos, abrir y volver`() {
        var s = TvPreviewState(tiles = DemoTvDriver.DEMO_APPS)
        s = DemoTvDriver.applyKey(s, RemoteKey.LEFT)
        assertEquals(0, s.focusedTile) // no sale de la rejilla
        s = DemoTvDriver.applyKey(s, RemoteKey.RIGHT)
        s = DemoTvDriver.applyKey(s, RemoteKey.DOWN)
        assertEquals(1 + DemoTvDriver.COLUMNS, s.focusedTile)
        s = DemoTvDriver.applyKey(s, RemoteKey.OK)
        assertEquals(DemoTvDriver.DEMO_APPS[1 + DemoTvDriver.COLUMNS], s.openApp)
        assertTrue(s.playing)
        s = DemoTvDriver.applyKey(s, RemoteKey.PLAY_PAUSE)
        assertFalse(s.playing)
        s = DemoTvDriver.applyKey(s, RemoteKey.BACK)
        assertNull(s.openApp)
        s = DemoTvDriver.applyKey(s, RemoteKey.HOME)
        assertEquals(0, s.focusedTile)
    }

    @Test
    fun `apagada solo responde al boton de encendido`() {
        var s = DemoTvDriver.applyKey(TvPreviewState(volume = 10), RemoteKey.POWER)
        assertFalse(s.poweredOn)
        s = DemoTvDriver.applyKey(s, RemoteKey.VOLUME_UP)
        assertEquals(10, s.volume)
        s = DemoTvDriver.applyKey(s, RemoteKey.POWER)
        assertTrue(s.poweredOn)
    }

    @Test
    fun `texto, apps y encendido`() = runTest {
        val driver = driver()
        driver.connectAndPair()

        driver.sendText("hola")
        assertEquals("hola", driver.preview.value.typedText)

        driver.launchApp("music")
        assertEquals("music", driver.preview.value.openApp?.id)

        driver.sendKey(RemoteKey.POWER)
        assertFalse(driver.preview.value.poweredOn)
        driver.powerOn()
        assertTrue(driver.preview.value.poweredOn)
    }
}
