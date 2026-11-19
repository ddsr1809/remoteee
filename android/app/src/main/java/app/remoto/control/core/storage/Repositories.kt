package app.remoto.control.core.storage

import app.remoto.control.core.driver.TvDevice
import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    /** Ya pasó por la pantalla de bienvenida. */
    val onboardingDone: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Último dispositivo usado; la app abre su control al arrancar. */
    val lastDeviceId: String? = null,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setOnboardingDone()

    suspend fun setHapticsEnabled(enabled: Boolean)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setLastDeviceId(id: String?)
}

/** Dispositivos guardados. La lista nunca sale del teléfono. */
interface DeviceRepository {
    val devices: Flow<List<TvDevice>>

    suspend fun get(id: String): TvDevice?

    suspend fun upsert(device: TvDevice)

    suspend fun rename(id: String, name: String)

    /** Borra el dispositivo **y sus secretos**. */
    suspend fun delete(id: String)

    /** Devuelve la tele simulada, creándola si no existe. */
    suspend fun ensureDemoDevice(): TvDevice
}

/**
 * Secretos por dispositivo (tokens, `client-key`, certificados). Cifrados en reposo.
 * Las implementaciones nunca lanzan por errores de cifrado: devuelven `null` y
 * descartan el dato ilegible.
 */
interface SecretStore {
    suspend fun get(deviceId: String, name: String): String?

    suspend fun put(deviceId: String, name: String, value: String)

    suspend fun deleteAll(deviceId: String)
}

const val DEMO_DEVICE_ID = "demo"
