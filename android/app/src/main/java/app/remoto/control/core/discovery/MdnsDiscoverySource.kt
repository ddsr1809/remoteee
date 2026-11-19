package app.remoto.control.core.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import app.remoto.control.core.diagnostics.DiagnosticsLog
import app.remoto.control.core.net.LocalNetwork
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Búsqueda mDNS (DNS-SD) con `NsdManager`, que ya viene en Android y no necesita el
 * bloqueo de multidifusión. Hoy solo busca Android TV / Google TV (`_androidtvremote2._tcp`):
 * Samsung, LG y Roku se encuentran por SSDP.
 *
 * `NsdManager` entrega primero el nombre del servicio y luego hay que "resolverlo" para saber
 * su IP. En Android 13 y anteriores solo admite una resolución a la vez, así que se resuelven
 * en fila.
 */
class MdnsDiscoverySource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val log: DiagnosticsLog,
) : DiscoverySource {
    override val method = DiscoveryMethod.MDNS

    private val serviceTypes = listOf(PlatformIdentifier.ANDROID_TV_REMOTE_SERVICE)

    override fun discover(network: LocalNetwork): Flow<DiscoveredDevice> = callbackFlow {
        val nsd = context.getSystemService(NsdManager::class.java)
            ?: throw IOException("NsdManager no disponible")
        val found = Channel<NsdServiceInfo>(Channel.UNLIMITED)

        val listeners = serviceTypes.map { type ->
            object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {
                    log.log(TAG, "Búsqueda iniciada: $serviceType")
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    found.trySend(serviceInfo)
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

                override fun onDiscoveryStopped(serviceType: String) = Unit

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    log.log(TAG, "No se pudo iniciar $serviceType (código $errorCode)")
                    close(IOException("mDNS: discoverServices falló con código $errorCode"))
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            }.also { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, it) }
        }

        launch {
            for (info in found) {
                val device = withTimeoutOrNull(RESOLVE_TIMEOUT_MS) { resolve(nsd, info) } ?: continue
                send(device)
            }
        }

        awaitClose {
            listeners.forEach { runCatching { nsd.stopServiceDiscovery(it) } }
            found.close()
        }
    }

    @Suppress("DEPRECATION") // resolveService: el reemplazo (registerServiceInfoCallback) es solo API 34+.
    private suspend fun resolve(nsd: NsdManager, info: NsdServiceInfo): DiscoveredDevice? =
        suspendCancellableCoroutine { cont ->
            val listener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    log.log(TAG, "No se pudo resolver un servicio (código $errorCode)")
                    if (cont.isActive) cont.resume(null)
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    if (cont.isActive) cont.resume(toDevice(serviceInfo, foundType = info.serviceType.orEmpty()))
                }
            }
            nsd.resolveService(info, listener)
            // Si se agota el tiempo, se cancela la resolución para no bloquear la siguiente.
            // stopServiceResolution solo existe desde API 34; antes, la siguiente puede fallar y se ignora.
            if (Build.VERSION.SDK_INT >= 34) {
                cont.invokeOnCancellation { runCatching { nsd.stopServiceResolution(listener) } }
            }
        }

    /** [foundType] es el tipo con el que se encontró: algunas versiones lo devuelven alterado al resolver. */
    private fun toDevice(info: NsdServiceInfo, foundType: String): DiscoveredDevice? {
        val host = ipv4Of(info)?.hostAddress ?: return null
        val platform = PlatformIdentifier.fromMdnsServiceType(foundType)
            ?: PlatformIdentifier.fromMdnsServiceType(info.serviceType.orEmpty())
        log.log(TAG, "Encontrado en $host: plataforma=${platform ?: "desconocida"}")
        return DiscoveredDevice(
            host = host,
            platform = platform,
            name = info.serviceName?.let { DnsSd.unescapeName(it) }?.takeIf { it.isNotBlank() },
            methods = setOf(DiscoveryMethod.MDNS),
        )
    }

    @Suppress("DEPRECATION") // NsdServiceInfo.host: obsoleto en API 34, sigue siendo lo único antes de 34.
    private fun ipv4Of(info: NsdServiceInfo): InetAddress? {
        val addresses = if (Build.VERSION.SDK_INT >= 34) info.hostAddresses else listOfNotNull(info.host)
        return addresses.firstOrNull { it is Inet4Address }
    }

    private companion object {
        const val TAG = "mdns"
        const val RESOLVE_TIMEOUT_MS = 3_000L
    }
}
