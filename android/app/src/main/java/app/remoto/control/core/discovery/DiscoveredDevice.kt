package app.remoto.control.core.discovery

import app.remoto.control.core.driver.FailureReason
import app.remoto.control.core.driver.Platform

/** Por qué vía apareció un dispositivo. */
enum class DiscoveryMethod { SSDP, MDNS }

/**
 * Un dispositivo encontrado en la red, todavía sin guardar.
 *
 * @property host IPv4 desde la que respondió.
 * @property platform plataforma deducida de la respuesta; `null` si no se reconoce.
 * @property name nombre que anuncia la tele, si lo anuncia (mDNS sí; SSDP no lo trae en la respuesta).
 * @property uniqueId identificador estable que anuncia la tele (por ejemplo, el `uuid` del USN de SSDP).
 */
data class DiscoveredDevice(
    val host: String,
    val platform: Platform?,
    val name: String? = null,
    val uniqueId: String? = null,
    val methods: Set<DiscoveryMethod> = emptySet(),
)

/** Lo que ve la pantalla de búsqueda. */
sealed interface ScanState {
    data object Idle : ScanState

    /** El teléfono no está en una red Wi-Fi (o Ethernet): no tiene sentido buscar. */
    data object NoWifi : ScanState

    /**
     * Hace falta el permiso de red local (Android 17 con `targetSdk` 37). Con `targetSdk` 36
     * nunca se llega aquí; la capa queda preparada (ver `LocalNetworkAccess`).
     */
    data object PermissionRequired : ScanState

    data class Scanning(val devices: List<DiscoveredDevice>) : ScanState

    data class Finished(val devices: List<DiscoveredDevice>) : ScanState

    /** Ninguna vía pudo buscar. [reason] distingue "permiso negado" de otros fallos. */
    data class Failed(val reason: FailureReason) : ScanState
}

val ScanState.devices: List<DiscoveredDevice>
    get() = when (this) {
        is ScanState.Scanning -> devices
        is ScanState.Finished -> devices
        else -> emptyList()
    }
