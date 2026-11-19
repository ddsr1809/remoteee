package app.remoto.control.core.discovery

import app.remoto.control.core.driver.FailureReason
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkErrorsTest {
    @Test
    fun `EPERM es permiso de red local negado`() {
        val e = SocketException("sendto failed: EPERM (Operation not permitted)")
        assertEquals(FailureReason.LOCAL_NETWORK_PERMISSION_DENIED, NetworkErrors.classify(e))
    }

    @Test
    fun `busca EPERM tambien en la causa`() {
        val e = IOException("fallo", SocketException("Operation not permitted"))
        assertEquals(FailureReason.LOCAL_NETWORK_PERMISSION_DENIED, NetworkErrors.classify(e))
    }

    @Test
    fun `red inalcanzable es sin red`() {
        val e = SocketException("sendto failed: ENETUNREACH (Network is unreachable)")
        assertEquals(FailureReason.NETWORK_UNAVAILABLE, NetworkErrors.classify(e))
    }

    @Test
    fun `tiempo agotado y otros errores de red`() {
        assertEquals(FailureReason.TIMEOUT, NetworkErrors.classify(SocketTimeoutException("timeout")))
        assertEquals(FailureReason.DEVICE_UNREACHABLE, NetworkErrors.classify(IOException("x")))
        assertEquals(FailureReason.UNKNOWN, NetworkErrors.classify(IllegalStateException()))
    }
}
