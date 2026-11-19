package app.remoto.control.core.discovery

import app.remoto.control.core.driver.FailureReason
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Traduce excepciones de red a un [FailureReason].
 *
 * Con el permiso de red local negado (Android 17, `targetSdk` 37), los sockets UDP fallan con
 * `EPERM` ("Operation not permitted") y los TCP suelen agotar el tiempo. Distinguirlo permite
 * decir "falta el permiso" en lugar de "no encontré tu tele".
 */
object NetworkErrors {
    fun classify(error: Throwable): FailureReason {
        val messages = generateSequence(error) { it.cause }.take(5).mapNotNull { it.message }.toList()
        return when {
            messages.any { "EPERM" in it || it.contains("Operation not permitted", ignoreCase = true) } ->
                FailureReason.LOCAL_NETWORK_PERMISSION_DENIED
            messages.any { "ENETUNREACH" in it || it.contains("Network is unreachable", ignoreCase = true) } ->
                FailureReason.NETWORK_UNAVAILABLE
            error is SocketTimeoutException -> FailureReason.TIMEOUT
            error is IOException -> FailureReason.DEVICE_UNREACHABLE
            else -> FailureReason.UNKNOWN
        }
    }
}
