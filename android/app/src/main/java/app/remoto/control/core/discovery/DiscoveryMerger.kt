package app.remoto.control.core.discovery

import app.remoto.control.core.driver.Platform

/**
 * Junta lo que llega por SSDP y por mDNS en una sola lista: un dispositivo aparece una vez
 * aunque responda por las dos vías o varias veces por la misma.
 *
 * Dos resultados son el mismo dispositivo si comparten IP **o** identificador único.
 * Lo segundo cubre una tele que contesta desde dos interfaces (Wi-Fi y cable).
 *
 * No es seguro entre hilos: el escáner lo usa desde una sola corrutina.
 */
class DiscoveryMerger {
    private val groups = mutableListOf<DiscoveredDevice>()

    /** Dispositivos con plataforma reconocida, en el orden en que aparecieron. */
    val devices: List<DiscoveredDevice>
        get() = groups.filter { it.platform != null }

    /** Añade un resultado. Devuelve `true` si cambió [devices]. */
    fun add(found: DiscoveredDevice): Boolean {
        val before = devices
        val matches = groups.withIndex().filter { (_, g) -> sameDevice(g, found) }
        if (matches.isEmpty()) {
            groups += found
        } else {
            val first = matches.first().index
            var merged = groups[first]
            // Si el nuevo resultado une dos grupos que antes parecían distintos, se funden.
            matches.drop(1).forEach { merged = merge(merged, it.value) }
            merged = merge(merged, found)
            matches.drop(1).map { it.index }.sortedDescending().forEach { groups.removeAt(it) }
            groups[first] = merged
        }
        return devices != before
    }

    private fun sameDevice(a: DiscoveredDevice, b: DiscoveredDevice): Boolean =
        a.host == b.host || (a.uniqueId != null && a.uniqueId == b.uniqueId)

    private fun merge(a: DiscoveredDevice, b: DiscoveredDevice): DiscoveredDevice = a.copy(
        platform = pickPlatform(a.platform, b.platform),
        name = a.name ?: b.name,
        uniqueId = a.uniqueId ?: b.uniqueId,
        methods = a.methods + b.methods,
    )

    /**
     * Si las dos vías dicen plataformas distintas, gana Android TV: su servicio mDNS es
     * específico del protocolo que usará el driver. En otro caso, la primera que llegó.
     */
    private fun pickPlatform(a: Platform?, b: Platform?): Platform? = when {
        a == null -> b
        b == null -> a
        b == Platform.ANDROID_TV -> b
        else -> a
    }
}
