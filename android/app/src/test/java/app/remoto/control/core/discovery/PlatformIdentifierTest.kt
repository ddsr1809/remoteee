package app.remoto.control.core.discovery

import app.remoto.control.core.driver.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlatformIdentifierTest {
    private fun ssdp(vararg headers: Pair<String, String>) =
        SsdpMessage(SsdpMessage.Kind.SEARCH_RESPONSE, headers.toMap())

    @Test
    fun `identifica cada plataforma por el tipo de servicio`() {
        assertEquals(Platform.ROKU, PlatformIdentifier.fromSsdp(ssdp("ST" to "roku:ecp")))
        assertEquals(
            Platform.SAMSUNG,
            PlatformIdentifier.fromSsdp(ssdp("ST" to "urn:samsung.com:device:RemoteControlReceiver:1")),
        )
        assertEquals(
            Platform.LG,
            PlatformIdentifier.fromSsdp(ssdp("ST" to "urn:lge-com:service:webos-second-screen:1")),
        )
    }

    @Test
    fun `usa el NT de los anuncios y el USN`() {
        assertEquals(Platform.ROKU, PlatformIdentifier.fromSsdp(ssdp("NT" to "roku:ecp")))
        assertEquals(Platform.ROKU, PlatformIdentifier.fromSsdp(ssdp("USN" to "uuid:roku:ecp:X1")))
    }

    @Test
    fun `si el tipo no dice nada mira la cabecera SERVER`() {
        assertEquals(Platform.LG, PlatformIdentifier.fromSsdp(ssdp("SERVER" to "WebOS/4.1.0 UPnP/1.0")))
        assertEquals(
            Platform.SAMSUNG,
            PlatformIdentifier.fromSsdp(ssdp("SERVER" to "SHP, UPnP/1.0, Samsung UPnP SDK/1.0")),
        )
        assertEquals(Platform.ROKU, PlatformIdentifier.fromSsdp(ssdp("SERVER" to "Roku/9.3.0 UPnP/1.0")))
    }

    @Test
    fun `el tipo de servicio manda sobre SERVER`() {
        val msg = ssdp("ST" to "roku:ecp", "SERVER" to "Linux webOS")
        assertEquals(Platform.ROKU, PlatformIdentifier.fromSsdp(msg))
    }

    @Test
    fun `un router u otro aparato no se identifica`() {
        val router = ssdp("ST" to "upnp:rootdevice", "SERVER" to "Linux/3.14 UPnP/1.0 MiniUPnPd/2.1")
        assertNull(PlatformIdentifier.fromSsdp(router))
    }

    @Test
    fun `reconoce el servicio mDNS de Android TV en sus distintas formas`() {
        listOf(
            "_androidtvremote2._tcp",
            "_androidtvremote2._tcp.",
            "._androidtvremote2._tcp",
            "_androidtvremote2._tcp.local.",
            "_AndroidTvRemote2._TCP.",
        ).forEach { assertEquals(it, Platform.ANDROID_TV, PlatformIdentifier.fromMdnsServiceType(it)) }
        assertNull(PlatformIdentifier.fromMdnsServiceType("_googlecast._tcp."))
    }
}
