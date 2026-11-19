package app.remoto.control.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsLogTest {
    private var now = 0L
    private val log = DiagnosticsLog(capacity = 3, clock = { now })

    @Test
    fun `todo lo que entra se redacta`() {
        log.log("ssdp", "Respuesta de 192.168.1.34")
        assertEquals("Respuesta de 192.168.x.x", log.snapshot().single().message)
    }

    @Test
    fun `conserva solo las ultimas entradas`() {
        (1..5).forEach { log.log("t", "m$it") }
        assertEquals(listOf("m3", "m4", "m5"), log.snapshot().map { it.message })
    }

    @Test
    fun `las excepciones se registran con tipo y mensaje`() {
        log.log("scan", "Falló SSDP", java.net.SocketException("EPERM en 10.0.0.9"))
        assertEquals("Falló SSDP: SocketException: EPERM en 10.0.x.x", log.snapshot().single().message)
    }

    @Test
    fun `exporta con cabecera, etiqueta y sin IP completas`() {
        log.log("ssdp", "desde 192.168.1.2")
        val text = log.export(header = listOf("Remoto 0.2.0", "IP 192.168.1.3"))
        val lines = text.lines()
        assertEquals("Remoto 0.2.0", lines[0])
        assertEquals("IP 192.168.x.x", lines[1])
        assertEquals("---", lines[2])
        assertTrue(lines[3].endsWith("[ssdp] desde 192.168.x.x"))
        assertFalse("192.168.1." in text)
    }

    @Test
    fun `clear vacia el registro`() {
        log.log("t", "m")
        log.clear()
        assertTrue(log.snapshot().isEmpty())
    }
}
