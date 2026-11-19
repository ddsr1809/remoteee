package app.remoto.control.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import app.remoto.control.core.ApplicationScope
import app.remoto.control.core.storage.DataStoreDeviceRepository
import app.remoto.control.core.storage.DataStoreSettingsRepository
import app.remoto.control.core.storage.DeviceRepository
import app.remoto.control.core.storage.SecretStore
import app.remoto.control.core.storage.SettingsRepository
import app.remoto.control.core.storage.TinkSecretStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    @Named(DataStoreSettingsRepository.SETTINGS_STORE)
    fun provideSettingsStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(DataStoreSettingsRepository.SETTINGS_STORE)
        }

    @Provides
    @Singleton
    @Named(DataStoreDeviceRepository.DEVICES_STORE)
    fun provideDevicesStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(DataStoreDeviceRepository.DEVICES_STORE)
        }

    @Provides
    @Singleton
    @Named(TinkSecretStore.SECRETS_STORE)
    fun provideSecretsStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(TinkSecretStore.SECRETS_STORE)
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageBindings {
    @Binds
    abstract fun bindSettings(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindDevices(impl: DataStoreDeviceRepository): DeviceRepository

    @Binds
    abstract fun bindSecrets(impl: TinkSecretStore): SecretStore
}
