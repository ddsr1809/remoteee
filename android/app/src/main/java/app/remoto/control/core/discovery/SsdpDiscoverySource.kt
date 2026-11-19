package app.remoto.control.core.discovery

import app.remoto.control.core.diagnostics.DiagnosticsLog
import app.remoto.control.core.net.LocalNetwork
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Búsqueda SSDP: manda un `M-SEARCH` por cada plataforma y escucha las respuestas durante
 * [durationMillis]. El bloqueo de multidifusión se toma al empezar y se suelta en `finally`,
 * también si falla o se cancela.
 */
class SsdpDiscoverySource(
    private val locks: MulticastLockProvider,
    private val log: DiagnosticsLog,
    private val durationMillis: Long,
) : DiscoverySource {
    @Inject
    constructor(locks: MulticastLockProvider, log: DiagnosticsLog) : this(locks, log, DEFAULT_DURATION_MS)

    override val method = DiscoveryMethod.SSDP

    override fun discover(network: LocalNetwork): Flow<DiscoveredDevice> = flow {
        locks.withLock {
            MulticastSocket(null as InetSocketAddress?).use { socket ->
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(0))
                // Atar el socket a la Wi-Fi: con datos móviles o VPN, el paquete podría salir por otra red.
                network.network?.bindSocket(socket)
                socket.timeToLive = MULTICAST_TTL
                socket.soTimeout = RECEIVE_TIMEOUT_MS

                val group = InetAddress.getByName(Ssdp.MULTICAST_ADDRESS)
                val deadline = System.currentTimeMillis() + durationMillis
                var nextRound = 0L
                var rounds = 0
                val buffer = ByteArray(BUFFER_SIZE)
                log.log(TAG, "Búsqueda iniciada (${Ssdp.SEARCH_TARGETS.size} tipos)")

                while (System.currentTimeMillis() < deadline) {
                    currentCoroutineContext().ensureActive()
                    // UDP puede perder paquetes: se repite el M-SEARCH un par de veces.
                    if (rounds < SEARCH_ROUNDS && System.currentTimeMillis() >= nextRound) {
                        Ssdp.SEARCH_TARGETS.forEach { st ->
                            val bytes = Ssdp.mSearch(st).toByteArray(Charsets.US_ASCII)
                            socket.send(DatagramPacket(bytes, bytes.size, group, Ssdp.PORT))
                        }
                        rounds++
                        nextRound = System.currentTimeMillis() + ROUND_INTERVAL_MS
                    }
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (e: SocketTimeoutException) {
                        continue
                    }
                    val host = packet.address?.hostAddress ?: continue
                    if (':' in host) continue // Solo IPv4.
                    val text = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                    val message = SsdpParser.parse(text) ?: continue
                    if (message.kind == SsdpMessage.Kind.NOTIFY_BYEBYE) continue
                    val platform = PlatformIdentifier.fromSsdp(message)
                    val label = platform?.name ?: "plataforma desconocida"
                    log.log(TAG, "Respuesta de $host: $label, ST=${message.searchTarget}")
                    emit(
                        DiscoveredDevice(
                            host = host,
                            platform = platform,
                            uniqueId = message.uniqueId,
                            methods = setOf(DiscoveryMethod.SSDP),
                        ),
                    )
                }
                log.log(TAG, "Búsqueda terminada")
            }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val TAG = "ssdp"
        const val DEFAULT_DURATION_MS = 5_000L
        const val RECEIVE_TIMEOUT_MS = 400
        const val ROUND_INTERVAL_MS = 1_200L
        const val SEARCH_ROUNDS = 2

        /** UPnP recomienda TTL 2 para que el M-SEARCH no salga de la red de casa. */
        const val MULTICAST_TTL = 2
        const val BUFFER_SIZE = 2048
    }
}
