package app.remoto.control.core.driver

import kotlinx.coroutines.flow.StateFlow

/**
 * Lo que un driver de tele simulada ofrece además de [TvDriver]: el estado de su
 * "pantalla" para dibujarlo y la posibilidad de provocar errores.
 *
 * Vive en el núcleo para que la interfaz pueda mostrar la tele simulada sin importar
 * `drivers/demo` (decisión D-08). Los drivers reales no lo implementan.
 */
interface SimulatedTv {
    val preview: StateFlow<TvPreviewState>

    /** Provoca [failure] en la próxima conexión y reconecta para mostrarlo. */
    suspend fun simulateFailure(failure: SimulatedFailure)
}

enum class SimulatedFailure {
    TIMEOUT,
    PAIRING_REJECTED,
    PERMISSION_DENIED,
}

/** Lo que "se ve" en la tele simulada. */
data class TvPreviewState(
    val poweredOn: Boolean = true,
    val volume: Int = 20,
    val muted: Boolean = false,
    val channel: Int = 1,
    val tiles: List<TvApp> = emptyList(),
    val focusedTile: Int = 0,
    /** App abierta a pantalla completa; `null` = pantalla de inicio. */
    val openApp: TvApp? = null,
    val playing: Boolean = false,
    val typedText: String = "",
    /** Puntero del touchpad en coordenadas 0..1. */
    val pointerX: Float = 0.5f,
    val pointerY: Float = 0.5f,
    val pointerVisible: Boolean = false,
    /** Código que "muestra la tele" mientras espera emparejamiento. */
    val pairingCode: String? = null,
    /** Última tecla recibida y un contador para que cada pulsación produzca una reacción visible. */
    val lastKey: RemoteKey? = null,
    val keyCount: Int = 0,
)
