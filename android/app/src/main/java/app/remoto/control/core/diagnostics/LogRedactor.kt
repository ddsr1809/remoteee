package app.remoto.control.core.diagnostics

/**
 * Quita del registro de diagnóstico lo que identifica a una persona o su red:
 *
 * - IPv4: se conservan los dos primeros números (dicen qué tipo de red es) → `192.168.x.x`.
 * - IPv6: se sustituye entera → `[ipv6]`.
 * - Direcciones MAC → `[mac]`.
 * - UUID (el identificador que anuncia cada tele) → solo los 4 primeros caracteres, `1a2b…`,
 *   para poder distinguir dos teles en el mismo registro sin exponer el identificador.
 *
 * Los nombres de las teles no pasan por aquí porque nunca se registran (pueden llevar el
 * nombre de una persona).
 */
object LogRedactor {
    private val uuid = Regex("""\b([0-9a-fA-F]{4})[0-9a-fA-F]{4}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}\b""")
    private val mac = Regex("""\b[0-9a-fA-F]{2}([:-])(?:[0-9a-fA-F]{2}\1){4}[0-9a-fA-F]{2}\b""")
    private val ipv4 = Regex("""\b(\d{1,3})\.(\d{1,3})\.\d{1,3}\.\d{1,3}\b""")

    // Candidato a IPv6: grupos hexadecimales separados por ":" (con zona "%wlan0" opcional).
    // En redact() solo se acepta si tiene "::" o los 8 grupos, para no confundirlo con una hora.
    private val ipv6Candidate = Regex("""(?<![\w:.])[0-9a-fA-F]{0,4}(?::[0-9a-fA-F]{0,4}){2,7}(?:%\w+)?(?![\w:])""")

    fun redact(text: String): String {
        var out = uuid.replace(text) { "${it.groupValues[1].lowercase()}…" }
        out = mac.replace(out, "[mac]")
        out = ipv4.replace(out) { "${it.groupValues[1]}.${it.groupValues[2]}.x.x" }
        out = ipv6Candidate.replace(out) { m ->
            val address = m.value.substringBefore('%')
            val looksLikeIpv6 = address.any { it.isLetterOrDigit() } &&
                ("::" in address || address.count { it == ':' } == 7)
            if (looksLikeIpv6) "[ipv6]" else m.value
        }
        return out
    }
}
