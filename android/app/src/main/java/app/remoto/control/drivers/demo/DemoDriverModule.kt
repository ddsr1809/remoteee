package app.remoto.control.drivers.demo

import app.remoto.control.core.driver.TvDriverFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Registra el driver demo. Quitar la tele simulada = borrar este paquete. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DemoDriverModule {
    @Binds
    @IntoSet
    abstract fun bindFactory(factory: DemoTvDriverFactory): TvDriverFactory
}
