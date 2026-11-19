package app.remoto.control.core.discovery

import android.content.Context
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** [MulticastLockProvider] real. Necesita el permiso `CHANGE_WIFI_MULTICAST_STATE`. */
@Singleton
class AndroidMulticastLockProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : MulticastLockProvider {
    override fun create(): MulticastLockHandle {
        val wifi = context.getSystemService(WifiManager::class.java)
        val lock = wifi?.createMulticastLock(TAG)?.apply { setReferenceCounted(false) }
        return object : MulticastLockHandle {
            override fun acquire() {
                lock?.acquire()
            }

            override fun release() {
                lock?.release()
            }

            override val isHeld: Boolean get() = lock?.isHeld == true
        }
    }

    private companion object {
        const val TAG = "remoto:ssdp"
    }
}
