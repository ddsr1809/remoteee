package app.remoto.control.core.discovery

import app.remoto.control.core.net.LocalNetwork
import kotlinx.coroutines.flow.Flow

/**
 * Una vía de búsqueda (SSDP, mDNS). Emite lo que encuentra mientras dura y termina sola
 * a los pocos segundos; el escáner también la corta con un tiempo máximo.
 *
 * Los errores se lanzan desde el flujo: el escáner los clasifica con [NetworkErrors].
 */
interface DiscoverySource {
    val method: DiscoveryMethod

    fun discover(network: LocalNetwork): Flow<DiscoveredDevice>
}
