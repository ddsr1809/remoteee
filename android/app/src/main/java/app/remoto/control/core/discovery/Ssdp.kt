package app.remoto.control.core.discovery

/**
 * SSDP (la parte de búsqueda de UPnP): se envía un `M-SEARCH` por multidifusión a
 * `239.255.255.250:1900` y cada dispositivo que coincide con el `ST` pedido contesta por
 * unicast con una respuesta con forma de HTTP.
 *
 * Todo lo de este archivo es lógica pura (sin Android) para poder probarlo en JVM.
 */
object Ssdp {
    const val MULTICAST_ADDRESS = "239.255.255.250"
    const val PORT = 1900

    /** Segundos que cada dispositivo puede esperar antes de contestar (cabecera `MX`). */
    const val MX_SECONDS = 2

    /**
     * Tipos de búsqueda (`ST`) que se piden, uno por plataforma.
     *
     * - `roku:ecp`: documentado por Roku (External Control Protocol).
     * - Samsung y LG: no tienen documentación pública; son los que usan las bibliotecas de
     *   código abierto más usadas (samsungctl/samsungtvws y LGTV2/aiopylgtv).
     *   **Verificar con hardware real** en los hitos 3 y 4.
     *
     * Android TV no contesta a SSDP de forma fiable: se busca por mDNS (`_androidtvremote2._tcp`).
     */
    val SEARCH_TARGETS = listOf(
        "roku:ecp",
        "urn:samsung.com:device:RemoteControlReceiver:1",
        "urn:lge-com:service:webos-second-screen:1",
    )

    /** Mensaje `M-SEARCH` para [searchTarget]. Las líneas terminan en CRLF, como en HTTP. */
    fun mSearch(searchTarget: String, mxSeconds: Int = MX_SECONDS): String =
        buildString {
            append("M-SEARCH * HTTP/1.1\r\n")
            append("HOST: $MULTICAST_ADDRESS:$PORT\r\n")
            append("MAN: \"ssdp:discover\"\r\n")
            append("MX: $mxSeconds\r\n")
            append("ST: $searchTarget\r\n")
            append("\r\n")
        }
}

/**
 * Respuesta (o anuncio `NOTIFY`) SSDP ya interpretada.
 *
 * @property headers cabeceras con el nombre en mayúsculas (SSDP no distingue mayúsculas).
 */
data class SsdpMessage(
    val kind: Kind,
    val headers: Map<String, String>,
) {
    enum class Kind { SEARCH_RESPONSE, NOTIFY_ALIVE, NOTIFY_BYEBYE }

    /** `ST` en respuestas y `NT` en anuncios: el tipo de servicio. */
    val searchTarget: String? get() = headers["ST"] ?: headers["NT"]
    val usn: String? get() = headers["USN"]
    val server: String? get() = headers["SERVER"]
    val location: String? get() = headers["LOCATION"]

    /**
     * Identificador estable del dispositivo, sacado del `USN`:
     * `uuid:1234-abcd::urn:...` → `1234-abcd`. Sin el prefijo `uuid:`, se usa el USN completo
     * hasta `::`. En minúsculas, para comparar sin sorpresas.
     */
    val uniqueId: String?
        get() {
            val raw = usn?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val withoutPrefix = if (raw.startsWith("uuid:", ignoreCase = true)) raw.substring(5) else raw
            return withoutPrefix.substringBefore("::").trim().lowercase().takeIf { it.isNotEmpty() }
        }
}

object SsdpParser {
    /** Límite de cabeceras: una respuesta normal trae menos de 15. Evita abusos con paquetes raros. */
    private const val MAX_HEADERS = 64

    /**
     * Interpreta un paquete. Devuelve `null` si no es una respuesta `200` a un `M-SEARCH`
     * ni un `NOTIFY` (por ejemplo, los `M-SEARCH` de otros teléfonos de la red).
     */
    fun parse(packet: String): SsdpMessage? {
        val lines = packet.split("\r\n", "\n")
        val statusLine = lines.firstOrNull()?.trim().orEmpty()
        val headers = parseHeaders(lines.drop(1)) ?: return null
        val kind = when {
            isOkResponse(statusLine) -> SsdpMessage.Kind.SEARCH_RESPONSE
            statusLine.startsWith("NOTIFY ", ignoreCase = true) -> when {
                headers["NTS"].equals("ssdp:byebye", ignoreCase = true) -> SsdpMessage.Kind.NOTIFY_BYEBYE
                else -> SsdpMessage.Kind.NOTIFY_ALIVE
            }
            else -> return null
        }
        return SsdpMessage(kind, headers)
    }

    private fun isOkResponse(statusLine: String): Boolean {
        val parts = statusLine.split(' ', limit = 3)
        return parts.size >= 2 && parts[0].startsWith("HTTP/", ignoreCase = true) && parts[1] == "200"
    }

    private fun parseHeaders(lines: List<String>): Map<String, String>? {
        val headers = LinkedHashMap<String, String>()
        for (line in lines) {
            if (line.isBlank()) break
            val colon = line.indexOf(':')
            if (colon <= 0) continue
            val name = line.substring(0, colon).trim().uppercase()
            if (name.isEmpty()) continue
            // Si una cabecera se repite, gana la primera.
            headers.putIfAbsent(name, line.substring(colon + 1).trim())
            if (headers.size > MAX_HEADERS) return null
        }
        return headers
    }
}
