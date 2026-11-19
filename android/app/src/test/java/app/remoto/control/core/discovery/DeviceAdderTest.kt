package app.remoto.control.core.discovery

import app.remoto.control.core.diagnostics.DiagnosticsLog
import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.testutil.InMemoryDeviceRepository
import app.remoto.control.testutil.InMemorySettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceAdderTest {
    private val devices = InMemoryDeviceRepository()
    private val settings = InMemorySettingsRepository()
    private var nextId = 0
    private val adder = DeviceAdder(devices, settings, DiagnosticsLog(), newId = { "id${++nextId}" })

    @Test
    fun `guarda una tele nueva con el nombre que anuncia y termina la bienvenida`() = runTest {
        val found = DiscoveredDevice("192.168.1.20", Platform.ANDROID_TV, name = " Sala ", uniqueId = "u1")
        val saved = adder.add(found)!!

        assertEquals(TvDevice("id1", "Sala", Platform.ANDROID_TV, "192.168.1.20", "u1"), saved)
        assertEquals(listOf(saved), devices.current.value)
        assertTrue(settings.current.value.onboardingDone)
    }

    @Test
    fun `si ya estaba guardada actualiza la IP y conserva el nombre del usuario`() = runTest {
        devices.upsert(TvDevice("old", "Cuarto", Platform.SAMSUNG, "192.168.1.10", "u1"))

        val saved = adder.add(DiscoveredDevice("192.168.1.99", Platform.SAMSUNG, uniqueId = "u1"))!!

        assertEquals(TvDevice("old", "Cuarto", Platform.SAMSUNG, "192.168.1.99", "u1"), saved)
        assertEquals(1, devices.current.value.size)
    }

    @Test
    fun `una tele agregada a mano gana el identificador cuando aparece en la busqueda`() = runTest {
        val manual = adder.add(Platform.ROKU, "192.168.1.30")
        val found = adder.add(DiscoveredDevice("192.168.1.30", Platform.ROKU, uniqueId = "roku:ecp:x"))!!

        assertEquals(manual.id, found.id)
        assertEquals("roku:ecp:x", found.uniqueId)
        assertEquals(1, devices.current.value.size)
    }

    @Test
    fun `sin plataforma no se guarda`() = runTest {
        assertNull(adder.add(DiscoveredDevice("192.168.1.40", null)))
        assertTrue(devices.current.value.isEmpty())
    }
}
