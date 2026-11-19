package app.remoto.control

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.remoto.control.core.driver.RemoteSession
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RemotoApp : Application() {
    @Inject
    lateinit var session: RemoteSession

    override fun onCreate() {
        super.onCreate()
        // Suelta la conexión al ir a segundo plano y reconecta sola al volver (sección 7.2).
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = session.onAppForeground()

                override fun onStop(owner: LifecycleOwner) = session.onAppBackground()
            },
        )
    }
}
