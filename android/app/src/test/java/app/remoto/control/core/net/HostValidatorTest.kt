package app.remoto.control.core.net

import app.remoto.control.core.net.HostValidator.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostValidatorTest {
    @Test
    fun `acepta IPv4 normales`() {
        listOf("192.168.1.20", "10.0.0.5", " 172.16.4.1 ", "8.8.8.8").forEach {
            assertEquals(it, Result.VALID, HostValidator.validateIpv4(it))
        }
    }

    @Test
    fun `rechaza formatos invalidos`() {
        listOf("192.168.1", "192.168.1.256", "192.168.1.-1", "a.b.c.d", "192.168..1", "1.2.3.4.5", "1234.1.1.1")
            .forEach { assertEquals(it, Result.INVALID_FORMAT, HostValidator.validateIpv4(it)) }
        assertEquals(Result.EMPTY, HostValidator.validateIpv4("  "))
    }

    @Test
    fun `rechaza direcciones que nunca son una tele`() {
        listOf("0.0.0.0", "127.0.0.1", "224.0.0.1", "239.255.255.250", "255.255.255.255")
            .forEach { assertEquals(it, Result.NOT_A_DEVICE_ADDRESS, HostValidator.validateIpv4(it)) }
    }

    @Test
    fun `normaliza ceros a la izquierda y espacios`() {
        assertEquals("192.168.1.5", HostValidator.normalizeIpv4(" 192.168.001.005 "))
        assertNull(HostValidator.normalizeIpv4("tele.local"))
    }

    @Test
    fun `reconoce redes privadas`() {
        assertTrue(HostValidator.isPrivateIpv4("192.168.0.1"))
        assertTrue(HostValidator.isPrivateIpv4("172.31.0.1"))
        assertTrue(HostValidator.isPrivateIpv4("10.1.2.3"))
        assertFalse(HostValidator.isPrivateIpv4("172.32.0.1"))
        assertFalse(HostValidator.isPrivateIpv4("8.8.8.8"))
    }
}
