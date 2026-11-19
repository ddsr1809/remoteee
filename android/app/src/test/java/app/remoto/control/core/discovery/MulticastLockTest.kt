package app.remoto.control.core.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MulticastLockTest {
    private class FakeLock : MulticastLockHandle {
        var acquired = 0
        var released = 0
        override var isHeld = false
            private set

        override fun acquire() {
            acquired++
            isHeld = true
        }

        override fun release() {
            released++
            isHeld = false
        }
    }

    private val lock = FakeLock()
    private val provider = MulticastLockProvider { lock }

    @Test
    fun `toma el bloqueo durante la busqueda y lo suelta al terminar`() {
        val result = provider.withLock {
            assertTrue(lock.isHeld)
            42
        }
        assertEquals(42, result)
        assertFalse(lock.isHeld)
        assertEquals(1, lock.acquired)
        assertEquals(1, lock.released)
    }

    @Test
    fun `lo suelta aunque la busqueda falle`() {
        try {
            provider.withLock { throw java.net.SocketException("EPERM") }
            fail("Debía lanzar")
        } catch (e: java.net.SocketException) {
            // esperado
        }
        assertFalse(lock.isHeld)
        assertEquals(1, lock.released)
    }
}
