package app.remoto.control.core.discovery

import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.TvDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedDeviceMatcherTest {
    private val samsung = TvDevice("s1", "", Platform.SAMSUNG, host = "192.168.1.10", uniqueId = "u1")
    private val roku = TvDevice("r1", "", Platform.ROKU, host = "192.168.1.20")
    private val saved = listOf(samsung, roku)

    @Test
    fun `reconoce una tele guardada aunque haya cambiado de IP`() {
        assertEquals(samsung, SavedDeviceMatcher.findMatch(saved, Platform.SAMSUNG, "192.168.1.99", "u1"))
    }

    @Test
    fun `sin identificador compara IP y plataforma`() {
        assertEquals(roku, SavedDeviceMatcher.findMatch(saved, Platform.ROKU, "192.168.1.20", null))
        assertNull(SavedDeviceMatcher.findMatch(saved, Platform.LG, "192.168.1.20", null))
    }

    @Test
    fun `misma IP con otro identificador es otra tele`() {
        assertNull(SavedDeviceMatcher.findMatch(saved, Platform.SAMSUNG, "192.168.1.10", "u2"))
    }

    @Test
    fun `una tele agregada a mano se reconoce cuando aparece en la busqueda`() {
        assertEquals(roku, SavedDeviceMatcher.findMatch(saved, Platform.ROKU, "192.168.1.20", "nuevo"))
    }

    @Test
    fun `isSaved necesita plataforma`() {
        assertTrue(SavedDeviceMatcher.isSaved(saved, DiscoveredDevice("192.168.1.20", Platform.ROKU)))
        assertFalse(SavedDeviceMatcher.isSaved(saved, DiscoveredDevice("192.168.1.20", null)))
    }
}
