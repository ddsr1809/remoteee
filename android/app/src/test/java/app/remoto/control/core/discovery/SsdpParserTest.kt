package app.remoto.control.core.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpParserTest {
    private fun packet(vararg lines: String) = lines.joinToString("\r\n", postfix = "\r\n\r\n")

    @Test
    fun `interpreta la respuesta de un Roku`() {
        val msg = SsdpParser.parse(
            packet(
                "HTTP/1.1 200 OK",
                "Cache-Control: max-age=3600",
                "ST: roku:ecp",
                "USN: uuid:roku:ecp:P0A070000007",
                "Ext: ",
                "Server: Roku/9.3.0 UPnP/1.0 Roku/9.3.0",
                "LOCATION: http://192.168.1.134:8060/",
            ),
        )!!
        assertEquals(SsdpMessage.Kind.SEARCH_RESPONSE, msg.kind)
        assertEquals("roku:ecp", msg.searchTarget)
        assertEquals("http://192.168.1.134:8060/", msg.location)
        assertEquals("Roku/9.3.0 UPnP/1.0 Roku/9.3.0", msg.server)
        assertEquals("roku:ecp:p0a070000007", msg.uniqueId)
        assertEquals("", msg.headers["EXT"])
    }

    @Test
    fun `extrae el uuid del USN de Samsung`() {
        val msg = SsdpParser.parse(
            packet(
                "HTTP/1.1 200 OK",
                "ST: urn:samsung.com:device:RemoteControlReceiver:1",
                "USN: uuid:2007E9E6-2EC1-F097-F2DF-944770EA00A3::urn:samsung.com:device:RemoteControlReceiver:1",
                "SERVER: SHP, UPnP/1.0, Samsung UPnP SDK/1.0",
            ),
        )!!
        assertEquals("2007e9e6-2ec1-f097-f2df-944770ea00a3", msg.uniqueId)
    }

    @Test
    fun `las cabeceras no distinguen mayusculas y acepta saltos de linea LF`() {
        val msg = SsdpParser.parse("http/1.1 200 OK\nst: roku:ecp\nlocation: http://10.0.0.5:8060/\n\n")!!
        assertEquals("roku:ecp", msg.searchTarget)
        assertEquals("http://10.0.0.5:8060/", msg.location)
    }

    @Test
    fun `si una cabecera se repite gana la primera`() {
        val msg = SsdpParser.parse(packet("HTTP/1.1 200 OK", "ST: roku:ecp", "ST: otra"))!!
        assertEquals("roku:ecp", msg.searchTarget)
    }

    @Test
    fun `ignora lo que hay despues de la linea en blanco`() {
        val msg = SsdpParser.parse("HTTP/1.1 200 OK\r\nST: roku:ecp\r\n\r\nUSN: cuerpo\r\n")!!
        assertNull(msg.usn)
    }

    @Test
    fun `interpreta anuncios NOTIFY alive y byebye`() {
        val alive = SsdpParser.parse(
            packet("NOTIFY * HTTP/1.1", "NT: roku:ecp", "NTS: ssdp:alive", "USN: uuid:roku:ecp:X1"),
        )!!
        assertEquals(SsdpMessage.Kind.NOTIFY_ALIVE, alive.kind)
        assertEquals("roku:ecp", alive.searchTarget)

        val bye = SsdpParser.parse(packet("NOTIFY * HTTP/1.1", "NT: roku:ecp", "NTS: ssdp:byebye"))!!
        assertEquals(SsdpMessage.Kind.NOTIFY_BYEBYE, bye.kind)
    }

    @Test
    fun `descarta busquedas de otros equipos, errores y basura`() {
        assertNull(SsdpParser.parse(Ssdp.mSearch("ssdp:all")))
        assertNull(SsdpParser.parse(packet("HTTP/1.1 404 Not Found", "ST: roku:ecp")))
        assertNull(SsdpParser.parse("hola"))
        assertNull(SsdpParser.parse(""))
    }

    @Test
    fun `descarta paquetes con demasiadas cabeceras`() {
        val lines = listOf("HTTP/1.1 200 OK") + (1..100).map { "X-$it: v" }
        assertNull(SsdpParser.parse(packet(*lines.toTypedArray())))
    }

    @Test
    fun `sin USN no hay identificador`() {
        val msg = SsdpParser.parse(packet("HTTP/1.1 200 OK", "ST: roku:ecp", "USN:   "))!!
        assertNull(msg.uniqueId)
    }

    @Test
    fun `el M-SEARCH tiene el formato de UPnP`() {
        val text = Ssdp.mSearch("roku:ecp", mxSeconds = 3)
        assertTrue(text.startsWith("M-SEARCH * HTTP/1.1\r\n"))
        assertTrue("HOST: 239.255.255.250:1900\r\n" in text)
        assertTrue("MAN: \"ssdp:discover\"\r\n" in text)
        assertTrue("MX: 3\r\n" in text)
        assertTrue("ST: roku:ecp\r\n" in text)
        assertTrue(text.endsWith("\r\n\r\n"))
    }
}
