package app.remoto.control.core.discovery

import app.remoto.control.core.diagnostics.DiagnosticsLog
import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.core.storage.DeviceRepository
import app.remoto.control.core.storage.SettingsRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Guarda una tele encontrada en la búsqueda o escrita a mano. Si ya estaba guardada
 * (misma tele, quizá con otra IP), la actualiza y conserva el nombre que le puso el usuario.
 */
class DeviceAdder(
    private val devices: DeviceRepository,
    private val settings: SettingsRepository,
    private val log: DiagnosticsLog,
    private val newId: () -> String,
) {
    @Inject
    constructor(devices: DeviceRepository, settings: SettingsRepository, log: DiagnosticsLog) :
        this(devices, settings, log, { UUID.randomUUID().toString() })

    suspend fun add(found: DiscoveredDevice): TvDevice? {
        val platform = found.platform ?: return null
        return add(platform, found.host, found.uniqueId, found.name)
    }

    suspend fun add(platform: Platform, host: String, uniqueId: String? = null, name: String? = null): TvDevice {
        val saved = devices.devices.first()
        val existing = SavedDeviceMatcher.findMatch(saved, platform, host, uniqueId)
        val device = existing?.copy(host = host, uniqueId = uniqueId ?: existing.uniqueId)
            ?: TvDevice(
                id = newId(),
                name = name?.trim().orEmpty(),
                platform = platform,
                host = host,
                uniqueId = uniqueId,
            )
        devices.upsert(device)
        // Agregar una tele desde la bienvenida también la da por terminada.
        settings.setOnboardingDone()
        log.log(TAG, "${if (existing != null) "Actualizada" else "Guardada"} tele $platform en $host")
        return device
    }

    private companion object {
        const val TAG = "devices"
    }
}
