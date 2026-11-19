package app.remoto.control.core.driver

/** Estado de la conexión con la tele. Lo publica cada driver como `StateFlow`. */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState

    data object Connecting : ConnectionState

    data class PairingRequired(val type: PairingType) : ConnectionState

    data object Connected : ConnectionState

    data class Failed(val reason: FailureReason, val help: HelpTopic?) : ConnectionState {
        companion object {
            /** Fallo con el tema de ayuda que le corresponde a [reason]. */
            fun of(reason: FailureReason): Failed = Failed(reason, HelpTopic.forFailure(reason))
        }
    }
}

/** Se puede enviar teclas. */
val ConnectionState.canSendCommands: Boolean
    get() = this == ConnectionState.Connected

/** Tiene sentido llamar a `connect()` (no hay una conexión en curso ni activa). */
val ConnectionState.canConnect: Boolean
    get() = this is ConnectionState.Disconnected || this is ConnectionState.Failed
