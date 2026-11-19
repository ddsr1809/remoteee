package app.remoto.control.core.driver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateTest {
    @Test
    fun `cada motivo de fallo tiene su tema de ayuda`() {
        assertEquals(HelpTopic.CONNECTION_TIMEOUT, HelpTopic.forFailure(FailureReason.TIMEOUT))
        assertEquals(HelpTopic.PAIRING_REJECTED, HelpTopic.forFailure(FailureReason.PAIRING_REJECTED))
        assertEquals(
            HelpTopic.LOCAL_NETWORK_PERMISSION,
            HelpTopic.forFailure(FailureReason.LOCAL_NETWORK_PERMISSION_DENIED),
        )
        assertEquals(HelpTopic.NO_WIFI, HelpTopic.forFailure(FailureReason.NETWORK_UNAVAILABLE))
        assertEquals(HelpTopic.DEVICE_UNREACHABLE, HelpTopic.forFailure(FailureReason.DEVICE_UNREACHABLE))
        // Ningún motivo se queda sin ayuda.
        FailureReason.entries.forEach { HelpTopic.forFailure(it) }
    }

    @Test
    fun `Failed of incluye el tema de ayuda del motivo`() {
        val failed = ConnectionState.Failed.of(FailureReason.TIMEOUT)
        assertEquals(FailureReason.TIMEOUT, failed.reason)
        assertEquals(HelpTopic.CONNECTION_TIMEOUT, failed.help)
    }

    @Test
    fun `solo se envian ordenes estando conectado`() {
        assertTrue(ConnectionState.Connected.canSendCommands)
        assertFalse(ConnectionState.Disconnected.canSendCommands)
        assertFalse(ConnectionState.Connecting.canSendCommands)
        assertFalse(ConnectionState.PairingRequired(PairingType.CODE_ON_TV).canSendCommands)
        assertFalse(ConnectionState.Failed.of(FailureReason.UNKNOWN).canSendCommands)
    }

    @Test
    fun `se puede conectar desde desconectado o fallido, nunca a mitad de conexion`() {
        assertTrue(ConnectionState.Disconnected.canConnect)
        assertTrue(ConnectionState.Failed.of(FailureReason.TIMEOUT).canConnect)
        assertFalse(ConnectionState.Connecting.canConnect)
        assertFalse(ConnectionState.Connected.canConnect)
        assertFalse(ConnectionState.PairingRequired(PairingType.CONFIRM_ON_TV).canConnect)
    }

    @Test
    fun `las teclas que se repiten son volumen, canales y flechas`() {
        val repeatable = RemoteKey.entries.filter { it.repeatable }.toSet()
        assertEquals(
            setOf(
                RemoteKey.UP, RemoteKey.DOWN, RemoteKey.LEFT, RemoteKey.RIGHT,
                RemoteKey.VOLUME_UP, RemoteKey.VOLUME_DOWN, RemoteKey.CHANNEL_UP, RemoteKey.CHANNEL_DOWN,
            ),
            repeatable,
        )
    }
}
