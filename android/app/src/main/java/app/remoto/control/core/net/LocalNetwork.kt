package app.remoto.control.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La red local por la que se busca. [network] sirve para atar los sockets a esa red y no
 * a los datos móviles o a una VPN. Es `null` en las pruebas.
 */
data class LocalNetwork(val network: Network?, val transport: Transport) {
    enum class Transport { WIFI, ETHERNET }
}

/** Dice si el teléfono está en una red local (Wi-Fi o cable) donde pueda haber una tele. */
fun interface LocalNetworkMonitor {
    fun current(): LocalNetwork?
}

@Singleton
class AndroidLocalNetworkMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LocalNetworkMonitor {
    /**
     * Busca entre todas las redes una de Wi-Fi o Ethernet que no sea una VPN. Se miran todas
     * (no solo la activa) porque con una VPN encendida la red activa es la VPN, y la
     * multidifusión no pasa por ella.
     */
    @Suppress("DEPRECATION") // allNetworks: no hay reemplazo síncrono; sigue funcionando.
    override fun current(): LocalNetwork? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val candidates = listOfNotNull(cm.activeNetwork) + cm.allNetworks.toList()
        for (network in candidates.distinct()) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                    return LocalNetwork(network, LocalNetwork.Transport.WIFI)
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                    return LocalNetwork(network, LocalNetwork.Transport.ETHERNET)
            }
        }
        return null
    }
}
