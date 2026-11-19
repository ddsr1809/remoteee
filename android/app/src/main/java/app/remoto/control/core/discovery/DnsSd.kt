package app.remoto.control.core.discovery

/**
 * Nombres de servicio DNS-SD. Según la versión de Android, `NsdManager` entrega el nombre
 * con los caracteres especiales escapados al estilo DNS (`Sala\032de\032estar`, donde
 * `\032` es el byte 32 en decimal, un espacio). Esto lo deja legible.
 */
object DnsSd {
    fun unescapeName(raw: String): String {
        if ('\\' !in raw) return raw
        val bytes = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\\' && i + 3 < raw.length && raw.substring(i + 1, i + 4).isDecimalTriplet()) {
                val value = raw.substring(i + 1, i + 4).toInt()
                if (value <= 255) {
                    bytes.write(value)
                    i += 4
                    continue
                }
            }
            if (c == '\\' && i + 1 < raw.length) {
                // "\." o "\\": el carácter siguiente va tal cual.
                bytes.write(raw[i + 1].toString().toByteArray(Charsets.UTF_8))
                i += 2
                continue
            }
            bytes.write(c.toString().toByteArray(Charsets.UTF_8))
            i++
        }
        // Los escapes decimales pueden formar UTF-8 de varios bytes (acentos, emojis).
        return bytes.toString(Charsets.UTF_8.name())
    }

    private fun String.isDecimalTriplet(): Boolean = length == 3 && all { it in '0'..'9' }
}
