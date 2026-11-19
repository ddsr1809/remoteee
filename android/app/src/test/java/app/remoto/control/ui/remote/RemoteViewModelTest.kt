package app.remoto.control.ui.remote

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.remoto.control.core.driver.Capability
import app.remoto.control.core.driver.ConnectionState
import app.remoto.control.core.driver.FailureReason
import app.remoto.control.core.driver.HelpTopic
import app.remoto.control.core.driver.PairingType
import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.RemoteKey
import app.remoto.control.core.driver.RemoteSession
import app.remoto.control.core.driver.SimulatedFailure
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.core.storage.AppSettings
import app.remoto.control.testutil.FakeDriverFactory
import app.remoto.control.testutil.FakeSimulatedTvDriver
import app.remoto.control.testutil.FakeTvDriver
import app.remoto.control.testutil.InMemoryDeviceRepository
import app.remoto.control.testutil.InMemorySettingsRepository
import app.remoto.control.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val tv = TvDevice("tv1", "Sala", Platform.DEMO)
    private val devices = InMemoryDeviceRepository(listOf(tv))
    private val settings = InMemorySettingsRepository(AppSettings(onboardingDone = true))

    private fun TestScope.viewModel(
        driver: FakeTvDriver = FakeTvDriver(),
        deviceId: String = tv.id,
    ): RemoteViewModel {
        val session = RemoteSession(setOf(FakeDriverFactory(Platform.DEMO) { driver }), settings, backgroundScope)
        return RemoteViewModel(
            SavedStateHandle(mapOf(RemoteViewModel.ARG_DEVICE_ID to deviceId)),
            session,
            devices,
            settings,
        )
    }

    /** `state` usa WhileSubscribed: hace falta alguien escuchando, como la pantalla. */
    private fun TestScope.observe(vm: RemoteViewModel) = backgroundScope.launch { vm.state.collect {} }

    @Test
    fun `al abrir conecta con la tele y expone estado y capacidades`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(FakeTvDriver(capabilities = setOf(Capability.NAVIGATION, Capability.VOLUME)))

        vm.state.test {
            val state = expectMostRecentItem()
            assertFalse(state.loading)
            assertEquals(tv, state.device)
            assertEquals(ConnectionState.Connected, state.connection)
            assertEquals(setOf(Capability.NAVIGATION, Capability.VOLUME), state.capabilities)
            assertFalse(state.isSimulated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `si la tele ya no existe lo indica`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(deviceId = "borrada")
        observe(vm)

        assertTrue(vm.state.value.deviceMissing)
        assertNull(vm.state.value.device)
    }

    @Test
    fun `las teclas llegan al driver`() = runTest(mainRule.dispatcher) {
        val driver = FakeTvDriver()
        val vm = viewModel(driver)
        observe(vm)

        vm.onKey(RemoteKey.OK)
        vm.onKey(RemoteKey.VOLUME_UP, longPress = true)
        vm.onText("hola")
        vm.onLaunchApp("a")

        assertEquals(listOf(RemoteKey.OK to false, RemoteKey.VOLUME_UP to true), driver.keys)
        assertEquals(listOf("hola"), driver.texts)
        assertEquals(listOf("a"), driver.launched)
    }

    @Test
    fun `las teclas sin capacidad no llegan al driver`() = runTest(mainRule.dispatcher) {
        val driver = FakeTvDriver(capabilities = setOf(Capability.NAVIGATION))
        val vm = viewModel(driver)
        observe(vm)

        vm.onKey(RemoteKey.CHANNEL_UP)
        vm.onText("hola")

        assertTrue(driver.keys.isEmpty())
        assertTrue(driver.texts.isEmpty())
    }

    @Test
    fun `un fallo se muestra con su tema de ayuda y reintentar reconecta`() = runTest(mainRule.dispatcher) {
        val driver = FakeTvDriver(connectResult = ConnectionState.Failed.of(FailureReason.TIMEOUT))
        val vm = viewModel(driver)
        observe(vm)

        val failed = vm.state.value.connection as ConnectionState.Failed
        assertEquals(HelpTopic.CONNECTION_TIMEOUT, failed.help)

        driver.connectResult = ConnectionState.Connected
        vm.onRetry()

        assertEquals(ConnectionState.Connected, vm.state.value.connection)
        assertEquals(2, driver.connectCalls)
    }

    @Test
    fun `emparejar envia el codigo al driver`() = runTest(mainRule.dispatcher) {
        val driver = FakeTvDriver(connectResult = ConnectionState.PairingRequired(PairingType.CODE_ON_TV))
        val vm = viewModel(driver)
        observe(vm)
        assertEquals(ConnectionState.PairingRequired(PairingType.CODE_ON_TV), vm.state.value.connection)

        vm.onPair("1234")

        assertEquals("1234", driver.pairCode)
        assertEquals(ConnectionState.Connected, vm.state.value.connection)
    }

    @Test
    fun `encendido con la tele simulada apagada la enciende; si no, envia la tecla`() = runTest(mainRule.dispatcher) {
        val driver = FakeSimulatedTvDriver()
        val vm = viewModel(driver)
        observe(vm)
        assertTrue(vm.state.value.isSimulated)

        vm.onPower()
        assertEquals(listOf(RemoteKey.POWER to false), driver.keys)

        driver.previewState.value = driver.previewState.value.copy(poweredOn = false)
        vm.onPower()
        assertEquals(1, driver.powerOnCalls)
        assertEquals(1, driver.keys.size)
    }

    @Test
    fun `simular un error llega a la tele simulada`() = runTest(mainRule.dispatcher) {
        val driver = FakeSimulatedTvDriver()
        val vm = viewModel(driver)
        observe(vm)

        vm.onSimulateFailure(SimulatedFailure.PERMISSION_DENIED)

        assertEquals(SimulatedFailure.PERMISSION_DENIED, driver.simulated)
    }

    @Test
    fun `respeta el ajuste de vibracion`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        observe(vm)
        assertTrue(vm.state.value.hapticsEnabled)

        settings.setHapticsEnabled(false)

        assertFalse(vm.state.value.hapticsEnabled)
    }

    @Test
    fun `cargar apps las pide al driver`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        observe(vm)

        vm.loadApps()

        assertEquals(listOf("a"), vm.apps.value.map { it.id })
    }
}
