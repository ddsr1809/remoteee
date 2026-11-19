package app.remoto.control.core.discovery

/**
 * El bloqueo de multidifusión de Wi-Fi (`WifiManager.MulticastLock`). Sin él, muchos
 * teléfonos descartan los paquetes de multidifusión para ahorrar batería. Gasta batería, así
 * que se toma **solo durante la búsqueda** y se suelta siempre.
 */
interface MulticastLockHandle {
    fun acquire()

    fun release()

    val isHeld: Boolean
}

/** Crea un bloqueo nuevo por búsqueda. */
fun interface MulticastLockProvider {
    fun create(): MulticastLockHandle
}

/**
 * Ejecuta [block] con el bloqueo tomado y lo suelta al terminar, también si [block] lanza una
 * excepción o la corrutina se cancela.
 */
inline fun <T> MulticastLockProvider.withLock(block: () -> T): T {
    val lock = create()
    lock.acquire()
    try {
        return block()
    } finally {
        if (lock.isHeld) lock.release()
    }
}
