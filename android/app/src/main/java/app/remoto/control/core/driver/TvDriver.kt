package app.remoto.control.core.driver

import kotlinx.coroutines.flow.StateFlow

/**
 * Interfaz común que implementa cada plataforma. La pantalla del control solo habla
 * con esto (a través de [RemoteSession]) y nunca con un driver concreto.
 *
 * Reglas para quien implemente un driver:
 * - Toda la red y el IR corren fuera del hilo principal (cada driver cambia de dispatcher).
 * - Los errores de conexión no se lanzan: se publican como [ConnectionState.Failed]
 *   con su [HelpTopic].
 * - Las teclas o acciones que el driver no declara en [capabilities] se ignoran.
 *
 * Cambios respecto al punto de partida del prompt (decisión D-07):
 * [movePointer] y [tap] para el touchpad, y [listApps], que puede devolver una lista
 * vacía (Android TV no expone las apps instaladas).
 */
interface TvDriver {
    val capabilities: Set<Capability>
    val state: StateFlow<ConnectionState>

    suspend fun connect()

    /** Completa el emparejamiento. [code] es el código que muestra la tele, si [PairingType.CODE_ON_TV]. */
    suspend fun pair(code: String? = null)

    suspend fun sendKey(key: RemoteKey, longPress: Boolean = false)

    suspend fun sendText(text: String)

    /** Mueve el puntero. [dx] y [dy] son desplazamientos relativos en píxeles del touchpad. */
    suspend fun movePointer(dx: Float, dy: Float)

    /** Clic del touchpad. */
    suspend fun tap()

    suspend fun listApps(): List<TvApp>

    suspend fun launchApp(appId: String)

    /** Enciende la tele (Wake-on-LAN cuando aplique). */
    suspend fun powerOn()

    fun disconnect()
}

/** Crea drivers para una plataforma. Cada paquete de `drivers/` aporta uno vía Hilt (`@IntoSet`). */
interface TvDriverFactory {
    val platform: Platform

    fun create(device: TvDevice): TvDriver
}
