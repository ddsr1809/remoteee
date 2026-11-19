package app.remoto.control.core.net

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalNetworkAccessTest {
    @Test
    fun `con targetSdk 36 nunca se pide aunque el telefono tenga Android 17`() {
        assertEquals(LocalNetworkPermission.NOT_REQUIRED, LocalNetworkAccess.statusFor(37, 36, granted = false))
    }

    @Test
    fun `en Android 16 no se pide aunque la app apunte a 37`() {
        assertEquals(LocalNetworkPermission.NOT_REQUIRED, LocalNetworkAccess.statusFor(36, 37, granted = false))
    }

    @Test
    fun `con Android 17 y targetSdk 37 depende de si se concedio`() {
        assertEquals(LocalNetworkPermission.GRANTED, LocalNetworkAccess.statusFor(37, 37, granted = true))
        assertEquals(LocalNetworkPermission.DENIED, LocalNetworkAccess.statusFor(37, 37, granted = false))
    }
}
