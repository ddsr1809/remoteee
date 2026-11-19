package app.remoto.control.core.discovery

import org.junit.Assert.assertEquals
import org.junit.Test

class DnsSdTest {
    @Test
    fun `convierte los escapes decimales en caracteres`() {
        assertEquals("Sala de estar", DnsSd.unescapeName("Sala\\032de\\032estar"))
    }

    @Test
    fun `los escapes pueden formar UTF-8 de varios bytes`() {
        assertEquals("Habitación", DnsSd.unescapeName("Habitaci\\195\\179n"))
    }

    @Test
    fun `una barra antes de otro caracter lo deja tal cual`() {
        assertEquals("a.b\\c", DnsSd.unescapeName("a\\.b\\\\c"))
    }

    @Test
    fun `sin escapes no cambia nada`() {
        assertEquals("Living Room TV", DnsSd.unescapeName("Living Room TV"))
        assertEquals("fin\\", DnsSd.unescapeName("fin\\"))
    }
}
