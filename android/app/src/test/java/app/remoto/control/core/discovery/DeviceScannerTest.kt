package app.remoto.control.core.discovery

import app.remoto.control.core.diagnostics.DiagnosticsLog
import app.remoto.control.core.driver.FailureReason
import app.remoto.control.core.driver.Platform
import app.remoto.control.core.net.LocalNetwork
import app.remoto.control.core.net.LocalNetworkAccess
import app.remoto.control.core.net.LocalNetworkPermission
import java.io.IOException
import java.net.SocketException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceScannerTest {
    private val wifi = LocalNetwork(network = null, transport = LocalNetwork.Transport.WIFI)

    private class FakeSource(
        override val method: DiscoveryMethod,
        private val body: suspend kotlinx.coroutines.flow.FlowCollector<DiscoveredDevice>.() -> Unit,
    ) : DiscoverySource {
        var calls = 0

        override fun discover(network: LocalNetwork): Flow<DiscoveredDevice> {
            calls++
            return flow { body() }
        }
    }

    private fun scanner(
        vararg sources: DiscoverySource,
        network: LocalNetwork? = wifi,
        permission: LocalNetworkPermission = LocalNetworkPermission.NOT_REQUIRED,
    ) = DeviceScanner(
        networkMonitor = { network },
        localNetworkAccess = object : LocalNetworkAccess {
            override fun status() = permission
        },
        sources = sources.toSet(),
        log = DiagnosticsLog(),
        timeoutMillis = 7_000,
    )

    private fun device(host: String, platform: Platform, method: DiscoveryMethod, name: String? = null) =
        DiscoveredDevice(host, platform, name = name, methods = setOf(method))

    @Test
    fun `sin Wi-Fi no busca y lo dice`() = runTest {
        val ssdp = FakeSource(DiscoveryMethod.SSDP) { error("no debería buscar") }
        val states = scanner(ssdp, network = null).scan().toList()
        assertEquals(listOf(ScanState.NoWifi), states)
        assertEquals(0, ssdp.calls)
    }

    @Test
    fun `sin permiso de red local no busca y lo pide`() = runTest {
        val ssdp = FakeSource(DiscoveryMethod.SSDP) { error("no debería buscar") }
        val states = scanner(ssdp, permission = LocalNetworkPermission.DENIED).scan().toList()
        assertEquals(listOf(ScanState.PermissionRequired), states)
        assertEquals(0, ssdp.calls)
    }

    @Test
    fun `junta las dos vias y la lista crece mientras llegan respuestas`() = runTest {
        val ssdp = FakeSource(DiscoveryMethod.SSDP) {
            emit(device("192.168.1.10", Platform.ROKU, DiscoveryMethod.SSDP))
            delay(100)
            emit(device("192.168.1.20", Platform.ANDROID_TV, DiscoveryMethod.SSDP))
        }
        val mdns = FakeSource(DiscoveryMethod.MDNS) {
            delay(200)
            emit(device("192.168.1.20", Platform.ANDROID_TV, DiscoveryMethod.MDNS, name = "Sala"))
            awaitCancellation() // NsdManager no termina solo: lo corta el tiempo máximo.
        }

        val states = scanner(ssdp, mdns).scan().toList()

        assertEquals(ScanState.Scanning(emptyList()), states.first())
        val final = states.last() as ScanState.Finished
        assertEquals(listOf("192.168.1.10", "192.168.1.20"), final.devices.map { it.host })
        assertEquals("Sala", final.devices[1].name)
        assertTrue(states.any { it is ScanState.Scanning && it.devices.size == 1 })
    }

    @Test
    fun `si una via falla, la otra sigue y se muestran sus resultados`() = runTest {
        val ssdp = FakeSource(DiscoveryMethod.SSDP) { throw IOException("socket roto") }
        val mdns = FakeSource(DiscoveryMethod.MDNS) {
            emit(device("192.168.1.20", Platform.ANDROID_TV, DiscoveryMethod.MDNS))
        }
        val final = scanner(ssdp, mdns).scan().toList().last()
        assertEquals(1, (final as ScanState.Finished).devices.size)
    }

    @Test
    fun `EPERM sin resultados es permiso negado, no tele no encontrada`() = runTest {
        val ssdp = FakeSource(DiscoveryMethod.SSDP) { throw SocketException("sendto failed: EPERM") }
        val mdns = FakeSource(DiscoveryMethod.MDNS) { awaitCancellation() }
        val final = scanner(ssdp, mdns).scan().toList().last()
        assertEquals(ScanState.Failed(FailureReason.LOCAL_NETWORK_PERMISSION_DENIED), final)
    }

    @Test
    fun `si fallan todas las vias se informa el motivo`() = runTest {
        val ssdp = FakeSource(DiscoveryMethod.SSDP) { throw IOException("x") }
        val mdns = FakeSource(DiscoveryMethod.MDNS) { throw IOException("y") }
        val final = scanner(ssdp, mdns).scan().toList().last()
        assertEquals(ScanState.Failed(FailureReason.DEVICE_UNREACHABLE), final)
    }

    @Test
    fun `sin respuestas termina con la lista vacia`() = runTest {
        val ssdp = FakeSource(DiscoveryMethod.SSDP) { delay(5_000) }
        val mdns = FakeSource(DiscoveryMethod.MDNS) { awaitCancellation() }
        val states = scanner(ssdp, mdns).scan().toList()
        assertEquals(ScanState.Finished(emptyList()), states.last())
        assertFalse(states.any { it is ScanState.Failed })
    }
}
