package app.remoto.control.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.TvDevice
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Lista de dispositivos guardada como JSON dentro de un Preferences DataStore.
 * Son pocos elementos (una persona tiene pocas teles), así que no hace falta Room.
 */
@Singleton
class DataStoreDeviceRepository @Inject constructor(
    @param:Named(DEVICES_STORE) private val store: DataStore<Preferences>,
    private val secrets: SecretStore,
) : DeviceRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(StoredDevice.serializer())

    override val devices: Flow<List<TvDevice>> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { decode(it[DEVICES]) }

    override suspend fun get(id: String): TvDevice? = devices.first().firstOrNull { it.id == id }

    override suspend fun upsert(device: TvDevice) {
        edit { list -> list.filterNot { it.id == device.id } + device }
    }

    override suspend fun rename(id: String, name: String) {
        edit { list -> list.map { if (it.id == id) it.copy(name = name.trim()) else it } }
    }

    override suspend fun delete(id: String) {
        edit { list -> list.filterNot { it.id == id } }
        secrets.deleteAll(id)
    }

    override suspend fun ensureDemoDevice(): TvDevice {
        get(DEMO_DEVICE_ID)?.let { return it }
        // Nombre vacío: la interfaz muestra el nombre por defecto en el idioma del teléfono.
        val demo = TvDevice(id = DEMO_DEVICE_ID, name = "", platform = Platform.DEMO)
        upsert(demo)
        return demo
    }

    private suspend fun edit(transform: (List<TvDevice>) -> List<TvDevice>) {
        store.edit { prefs ->
            val updated = transform(decode(prefs[DEVICES]))
            prefs[DEVICES] = json.encodeToString(listSerializer, updated.map { StoredDevice.from(it) })
        }
    }

    private fun decode(raw: String?): List<TvDevice> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(listSerializer, raw).mapNotNull { it.toDevice() }
        } catch (e: SerializationException) {
            emptyList()
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
    }

    /** Formato en disco. Si se cambia, mantener compatibilidad con lo ya guardado. */
    @Serializable
    private data class StoredDevice(
        val id: String,
        val name: String,
        val platform: String,
        val host: String? = null,
    ) {
        fun toDevice(): TvDevice? {
            val p = Platform.entries.firstOrNull { it.name == platform } ?: return null
            return TvDevice(id = id, name = name, platform = p, host = host)
        }

        companion object {
            fun from(d: TvDevice) = StoredDevice(id = d.id, name = d.name, platform = d.platform.name, host = d.host)
        }
    }

    companion object {
        const val DEVICES_STORE = "devices"
        private val DEVICES = stringPreferencesKey("devices_json")
    }
}
