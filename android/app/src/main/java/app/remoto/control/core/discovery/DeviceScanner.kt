package app.remoto.control.core.discovery

import app.remoto.control.core.diagnostics.DiagnosticsLog
import app.remoto.control.core.driver.FailureReason
import app.remoto.control.core.net.LocalNetworkAccess
import app.remoto.control.core.net.LocalNetworkMonitor
import app.remoto.control.core.net.LocalNetworkPermission
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Busca teles por todas las vías a la vez y publica una lista unificada que crece mientras
 * llegan respuestas.
 *
 * Orden de comprobaciones: primero que haya Wi-Fi (si no, [ScanState.NoWifi] sin buscar),
 * después el permiso de red local, y solo entonces la búsqueda.
 */
class DeviceScanner(
    private val networkMonitor: LocalNetworkMonitor,
    private val localNetworkAccess: LocalNetworkAccess,
    private val sources: Set<DiscoverySource>,
    private val log: DiagnosticsLog,
    private val timeoutMillis: Long,
) {
    @Inject
    constructor(
        networkMonitor: LocalNetworkMonitor,
        localNetworkAccess: LocalNetworkAccess,
        sources: Set<@JvmSuppressWildcards DiscoverySource>,
        log: DiagnosticsLog,
    ) : this(networkMonitor, localNetworkAccess, sources, log, DEFAULT_TIMEOUT_MS)

    private sealed interface SourceEvent {
        data class Found(val device: DiscoveredDevice) : SourceEvent

        data class Failed(val method: DiscoveryMethod, val reason: FailureReason) : SourceEvent
    }

    fun scan(): Flow<ScanState> = channelFlow {
        val network = networkMonitor.current()
        if (network == null) {
            log.log(TAG, "Sin Wi-Fi: no se busca")
            send(ScanState.NoWifi)
            return@channelFlow
        }
        if (localNetworkAccess.status() == LocalNetworkPermission.DENIED) {
            log.log(TAG, "Falta el permiso de red local")
            send(ScanState.PermissionRequired)
            return@channelFlow
        }

        log.log(TAG, "Buscando por ${network.transport} con ${sources.map { it.method }}")
        val merger = DiscoveryMerger()
        val failures = mutableMapOf<DiscoveryMethod, FailureReason>()
        send(ScanState.Scanning(emptyList()))

        // Cada vía corre en su propia corrutina; los eventos se procesan aquí, de uno en uno,
        // así que el merger no necesita sincronización.
        val events = sources.map { source ->
            source.discover(network)
                .map<DiscoveredDevice, SourceEvent> { SourceEvent.Found(it) }
                .catch { e ->
                    if (e is CancellationException) throw e
                    log.log(TAG, "Falló ${source.method}", e)
                    emit(SourceEvent.Failed(source.method, NetworkErrors.classify(e)))
                }
        }.merge()

        withTimeoutOrNull(timeoutMillis) {
            events.collect { event ->
                when (event) {
                    is SourceEvent.Found -> if (merger.add(event.device)) send(ScanState.Scanning(merger.devices))
                    is SourceEvent.Failed -> failures[event.method] = event.reason
                }
            }
        }

        val found = merger.devices
        log.log(TAG, "Fin: ${found.size} dispositivo(s), fallos=${failures.values}")
        send(finalState(found, failures))
    }

    private fun finalState(found: List<DiscoveredDevice>, failures: Map<DiscoveryMethod, FailureReason>): ScanState {
        val allFailed = sources.isNotEmpty() && failures.size == sources.size
        return when {
            found.isNotEmpty() -> ScanState.Finished(found)
            // Si alguna vía chocó con el permiso, eso explica que no haya resultados.
            FailureReason.LOCAL_NETWORK_PERMISSION_DENIED in failures.values ->
                ScanState.Failed(FailureReason.LOCAL_NETWORK_PERMISSION_DENIED)
            allFailed -> ScanState.Failed(failures.values.first())
            else -> ScanState.Finished(emptyList())
        }
    }

    private companion object {
        const val TAG = "scan"

        /** Tope de seguridad. SSDP termina sola en ~5 s; mDNS sigue hasta que se corta. */
        const val DEFAULT_TIMEOUT_MS = 7_000L
    }
}
