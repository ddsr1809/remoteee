package app.remoto.control.core.discovery

import app.remoto.control.core.driver.Platform

/**
 * Deduce la plataforma a partir de lo que anuncia el dispositivo. Primero mira el tipo de
 * servicio (lo más fiable) y después la cabecera `SERVER`.
 *
 * Las cadenas de Samsung y LG salen de bibliotecas de código abierto, no de documentación
 * oficial: hay que confirmarlas con teles reales (hitos 3 y 4).
 */
object PlatformIdentifier {
    /** Servicio mDNS del protocolo "Android TV Remote v2" (verificar con hardware real). */
    const val ANDROID_TV_REMOTE_SERVICE = "_androidtvremote2._tcp"

    fun fromSsdp(message: SsdpMessage): Platform? {
        val target = message.searchTarget.orEmpty()
        val usn = message.usn.orEmpty()
        val server = message.server.orEmpty()
        return when {
            target.contains("roku:ecp", true) || usn.contains("roku:ecp", true) -> Platform.ROKU
            target.contains("samsung.com:device:RemoteControlReceiver", true) -> Platform.SAMSUNG
            target.contains("lge-com:service:webos-second-screen", true) -> Platform.LG
            server.contains("Roku", true) -> Platform.ROKU
            server.contains("webOS", true) -> Platform.LG
            server.contains("Samsung", true) || server.contains("Tizen", true) -> Platform.SAMSUNG
            else -> null
        }
    }

    /** [serviceType] tal como lo da `NsdManager` (con o sin punto final, con o sin `.local`). */
    fun fromMdnsServiceType(serviceType: String): Platform? =
        when (normalizeServiceType(serviceType)) {
            ANDROID_TV_REMOTE_SERVICE -> Platform.ANDROID_TV
            else -> null
        }

    /** `"._androidtvremote2._tcp.local."` → `"_androidtvremote2._tcp"`. */
    fun normalizeServiceType(serviceType: String): String =
        serviceType.trim().trim('.').lowercase().removeSuffix(".local").trim('.')
}
