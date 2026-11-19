package app.remoto.control.core.discovery

import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.TvDevice

/**
 * Decide si un dispositivo que se va a agregar ya estaba guardado, para actualizarlo
 * en lugar de duplicarlo.
 */
object SavedDeviceMatcher {
    /**
     * Coincide si es la misma plataforma y además el mismo identificador único (aunque haya
     * cambiado de IP) o, a falta de identificador, la misma IP.
     */
    fun findMatch(saved: List<TvDevice>, platform: Platform, host: String, uniqueId: String?): TvDevice? {
        val samePlatform = saved.filter { it.platform == platform }
        if (uniqueId != null) {
            samePlatform.firstOrNull { it.uniqueId == uniqueId }?.let { return it }
        }
        return samePlatform.firstOrNull { it.host == host && (it.uniqueId == null || uniqueId == null) }
    }

    fun isSaved(saved: List<TvDevice>, device: DiscoveredDevice): Boolean {
        val platform = device.platform ?: return false
        return findMatch(saved, platform, device.host, device.uniqueId) != null
    }
}
