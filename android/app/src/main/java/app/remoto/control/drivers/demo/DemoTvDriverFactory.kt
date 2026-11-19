package app.remoto.control.drivers.demo

import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.TvDevice
import app.remoto.control.core.driver.TvDriver
import app.remoto.control.core.driver.TvDriverFactory
import app.remoto.control.core.storage.SecretStore
import javax.inject.Inject

class DemoTvDriverFactory @Inject constructor(
    private val secrets: SecretStore,
) : TvDriverFactory {
    override val platform: Platform = Platform.DEMO

    override fun create(device: TvDevice): TvDriver = DemoTvDriver(device, secrets)
}
