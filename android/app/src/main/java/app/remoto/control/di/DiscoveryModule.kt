package app.remoto.control.di

import app.remoto.control.core.discovery.AndroidMulticastLockProvider
import app.remoto.control.core.discovery.DiscoverySource
import app.remoto.control.core.discovery.MdnsDiscoverySource
import app.remoto.control.core.discovery.MulticastLockProvider
import app.remoto.control.core.discovery.SsdpDiscoverySource
import app.remoto.control.core.net.AndroidLocalNetworkAccess
import app.remoto.control.core.net.AndroidLocalNetworkMonitor
import app.remoto.control.core.net.LocalNetworkAccess
import app.remoto.control.core.net.LocalNetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoveryModule {
    @Binds
    abstract fun bindNetworkMonitor(impl: AndroidLocalNetworkMonitor): LocalNetworkMonitor

    @Binds
    abstract fun bindLocalNetworkAccess(impl: AndroidLocalNetworkAccess): LocalNetworkAccess

    @Binds
    abstract fun bindMulticastLocks(impl: AndroidMulticastLockProvider): MulticastLockProvider

    @Binds
    @IntoSet
    abstract fun bindSsdp(impl: SsdpDiscoverySource): DiscoverySource

    @Binds
    @IntoSet
    abstract fun bindMdns(impl: MdnsDiscoverySource): DiscoverySource
}
