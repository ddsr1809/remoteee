package app.remoto.control.core.net

/** Valida la IP que el usuario escribe a mano. */
object HostValidator {
    enum class Result { VALID, EMPTY, INVALID_FORMAT, NOT_A_DEVICE_ADDRESS }

    /**
     * Solo IPv4 con cuatro números de 0 a 255 (`192.168.1.20`). Se rechazan direcciones que
     * nunca son una tele: `0.x.x.x`, `127.x.x.x` (el propio teléfono), multidifusión
     * (`224`–`239`), reservadas (`240`–`255`) y la de difusión.
     */
    fun validateIpv4(input: String): Result {
        val text = input.trim()
        if (text.isEmpty()) return Result.EMPTY
        val octets = parseIpv4(text) ?: return Result.INVALID_FORMAT
        val first = octets[0]
        return when {
            first == 0 || first == 127 || first >= 224 -> Result.NOT_A_DEVICE_ADDRESS
            else -> Result.VALID
        }
    }

    /** Forma canónica (sin espacios ni ceros a la izquierda) o `null` si no es IPv4. */
    fun normalizeIpv4(input: String): String? = parseIpv4(input.trim())?.joinToString(".")

    /** Dirección privada de una red de casa u oficina (RFC 1918) o de enlace local. */
    fun isPrivateIpv4(host: String): Boolean {
        val o = parseIpv4(host) ?: return false
        return o[0] == 10 ||
            (o[0] == 172 && o[1] in 16..31) ||
            (o[0] == 192 && o[1] == 168) ||
            (o[0] == 169 && o[1] == 254)
    }

    private fun parseIpv4(text: String): List<Int>? {
        val parts = text.split('.')
        if (parts.size != 4) return null
        return parts.map { part ->
            if (part.isEmpty() || part.length > 3 || !part.all { it in '0'..'9' }) return null
            part.toInt().takeIf { it in 0..255 } ?: return null
        }
    }
}
