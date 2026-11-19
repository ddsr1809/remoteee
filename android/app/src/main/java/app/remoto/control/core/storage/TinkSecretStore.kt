package app.remoto.control.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.GeneralSecurityException
import java.util.Base64
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Secretos cifrados con Tink (AES-256-GCM). La clave maestra vive en Android Keystore
 * y nunca sale del hardware; Tink guarda su *keyset* cifrado con ella en
 * SharedPreferences (así funciona `AndroidKeysetManager`). Los secretos cifrados van
 * en un DataStore aparte.
 *
 * Ambos archivos están excluidos de las copias de seguridad (`res/xml/data_extraction_rules.xml`):
 * la clave de Keystore no viaja a otro teléfono, así que una copia restaurada sería ilegible.
 *
 * Nota: no se usa `androidx.security:security-crypto` (EncryptedSharedPreferences) porque está obsoleta.
 */
@Singleton
class TinkSecretStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Named(SECRETS_STORE) private val store: DataStore<Preferences>,
) : SecretStore {
    private val initLock = Mutex()
    private var aead: Aead? = null

    override suspend fun get(deviceId: String, name: String): String? {
        val key = prefKey(deviceId, name)
        val encoded = store.data.first()[key] ?: return null
        val plain = withContext(Dispatchers.IO) {
            runCatching {
                val cipher = Base64.getDecoder().decode(encoded)
                aead().decrypt(cipher, key.name.toByteArray()).toString(Charsets.UTF_8)
            }.getOrNull()
        }
        // Dato ilegible (p. ej. se restauró de otro teléfono): se descarta.
        if (plain == null) store.edit { it.remove(key) }
        return plain
    }

    override suspend fun put(deviceId: String, name: String, value: String) {
        val key = prefKey(deviceId, name)
        val encoded = withContext(Dispatchers.IO) {
            val cipher = aead().encrypt(value.toByteArray(Charsets.UTF_8), key.name.toByteArray())
            Base64.getEncoder().encodeToString(cipher)
        }
        store.edit { it[key] = encoded }
    }

    override suspend fun deleteAll(deviceId: String) {
        val prefix = "$deviceId$SEPARATOR"
        store.edit { prefs ->
            prefs.asMap().keys
                .map { it.name }
                .filter { it.startsWith(prefix) }
                .forEach { prefs.remove(stringPreferencesKey(it)) }
        }
    }

    /** Crea el primitivo AEAD la primera vez. Toca Keystore, así que corre fuera del hilo principal. */
    private suspend fun aead(): Aead = initLock.withLock {
        aead ?: withContext(Dispatchers.IO) { createAead() }.also { aead = it }
    }

    @Throws(GeneralSecurityException::class)
    private fun createAead(): Aead {
        AeadConfig.register()
        val handle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        return handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private fun prefKey(deviceId: String, name: String): Preferences.Key<String> =
        stringPreferencesKey("$deviceId$SEPARATOR$name")

    companion object {
        const val SECRETS_STORE = "secrets"
        const val KEYSET_PREFS_FILE = "remoto_tink_keyset"
        private const val KEYSET_NAME = "secrets_keyset"
        private const val MASTER_KEY_URI = "android-keystore://remoto_master_key"
        private const val SEPARATOR = "/"
    }
}
