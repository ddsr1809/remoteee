package app.remoto.control.core.discovery

import app.remoto.control.core.driver.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryMergerTest {
    private val merger = DiscoveryMerger()

    private fun ssdp(host: String, platform: Platform?, uuid: String? = null) =
        DiscoveredDevice(host, platform, uniqueId = uuid, methods = setOf(DiscoveryMethod.SSDP))

    private fun mdns(host: String, platform: Platform?, name: String? = null) =
        DiscoveredDevice(host, platform, name = name, methods = setOf(DiscoveryMethod.MDNS))

    @Test
    fun `la misma tele por SSDP y por mDNS aparece una sola vez con todo junto`() {
        merger.add(ssdp("192.168.1.10", Platform.ANDROID_TV, uuid = "abc"))
        merger.add(mdns("192.168.1.10", Platform.ANDROID_TV, name = "Sala"))

        val only = merger.devices.single()
        assertEquals("192.168.1.10", only.host)
        assertEquals("Sala", only.name)
        assertEquals("abc", only.uniqueId)
        assertEquals(setOf(DiscoveryMethod.SSDP, DiscoveryMethod.MDNS), only.methods)
    }

    @Test
    fun `una respuesta repetida no cambia la lista`() {
        assertTrue(merger.add(ssdp("192.168.1.10", Platform.ROKU)))
        assertFalse(merger.add(ssdp("192.168.1.10", Platform.ROKU)))
        assertEquals(1, merger.devices.size)
    }

    @Test
    fun `teles distintas se conservan en el orden en que llegaron`() {
        merger.add(ssdp("192.168.1.30", Platform.LG))
        merger.add(ssdp("192.168.1.10", Platform.ROKU))
        merger.add(mdns("192.168.1.20", Platform.ANDROID_TV))
        assertEquals(listOf("192.168.1.30", "192.168.1.10", "192.168.1.20"), merger.devices.map { it.host })
    }

    @Test
    fun `el mismo identificador desde dos IP es la misma tele`() {
        merger.add(ssdp("192.168.1.10", Platform.SAMSUNG, uuid = "u1"))
        merger.add(ssdp("192.168.1.11", Platform.SAMSUNG, uuid = "u1"))
        assertEquals(1, merger.devices.size)
        assertEquals("192.168.1.10", merger.devices.single().host)
    }

    @Test
    fun `un resultado que une dos grupos los funde en uno`() {
        merger.add(ssdp("192.168.1.10", Platform.SAMSUNG, uuid = "u1"))
        merger.add(mdns("192.168.1.11", Platform.SAMSUNG, name = "Cuarto"))
        assertEquals(2, merger.devices.size)

        merger.add(ssdp("192.168.1.11", Platform.SAMSUNG, uuid = "u1"))
        val only = merger.devices.single()
        assertEquals("Cuarto", only.name)
        assertEquals("u1", only.uniqueId)
    }

    @Test
    fun `lo que no se reconoce no se muestra hasta que otra via lo identifica`() {
        assertFalse(merger.add(ssdp("192.168.1.50", null)))
        assertTrue(merger.devices.isEmpty())

        assertTrue(merger.add(mdns("192.168.1.50", Platform.ANDROID_TV, name = "Tele")))
        assertEquals(Platform.ANDROID_TV, merger.devices.single().platform)
    }

    @Test
    fun `si las vias no coinciden en la plataforma gana Android TV`() {
        merger.add(ssdp("192.168.1.10", Platform.SAMSUNG))
        merger.add(mdns("192.168.1.10", Platform.ANDROID_TV))
        assertEquals(Platform.ANDROID_TV, merger.devices.single().platform)

        val other = DiscoveryMerger()
        other.add(mdns("192.168.1.10", Platform.ANDROID_TV))
        other.add(ssdp("192.168.1.10", Platform.SAMSUNG))
        assertEquals(Platform.ANDROID_TV, other.devices.single().platform)
    }

    @Test
    fun `en otros conflictos se queda la primera plataforma`() {
        merger.add(ssdp("192.168.1.10", Platform.ROKU))
        merger.add(ssdp("192.168.1.10", Platform.LG))
        assertEquals(Platform.ROKU, merger.devices.single().platform)
    }
}
