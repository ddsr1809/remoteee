package app.remoto.control.core.driver

import app.remoto.control.core.diagnostics.DiagnosticsLog
import app.remoto.control.testutil.FakeDriverFactory
import app.remoto.control.testutil.FakeTvDriver
import app.remoto.control.testutil.InMemorySettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteSessionTest {
    private val tv1 = TvDevice("tv1", "Sala", Platform.DEMO)
    private val tv2 = TvDevice("tv2", "Cuarto", Platform.DEMO)
    private val settings = InMemorySettingsRepository()

    private fun session(scope: CoroutineScope, factory: FakeDriverFactory = FakeDriverFactory(Platform.DEMO)) =
        RemoteSession(setOf(factory), settings, scope, DiagnosticsLog())

    @Test
    fun `abrir un dispositivo crea su driver, conecta y lo recuerda`() = runTest(UnconfinedTestDispatcher()) {
        val factory = FakeDriverFactory(Platform.DEMO)
        val session = session(backgroundScope, factory)

        assertTrue(session.open(tv1))

        assertEquals(1, factory.created.size)
        assertEquals(1, factory.created.single().connectCalls)
        assertEquals(ConnectionState.Connected, session.state.value)
        assertEquals(tv1, session.device.value)
        assertEquals("tv1", settings.current.value.lastDeviceId)
    }

    @Test
    fun `abrir otra vez el mismo dispositivo no crea otro driver`() = runTest(UnconfinedTestDispatcher()) {
        val factory = FakeDriverFactory(Platform.DEMO)
        val session = session(backgroundScope, factory)

        session.open(tv1)
        session.open(tv1.copy(name = "Sala nueva"))

        assertEquals(1, factory.created.size)
        assertEquals(1, factory.created.single().connectCalls) // ya estaba conectado
        assertEquals("Sala nueva", session.device.value?.name)
    }

    @Test
    fun `cambiar de dispositivo desconecta el anterior`() = runTest(UnconfinedTestDispatcher()) {
        val factory = FakeDriverFactory(Platform.DEMO)
        val session = session(backgroundScope, factory)

        session.open(tv1)
        session.open(tv2)

        val (first, second) = factory.created
        assertEquals(1, first.disconnectCalls)
        assertEquals(1, second.connectCalls)
        assertEquals(tv2, session.device.value)
    }

    @Test
    fun `sin driver para la plataforma no abre nada`() = runTest(UnconfinedTestDispatcher()) {
        val session = session(backgroundScope)

        assertFalse(session.open(TvDevice("x", "", Platform.ROKU)))
        assertNull(session.device.value)
        assertFalse(session.supports(Platform.ROKU))
        assertTrue(session.supports(Platform.DEMO))
    }

    @Test
    fun `las teclas llegan al driver solo si esta conectado y declara la capacidad`() =
        runTest(UnconfinedTestDispatcher()) {
            val driver = FakeTvDriver(capabilities = setOf(Capability.NAVIGATION))
            val session = session(backgroundScope, FakeDriverFactory(Platform.DEMO) { driver })
            session.open(tv1)

            session.sendKey(RemoteKey.OK)
            session.sendKey(RemoteKey.VOLUME_UP) // sin capacidad VOLUME
            session.sendText("hola") // sin capacidad KEYBOARD

            assertEquals(listOf(RemoteKey.OK to false), driver.keys)
            assertTrue(driver.texts.isEmpty())

            driver.setState(ConnectionState.Failed.of(FailureReason.TIMEOUT))
            session.sendKey(RemoteKey.UP, longPress = true)
            assertEquals(1, driver.keys.size) // ignorada: no está conectado
        }

    @Test
    fun `al ir a segundo plano desconecta y al volver reconecta solo`() = runTest(UnconfinedTestDispatcher()) {
        val factory = FakeDriverFactory(Platform.DEMO)
        val session = session(backgroundScope, factory)
        session.open(tv1)
        val driver = factory.created.single()

        session.onAppBackground()
        assertEquals(ConnectionState.Disconnected, session.state.value)

        session.onAppForeground()
        assertEquals(2, driver.connectCalls)
        assertEquals(ConnectionState.Connected, session.state.value)
    }

    @Test
    fun `al volver a primer plano no reintenta un fallo`() = runTest(UnconfinedTestDispatcher()) {
        val driver = FakeTvDriver(connectResult = ConnectionState.Failed.of(FailureReason.PAIRING_REJECTED))
        val session = session(backgroundScope, FakeDriverFactory(Platform.DEMO) { driver })
        session.open(tv1)

        session.onAppForeground()

        assertEquals(1, driver.connectCalls)
        val failed = session.state.value as ConnectionState.Failed
        assertEquals(HelpTopic.PAIRING_REJECTED, failed.help)
    }

    @Test
    fun `reintentar vuelve a conectar tras un fallo`() = runTest(UnconfinedTestDispatcher()) {
        val driver = FakeTvDriver(connectResult = ConnectionState.Failed.of(FailureReason.TIMEOUT))
        val session = session(backgroundScope, FakeDriverFactory(Platform.DEMO) { driver })
        session.open(tv1)

        driver.connectResult = ConnectionState.Connected
        session.retry()

        assertEquals(2, driver.connectCalls)
        assertEquals(ConnectionState.Connected, session.state.value)
    }

    @Test
    fun `olvidar el dispositivo activo lo desconecta y lo suelta`() = runTest(UnconfinedTestDispatcher()) {
        val factory = FakeDriverFactory(Platform.DEMO)
        val session = session(backgroundScope, factory)
        session.open(tv1)

        session.forget("otro")
        assertEquals(tv1, session.device.value)

        session.forget("tv1")
        assertNull(session.device.value)
        assertEquals(ConnectionState.Disconnected, session.state.value)
        assertEquals(1, factory.created.single().disconnectCalls)
    }

    @Test
    fun `una excepcion del driver no tumba la sesion`() = runTest(UnconfinedTestDispatcher()) {
        val driver = FakeTvDriver().apply { throwOnKey = true }
        val session = session(backgroundScope, FakeDriverFactory(Platform.DEMO) { driver })
        session.open(tv1)

        session.sendKey(RemoteKey.OK)

        assertEquals(ConnectionState.Connected, session.state.value)
    }
}
