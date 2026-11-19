package app.remoto.control.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @param:Named(SETTINGS_STORE) private val store: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            AppSettings(
                onboardingDone = prefs[ONBOARDING_DONE] ?: false,
                hapticsEnabled = prefs[HAPTICS] ?: true,
                themeMode = prefs[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: ThemeMode.SYSTEM,
                lastDeviceId = prefs[LAST_DEVICE],
            )
        }

    override suspend fun setOnboardingDone() {
        store.edit { it[ONBOARDING_DONE] = true }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        store.edit { it[HAPTICS] = enabled }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    override suspend fun setLastDeviceId(id: String?) {
        store.edit { if (id == null) it.remove(LAST_DEVICE) else it[LAST_DEVICE] = id }
    }

    companion object {
        const val SETTINGS_STORE = "settings"
        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val HAPTICS = booleanPreferencesKey("haptics_enabled")
        private val THEME = stringPreferencesKey("theme_mode")
        private val LAST_DEVICE = stringPreferencesKey("last_device_id")
    }
}
