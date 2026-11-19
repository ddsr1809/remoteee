package app.remoto.control.core.net

enum class LocalNetworkPermission {
    /** El sistema no lo pide (Android 16 o anterior, o `targetSdk` 36 o menor). */
    NOT_REQUIRED,
    GRANTED,

    /** Falta el permiso: hay que explicarlo y pedirlo antes de tocar la red local. */
    DENIED,
}

/**
 * Permiso de red local de Android 17 (`ACCESS_LOCAL_NETWORK`, grupo "Dispositivos cercanos").
 *
 * Con `targetSdk` 37 el sistema bloquea todo el tráfico a direcciones locales (TCP, UDP,
 * multidifusión y mDNS) hasta que el usuario lo concede. **Hoy la app tiene `targetSdk` 36**,
 * así que [status] siempre devuelve [LocalNetworkPermission.NOT_REQUIRED] y el permiso no se
 * declara en el manifiesto ni se pide (sección 7.4 del prompt).
 *
 * Para activarlo, el día que se suba a `targetSdk` 37:
 * 1. Declarar `<uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />`.
 * 2. En `LocalNetworkPermissionContent`, cambiar el botón para que lance la petición con
 *    `rememberLauncherForActivityResult(RequestPermission())` en lugar de abrir los ajustes.
 * 3. Probar en Android 16 con `adb shell am compat enable RESTRICT_LOCAL_NETWORK app.remoto.control`.
 */
interface LocalNetworkAccess {
    fun status(): LocalNetworkPermission

    companion object {
        const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

        /** Nivel de API de Android 17, el primero que aplica la restricción. */
        const val ENFORCED_FROM_API = 37

        /** Lógica pura, separada para probarla sin Android. */
        fun statusFor(deviceApi: Int, targetSdk: Int, granted: Boolean): LocalNetworkPermission = when {
            deviceApi < ENFORCED_FROM_API || targetSdk < ENFORCED_FROM_API -> LocalNetworkPermission.NOT_REQUIRED
            granted -> LocalNetworkPermission.GRANTED
            else -> LocalNetworkPermission.DENIED
        }
    }
}
