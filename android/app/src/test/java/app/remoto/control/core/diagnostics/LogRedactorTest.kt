package app.remoto.control.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LogRedactorTest {
    @Test
    fun `oculta los dos ultimos numeros de cada IPv4`() {
        assertEquals("Respuesta de 192.168.x.x y 10.0.x.x", LogRedactor.redact("Respuesta de 192.168.1.34 y 10.0.0.7"))
    }

    @Test
    fun `oculta IPv6 y MAC`() {
        assertEquals("host [ipv6] ok", LogRedactor.redact("host fe80::1c2b:3aff:fe4d:5e6f%wlan0 ok"))
        assertEquals("host [ipv6]", LogRedactor.redact("host 2001:db8:0:0:0:0:2:1"))
        assertEquals("mac [mac] y [mac]", LogRedactor.redact("mac AA:BB:CC:DD:EE:FF y aa-bb-cc-dd-ee-ff"))
    }

    @Test
    fun `acorta los uuid`() {
        val out = LogRedactor.redact("USN uuid:2007E9E6-2EC1-F097-F2DF-944770EA00A3::urn:x")
        assertEquals("USN uuid:2007…::urn:x", out)
    }

    @Test
    fun `no toca horas, URL de protocolo ni texto normal`() {
        assertEquals("a las 12:30:05 llegó", LogRedactor.redact("a las 12:30:05 llegó"))
        assertEquals("ST=roku:ecp", LogRedactor.redact("ST=roku:ecp"))
        assertEquals(
            "ST=urn:samsung.com:device:RemoteControlReceiver:1",
            LogRedactor.redact("ST=urn:samsung.com:device:RemoteControlReceiver:1"),
        )
        assertFalse("[ipv6]" in LogRedactor.redact("Búsqueda iniciada (3 tipos)"))
    }
}
